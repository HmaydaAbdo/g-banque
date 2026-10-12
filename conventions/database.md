# Base de données — Migrations Flyway et cohérence

> La base de données de production est vivante. Toute évolution du schéma ou des données est écrite comme une migration Flyway versionnée, relue, testée sur une base vierge **et** sur une copie de la production, puis appliquée automatiquement au démarrage du backend.

---

## Principes

* **Moteur unique : PostgreSQL 18** (depuis le 10/10/2026, à la place de MySQL 8), en développement, en test, en CI et en production : image `postgres:18.6-alpine` du `docker-compose.yml` et de Testcontainers, base créée en locale ICU `fr-FR`. Aucun autre moteur (MySQL, MariaDB/XAMPP, H2) n'est utilisé pour développer ou tester.
* **Flyway est la seule source de vérité du schéma.** Hibernate ne crée ni ne modifie jamais rien : `spring.jpa.hibernate.ddl-auto=validate` partout. Au démarrage, Hibernate vérifie que les entités correspondent au schéma et refuse de démarrer sinon.
* Le backend applique lui-même les migrations en attente au démarrage (`spring.flyway`). Aucun script n'est plus exécuté à la main sur la production.
* Une base vierge se construit entièrement par les migrations (`V1`, `V2`, … puis `R__`). C'est ainsi que les tests d'intégration construisent leur base : un écart entre une entité et les migrations fait échouer la CI.

---

## Emplacement et nommage

Dossier unique : `Backend/src/main/resources/db/migration/`.

| Type | Nom | Usage |
|---|---|---|
| Versionnée | `V<n>__<description_en_snake_case>.sql` | Toute évolution de schéma ou de données (table, colonne, index, contrainte, enum, correction de données). Exécutée **une seule fois**, dans l'ordre des numéros. |
| Répétable | `R__<objet>.sql` | Objets recréables sans perte : **vues** (`R__v_recapitulatif_encours.sql`, `R__v_recap_moyens_paiement.sql`). Ré-appliquée automatiquement dès que le fichier change, après toutes les versionnées. |

* Numérotation continue (`V3`, `V4`…), jamais de trou volontaire ni de réutilisation.
* **Plusieurs sessions de travail peuvent écrire sur `main` en parallèle** (poste local, autre session, correctif urgent). Pour éviter deux migrations de même numéro : `git fetch` puis prendre le premier numéro libre **sur `origin/main`** (`git ls-tree origin/main Backend/src/main/resources/db/migration/`) avant de nommer le fichier, et `git pull --rebase` juste avant `git push`. Si deux migrations portent le même numéro, la CI échoue (Flyway refuse de construire la base de test) : la seconde poussée est alors **renumérotée avant tout déploiement**. Une migration déjà appliquée sur une base (même locale de recette) ne se renumérote jamais.
* **Une migration appliquée n'est jamais modifiée** (Flyway refuse de démarrer si son empreinte change). Une erreur se corrige par une nouvelle migration.
* Une migration = un objectif. En-tête obligatoire : objectif, tables concernées, impact sur les données existantes, requêtes de contrôle.
* Une vue se modifie en modifiant son fichier `R__` (commencer par `DROP VIEW IF EXISTS` puis `CREATE VIEW`). Les colonnes texte d'une vue portent `COLLATE ci_ai` dans chaque branche d'un `UNION` ; les `NULL` de la **première** branche sont typés (`CAST(NULL AS date)`, `CAST(NULL AS varchar)`) car PostgreSQL résout les types du `UNION` de gauche à droite ; concaténer avec `a || b` (NULL si un opérande est NULL), jamais `concat()` qui ignore les NULL.
* `V1__schema_initial.sql` est le schéma PostgreSQL généré depuis les entités le 10/10/2026 ; les données de production y ont été versées lors de la migration depuis MySQL (cf. `docs/infra/migration-postgres.md`, archive).

---

## Checklist obligatoire avant toute modification d'entité

