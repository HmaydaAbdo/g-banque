# Base de données — Migrations Flyway et cohérence

> La base de données de production est vivante. Toute évolution du schéma ou des données est écrite comme une migration Flyway versionnée, relue, testée sur une base vierge **et** sur une copie de la production, puis appliquée automatiquement au démarrage du backend.

---

## Principes

* **Moteur unique : MySQL 8**, en développement, en test, en CI et en production (`mysql:8.0` du `docker-compose.yml`, Testcontainers `mysql:8.0`). Aucun autre moteur (MariaDB/XAMPP, H2) n'est utilisé pour développer ou tester.
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
* **Une migration appliquée n'est jamais modifiée** (Flyway refuse de démarrer si son empreinte change). Une erreur se corrige par une nouvelle migration.
* Une migration = un objectif. En-tête obligatoire : objectif, tables concernées, impact sur les données existantes, requêtes de contrôle.
* Une vue se modifie en modifiant son fichier `R__` (commencer par `DROP VIEW IF EXISTS` puis `CREATE VIEW`). Les littéraux texte portent un `COLLATE` explicite aligné sur les colonnes (la connexion JDBC n'a pas la même collation que les tables).
* `V1__schema_initial.sql` est le schéma de la production au 10/10/2026 (adoption de Flyway). Sur la production, elle n'a pas été exécutée : la base y a été marquée « baseline » en version 1 (`baseline-on-migrate`).

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
| Nom = mot réservé MySQL ? | Choisir un autre nom (`@Column(name = "...")`). |

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
4. **Tester** : sur une base vierge (tests d'intégration, automatique) **et** sur une copie restaurée de la sauvegarde de production (cf. `docs/infra/backup-mysql.md`), backend démarré contre cette copie.

Toute écriture sur des données de production (UPDATE/DELETE dans une migration) exige un accord explicite préalable.

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

* MySQL 8 partout ; Flyway seul maître du schéma ; Hibernate en `validate`.
* Toute évolution d'entité = une migration `V<n>` dans le même changement ; une vue = son fichier `R__`.
* Jamais de modification d'une migration appliquée ; jamais de script manuel sur la production.
* Données existantes analysées avant, contrôlées après ; aucune écriture sans validation explicite.
