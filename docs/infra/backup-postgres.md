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

Les données de la base sont dans le **volume Docker nommé `gbanque-postgres-data`** (monté sur
`/var/lib/postgresql`, emplacement imposé par PostgreSQL 18). Il survit à `docker compose down` et aux
redéploiements ; **`docker compose down -v` le supprime** (ne jamais utiliser `-v` en production). Les sauvegardes,
elles, sont des fichiers de l'hôte (`./backups/postgres`, non versionné) pour pouvoir être copiées ailleurs.
Voir les volumes : `docker volume ls`.

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
Sur une base non vide, la restauration s'arrête dès la première instruction (`collation "ci_ai" already exists`)
sans rien modifier.

```bash
# 1. Vérifier que la sauvegarde à restaurer est lisible AVANT de supprimer quoi que ce soit
gunzip -t backups/postgres/daily/gbanque-AAAAMMJJ.sql.gz
# 2. Arrêter l'application et repartir d'un volume vide (la base actuelle est DÉTRUITE)
docker compose down
docker volume rm gbanque-postgres-data
docker compose up -d postgres                  # attendre « PostgreSQL init process complete » dans docker logs gbanque-postgres
# 3. Restaurer (copie dans le conteneur : évite les tubes binaires de PowerShell)
docker cp backups/postgres/daily/gbanque-AAAAMMJJ.sql.gz gbanque-postgres:/tmp/restore.sql.gz
docker exec gbanque-postgres sh -c "gunzip -c /tmp/restore.sql.gz | psql -U <POSTGRES_USER> -d <POSTGRES_DB> -v ON_ERROR_STOP=1 -q"
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

La base vit dans un volume Docker : elle se transfère **par une sauvegarde**, jamais en copiant des fichiers.

1. Sur la machine d'origine : copier le dernier `backups/postgres/daily/*.sql.gz`.
2. Sur le serveur : `docker-compose.yml` + `.env` (les images viennent de Docker Hub : `docker compose pull`), puis
   la procédure « Restaurer une sauvegarde » ci-dessus avec ce fichier.

**Fait le 10/10/2026** pour la mise en production (serveur Windows, `C:\apps\G-Banque`) ; contrôle attendu après
restauration : 104 tables, 2 vues, 55 407 lignes, Flyway en version 2.

---

## Test de restauration périodique

Une sauvegarde jamais restaurée n'est pas une garantie. Tester la procédure ci-dessus au moins une fois par
trimestre, sur une machine de test (jamais sur la production).

---

## Historique

- **2026-08-05** : mise en place initiale (MySQL, `fradelg/mysql-cron-backup`), suite à une perte de base de données
  sans sauvegarde disponible. Rétention 7 jours choisie par HMAYDA.
- **2026-10-10** : passage à PostgreSQL 18 ; `postgres-backup-local`, rétention étendue à 7 jours / 4 semaines /
  6 mois ; restauration testée ; base dans un volume Docker nommé (`gbanque-postgres-data`).