Toute modification d'entité JPA s'accompagne, dans le même changement, de la migration `V<n>` qui fait évoluer le schéma — sinon Hibernate (`validate`) refuse de démarrer.

| Question | Action requise dans la migration |
|---|---|
| Nouvelle table ? | `CREATE TABLE` avec clés, index et contraintes ; table `_aud` si l'entité est `@Audited`. |
| Nouvelle colonne nullable ? | `ALTER TABLE … ADD COLUMN …` (et sur la table `_aud`). |
| Nouvelle colonne NOT NULL sur une table non vide ? | Ajouter nullable → `UPDATE` des lignes existantes → `MODIFY … NOT NULL`, dans la même migration ou en deux migrations. |
| Colonne renommée ? | `ALTER TABLE … RENAME COLUMN` (données conservées) — jamais « nouvelle colonne + ancienne abandonnée ». |
| Colonne supprimée ? | `DROP COLUMN` explicite, après vérification qu'aucun code ni vue ne la lit. |
| Type modifié ? | `MODIFY` avec conversion des données ; vérifier les valeurs incompatibles avant. |
| Enum étendu ? | `MODIFY … ENUM(…)` avec la nouvelle valeur (table et `_aud`). |
| Enum réduit ? | Migrer d'abord les lignes portant la valeur retirée (`UPDATE`), contrôler, puis `MODIFY`. |
| Contrainte UNIQUE ajoutée ? | Vérifier l'absence de doublons ; les traiter dans la migration si décision validée. |
| Index ajouté ? | `CREATE INDEX` ; vérifier l'impact sur les tables volumineuses. |
| Vue impactée ? | Mettre à jour le fichier `R__` de la vue dans le même changement. |
| Nouvelle colonne texte libre ? | `varchar(n) COLLATE ci_ai` (ou `text COLLATE ci_ai`), cf. « Texte et recherche ». Énumération : `varchar(50)` + `CHECK (col IN (...))`, sans `ci_ai`. |
| Nom = mot réservé PostgreSQL ? | Choisir un autre nom (`@Column(name = "...")`). |

---

## Analyse obligatoire des données existantes

Avant tout changement de schéma, **analyser l'impact sur les données déjà en production**. Cette analyse précède l'écriture de la migration.

1. Identifier les tables impactées (nom de table = `@Table(name = ...)`, plus les tables `_aud`).
2. Fournir les requêtes de diagnostic à exécuter sur la production (lecture seule) :

```sql
SELECT COUNT(*) FROM nom_table;
SELECT colonne, COUNT(*) FROM nom_table GROUP BY colonne;                      -- distribution d'un enum
SELECT colonne, COUNT(*) FROM nom_table GROUP BY colonne HAVING COUNT(*) > 1;  -- doublons avant UNIQUE
SELECT COUNT(*) FROM nom_table WHERE colonne IS NULL;                          -- avant NOT NULL
```

