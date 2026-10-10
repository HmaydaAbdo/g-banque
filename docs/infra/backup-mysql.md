# Backup automatique MySQL — G-Banque

> Document technique (infra/exploitation). Ne relève pas des quatre niveaux de documentation
> fonctionnelle définis dans `CLAUDE.md` (aucune règle métier, aucun écran, aucun endpoint).

---

## Mécanisme

Un service dédié `backup` a été ajouté à `docker-compose.yml`, basé sur l'image
[`fradelg/mysql-cron-backup`](https://github.com/fradelg/docker-mysql-cron-backup) (pattern standard
en production : conteneur sidecar avec cron intégré, qui exécute `mysqldump` sur le réseau Docker
interne, sans exposer MySQL à l'extérieur).

| Paramètre | Valeur | Détail |
|---|---|---|
| Fréquence | Tous les jours à 02:00 | `CRON_TIME=0 2 * * *`, heure = `TZ` du `.env.prod` |
| Rétention | 7 derniers backups | `MAX_BACKUPS=7` — purge automatique au-delà |
| Backup au démarrage | Oui | `INIT_BACKUP=1` — une sauvegarde est prise dès que le conteneur démarre |
| Compression | gzip niveau 9 | `GZIP_LEVEL=9` |
| Destination | `./backups/mysql` (hôte) | Monté dans `/backup` du conteneur — **dossier local**, en dehors de `docker-data/` |
| Utilisateur DB | `root` | Nécessaire pour un dump complet fiable (cohérent avec le service `phpmyadmin` existant) |
| Image | `fradelg/mysql-cron-backup:1.15.0` | Version épinglée (jamais `latest`) |
| Options du dump | `--no-tablespaces --single-transaction --skip-ssl` | `--skip-ssl` **obligatoire** depuis la 1.15.0, voir ci-dessous |

**Point d'attention — échec silencieux (constaté le 10/10/2026).** Depuis la 1.15.0, l'image utilise le
client `mariadb-dump`, qui refuse le certificat auto-signé de MySQL 8 : sans `--skip-ssl`, le journal du
conteneur affiche `TLS/SSL error: self-signed certificate` mais le conteneur reste « en marche » et
annonce « Backup process finished » — **aucun fichier n'est produit**. `--skip-ssl` désactive le
chiffrement de l'échange, qui reste confiné au réseau Docker interne (MySQL n'est pas exposé).
Test réalisé avant adoption : sauvegarde par l'ancienne image et par la 1.15.0 d'une base de test
(décimaux, accents, vue), restauration de chacune dans un MySQL 8 vierge, résultats identiques à la source.

**Vérifier qu'une sauvegarde est réellement produite** (après toute mise à jour de l'image ou de MySQL) :

```bash
docker logs gbanque-backup 2>&1 | grep -iE "error|symlink"
ls -lh ./backups/mysql
```

Le fichier du jour doit exister et le journal ne doit contenir aucune ligne `error`.

**Limite assumée (validée avec HMAYDA) :** le stockage est local uniquement, sur le même disque que
`docker-data/`. Cela protège contre une corruption de la base, une mauvaise commande, ou un incident
applicatif — mais **pas** contre une perte du disque ou de la machine elle-même. Si cette protection
supplémentaire devient nécessaire, il faudra copier périodiquement `./backups/mysql` vers un second
emplacement (disque externe, NAS, stockage cloud).

---

## Activer le mécanisme

```bash
docker compose up -d backup
```

Comme `INIT_BACKUP=1`, un premier backup est généré immédiatement — pas besoin d'attendre 02:00 le
lendemain.

---

## Lister les backups disponibles

```bash
docker exec gbanque-backup ls -lh /backup
```

ou directement sur l'hôte :

```bash
ls -lh ./backups/mysql
```

Format des fichiers : `AAAAMMJJHHmm.<nom_base>.sql.gz`

---

## Restaurer un backup

**Étape 1 — Identifier le fichier à restaurer** (voir section précédente).

**Étape 2 — Restaurer via le conteneur de backup :**

```bash
docker exec gbanque-backup /restore.sh /backup/<fichier>.sql.gz
```

Ce script :
1. décompresse le fichier ;
2. recrée la base si nécessaire ;
3. réimporte toutes les données.

**Étape 3 — Vérifier après restauration :**

```sql
SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '<MYSQL_DATABASE>';
```

Comparer le nombre de tables et, sur quelques tables clés, le nombre de lignes, avec ce qui était
attendu avant l'incident.

**Étape 4 — Redémarrer le backend** pour repartir sur une base propre :

```bash
docker compose restart backend
```

---

## Test de restauration périodique

Un backup jamais testé n'est pas une garantie de récupération. Il est recommandé de tester une
restauration (sur un environnement de test, pas en production) au moins une fois par trimestre pour
s'assurer que la procédure ci-dessus fonctionne réellement.

---

## Historique

- **2026-08-05** : mise en place initiale, suite à une perte de base de données sans backup disponible
  (dernier backup manuel disponible : 31/07/2026). Rétention 7 jours choisie par HMAYDA.
