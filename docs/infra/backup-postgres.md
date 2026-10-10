# Sauvegarde et restauration PostgreSQL — G-Banque

> Document technique (infra/exploitation). Ne relève pas des quatre niveaux de documentation
> fonctionnelle définis dans `CLAUDE.md` (aucune règle métier, aucun écran, aucun endpoint).

---

## Mécanisme

Service `backup` de `docker-compose.yml`, image `prodrigestivill/postgres-backup-local:18-alpine-d257e5d` : conteneur
sidecar qui exécute `pg_dump` sur le réseau Docker interne (PostgreSQL n'est pas exposé à l'extérieur).

| Paramètre | Valeur | Détail |
|---|---|---|
| Fréquence | Tous les jours à 02:00 | `SCHEDULE=0 2 * * *`, heure = `TZ` du `.env` |
| Au démarrage | Oui | `BACKUP_ON_START=TRUE` : une sauvegarde à chaque démarrage du conteneur |
| Rétention | 7 jours, 4 semaines, 6 mois | `BACKUP_KEEP_DAYS` / `_WEEKS` / `_MONTHS` — purge automatique |
| Contenu | Base complète (schéma, données, historique Flyway, collation `ci_ai`) | `pg_dump` texte compressé, `POSTGRES_EXTRA_OPTS=-Z9` |
| Destination | `./backups/postgres` (hôte) | `daily/`, `weekly/`, `monthly/`, `last/` ; liens `*-latest.sql.gz` vers le plus récent |

Les données de la base sont dans `./docker-data/postgres` (dossier de l'hôte monté sur `/var/lib/postgresql`,
emplacement imposé par PostgreSQL 18). `./docker-data/` et `./backups/` survivent à `docker compose down` et aux
redéploiements ; ils ne sont pas versionnés.

**Limite assumée (validée avec HMAYDA) :** stockage local, sur le même disque que la base. Protège contre une
corruption, une mauvaise commande ou un incident applicatif — **pas** contre la perte du disque ou de la machine.
Copier périodiquement `./backups/postgres` vers un second emplacement si cette protection devient nécessaire.

---

## Vérifier qu'une sauvegarde est produite

```bash
docker logs gbanque-backup 2>&1 | tail -3      # doit finir par « SQL backup created successfully »
ls -lh ./backups/postgres/daily
```

---

## Restaurer une sauvegarde

Toujours dans une base **vierge** (le fichier recrée tout : tables, séquences, vues, collation, historique Flyway).

```bash
docker compose stop backend backup
docker compose rm -sf postgres
# Mettre de côté l'ancien dossier de données (ne pas le supprimer avant d'avoir vérifié la restauration)
mv docker-data/postgres docker-data/postgres.avant-restauration
docker compose up -d postgres                  # base vierge (attendre « healthy »)
gunzip -c backups/postgres/daily/gbanque-AAAAMMJJ.sql.gz | docker exec -i gbanque-postgres psql -U <POSTGRES_USER> -d <POSTGRES_DB> -v ON_ERROR_STOP=1 -q
docker compose up -d
```

Contrôle après restauration (comparer avec les valeurs attendues) :

```bash
docker exec gbanque-postgres psql -U <POSTGRES_USER> -d <POSTGRES_DB> -c "select count(*) from transaction; select count(*) from commission; select version, success from flyway_schema_history;"
```

**Vérifié le 10/10/2026 :** sauvegarde restaurée dans un PostgreSQL 18 vierge → mêmes volumes (5 713 transactions,
5 621 commissions, 3 utilisateurs), même total des commissions au centime, 5 739 lignes dans la vue des encours,
historique Flyway et recherche insensible aux accents identiques.

---

## Installer la base sur un autre serveur (transfert de production)

Ne pas copier `./docker-data/postgres` d'un poste Windows vers un serveur Linux : PostgreSQL refuse un dossier de
données dont le propriétaire et les droits ne sont pas les siens. Le transfert se fait par une sauvegarde :

1. Sur la machine d'origine : `docker logs gbanque-backup` → copier le dernier `backups/postgres/daily/*.sql.gz`.
2. Sur le serveur : dépôt (ou `docker-compose.yml` + images), `.env`, puis la procédure « Restaurer une sauvegarde »
   ci-dessus avec ce fichier.

---

## Test de restauration périodique

Une sauvegarde jamais restaurée n'est pas une garantie. Tester la procédure ci-dessus au moins une fois par
trimestre, sur une machine de test (jamais sur la production).

---

## Historique

- **2026-08-05** : mise en place initiale (MySQL, `fradelg/mysql-cron-backup`), suite à une perte de base de données
  sans sauvegarde disponible. Rétention 7 jours choisie par HMAYDA.
- **2026-10-10** : passage à PostgreSQL 18 ; `postgres-backup-local`, rétention étendue à 7 jours / 4 semaines /
  6 mois ; restauration testée.