3. Écrire la migration en tenant compte des résultats, avec ses requêtes de contrôle en commentaire.
4. **Tester** : sur une base vierge (tests d'intégration, automatique) **et** sur une copie restaurée de la sauvegarde de production (cf. `docs/infra/backup-postgres.md`), backend démarré contre cette copie.

Toute écriture sur des données de production (UPDATE/DELETE dans une migration) exige un accord explicite préalable.

---

## Texte et recherche (PostgreSQL distingue majuscules, minuscules et accents)

Sous MySQL (`utf8mb4_*_ci`), « credit » trouvait « Crédit » et `'BMCI' = 'bmci'`. PostgreSQL, par défaut, distingue
majuscules, minuscules **et** accents : sans précaution, une recherche ou une égalité sur du texte change de résultat.

* Toute colonne de **texte libre** (libellés, noms, numéros, observations, `username`…) utilise la collation
  **`ci_ai`** (ICU `und-u-ks-level1`, non déterministe, créée en V1) : égalité, `LIKE` (supporté depuis PostgreSQL 18),
  `GROUP BY`, `DISTINCT` et contraintes d'unicité y ignorent la casse **et** les accents, comme sous MySQL.
* Les colonnes d'**énumération** (`CHECK ... IN`) gardent la collation stricte de la base : leurs valeurs sont des
  constantes du code.
* Une recherche « contient » s'écrit `cb.like(root.get("libelle"), "%" + terme + "%")` sur une colonne `ci_ai` ;
  `cb.lower(...)` y est inutile mais sans effet. Sur une colonne **sans** `ci_ai` (énumération, colonne technique),
  une recherche textuelle libre est interdite.
* `ORDER BY` sur texte suit la locale de la base (`fr-FR`) ; `ci_ai` ne sert pas au tri.
* Nouvelle colonne texte libre dans une migration : `COLLATE ci_ai` obligatoire (oubli = recherche sensible à la
  casse et aux accents, détectable en recette).
* Conséquence assumée : le nom d'utilisateur à la connexion est insensible à la casse et aux accents (comme avant).
* Les **expressions régulières** (`~`, `regexp_*`) sont refusées par PostgreSQL sur une collation non déterministe :
  écrire `colonne COLLATE "C" ~ '...'` si une requête manuelle en a besoin (l'application n'en utilise pas).
* `CHECK`, `NOT NULL` et longueurs : la V1 suit les entités, sauf exceptions documentées en tête de V1 (valeur
  héritée `Importation` ; les 60 colonnes laissées facultatives en V1 pour verser les données de production sont
  obligatoires depuis V2). Toute nouvelle exception est une décision écrite.

---

## Format de signalement

Lorsqu'une modification touche le schéma ou des données existantes, **signaler avant d'écrire le code** :

```
⚠️ CHANGEMENT BASE DE DONNÉES DÉTECTÉ

Table(s) concernée(s) : nom_table (+ nom_table_aud)
Migration prévue      : V<n>__<description>.sql

─── SCHÉMA ─────────────────────────────────────────
Changement : [ex. colonne `statut` renommée en `etat`]
Risque     : [ex. lignes existantes à valeur hors enum]

─── DIAGNOSTIC (lecture seule, sur la prod) ────────
SELECT statut, COUNT(*) FROM nom_table GROUP BY statut;

─── MIGRATION ───────────────────────────────────────
ALTER TABLE nom_table RENAME COLUMN statut TO etat;

─── CONTRÔLE ────────────────────────────────────────
SELECT COUNT(*) FROM nom_table WHERE etat IS NULL; -- doit retourner 0

⛔ NE PAS CONTINUER SANS VALIDATION EXPLICITE.
```

---

## Déploiement

1. Sauvegarde de la base vérifiée (fichier du jour présent).
2. Déploiement des images : au démarrage, le backend applique les migrations en attente puis valide le schéma.
3. Vérifier dans les logs : `Successfully applied N migrations` (ou « Schema … is up to date »), puis démarrage complet.
4. En cas d'échec d'une migration : le backend ne démarre pas ; restaurer la sauvegarde, corriger par une nouvelle migration (jamais en modifiant celle qui a échoué si elle a été partiellement appliquée sans réparation `flyway repair` documentée).

---

## Responsabilités

| Qui | Quoi |
|---|---|
| Développeur (IA) | Signaler tout changement de schéma ou de données avant de modifier le code ; écrire la migration et ses contrôles. |
| Développeur (IA) | Tester la migration sur base vierge et sur copie de la production. |
| Développeur (humain) | Valider le changement, en particulier toute écriture sur des données existantes. |
| Développeur (humain) | Vérifier la sauvegarde avant déploiement et les logs Flyway après. |

---

## Résumé

* PostgreSQL 18 partout ; Flyway seul maître du schéma ; Hibernate en `validate`.
* Texte libre en collation `ci_ai` : recherches insensibles à la casse et aux accents.
* Toute évolution d'entité = une migration `V<n>` dans le même changement ; une vue = son fichier `R__`.
* Jamais de modification d'une migration appliquée ; jamais de script manuel sur la production.
* Données existantes analysées avant, contrôlées après ; aucune écriture sans validation explicite.
