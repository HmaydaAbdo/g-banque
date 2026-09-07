# Base de données — Migrations et cohérence

> La base de données de production est vivante. Toute évolution du schéma ou des données doit être préparée, communiquée et validée avant d'être appliquée.

---

## Principe fondamental

Le projet utilise `spring.jpa.hibernate.ddl-auto=update`. Hibernate applique automatiquement les changements de schéma au démarrage. Ce mécanisme est limité : il **ajoute** mais ne **renomme pas**, ne **supprime pas** de colonnes et ne **migre pas** les données existantes.

Toute évolution qui dépasse ce que Hibernate peut faire automatiquement doit être signalée et traitée manuellement.

---

## Checklist obligatoire avant toute modification d'entité

Avant de modifier une entité JPA, répondre aux questions suivantes et signaler chaque point concerné :

### 1. Changement de schéma (DDL)

| Question | Action requise si oui |
|---|---|
| Nouvelle table ? | Hibernate la crée automatiquement — vérifier les index |
| Nouvelle colonne nullable ? | Hibernate l'ajoute — aucune action supplémentaire |
| Nouvelle colonne NOT NULL sur une table non vide ? | **Bloquer** — nécessite une valeur par défaut ou une migration DML préalable |
| Colonne renommée ? | **Bloquer** — Hibernate crée une nouvelle colonne, l'ancienne reste (dead column) |
| Colonne supprimée de l'entité ? | **Bloquer** — Hibernate ne supprime pas la colonne en base (`update` ne fait jamais DROP) |
| Type de colonne modifié ? | **Bloquer** — Hibernate peut échouer ou corrompre des données |
| Enum étendu (nouvelle valeur) ? | Hibernate modifie l'enum en base — vérifier que MariaDB accepte la syntaxe |
| Enum réduit (valeur supprimée) ? | **Bloquer** — les lignes existantes avec cette valeur deviennent invalides |
| Contrainte UNIQUE ajoutée ? | **Bloquer** si des doublons existent déjà en base |
| Index ajouté ? | Hibernate l'ajoute — acceptable, mais vérifier l'impact sur les tables volumineuses |
| Nom de colonne = mot réservé SQL/MariaDB ? | **Bloquer** — utiliser `@Column(name = "...")` avec un nom alternatif |

### 2. Migration de données (DML)

| Situation | Action requise |
|---|---|
| Nouvelle colonne NOT NULL avec données existantes | Fournir le script `UPDATE` à exécuter avant le démarrage |
| Colonne renommée avec données à conserver | Fournir le script `UPDATE … SET nouvelle = ancienne` + DROP ancienne après validation |
| Données à transformer (ex. format, enum changé) | Fournir le script `UPDATE` avec la logique de transformation |
| Seed data obligatoire pour le fonctionnement de la fonctionnalité | Fournir le script `INSERT` ou documenter l'insertion via l'UI |
| Suppression de données liées à une entité supprimée | Fournir le script `DELETE` ou vérifier que les FK avec CASCADE couvrent le cas |

---

## Analyse obligatoire des données existantes

Avant tout changement de schéma, **analyser systématiquement l'impact sur les données déjà en base**. Cette analyse précède l'écriture de tout code.

### Étapes d'analyse

**Étape 1 — Identifier les tables impactées**

Pour chaque entité modifiée, identifier les tables en base concernées (nom de table = `@Table(name = ...)`).

**Étape 2 — Évaluer le volume et l'état des données**

Fournir les requêtes de diagnostic à exécuter sur la prod pour connaître l'état réel avant d'agir :

```sql
-- Volume général
SELECT COUNT(*) FROM nom_table;

-- Distribution d'un enum / statut
SELECT colonne, COUNT(*) FROM nom_table GROUP BY colonne;

-- Vérifier les doublons avant ajout d'une contrainte UNIQUE
SELECT colonne, COUNT(*) FROM nom_table GROUP BY colonne HAVING COUNT(*) > 1;

-- Vérifier les valeurs NULL avant ajout d'une contrainte NOT NULL
SELECT COUNT(*) FROM nom_table WHERE colonne IS NULL;

-- Vérifier les lignes portant une valeur d'enum à supprimer
SELECT COUNT(*) FROM nom_table WHERE colonne = 'VALEUR_A_SUPPRIMER';
```

**Étape 3 — Fournir les scripts de migration dans l'ordre**

Les scripts sont fournis dans l'ordre d'exécution strict, numérotés, avec leur objectif :

```sql
-- [1/3] Peupler la nouvelle colonne pour les lignes existantes
UPDATE nom_table SET nouvelle_colonne = 'VALEUR_PAR_DEFAUT' WHERE nouvelle_colonne IS NULL;

-- [2/3] Vérifier que toutes les lignes sont migrées
SELECT COUNT(*) FROM nom_table WHERE nouvelle_colonne IS NULL; -- doit retourner 0

-- [3/3] (après redémarrage) Supprimer l'ancienne colonne si renommage
ALTER TABLE nom_table DROP COLUMN ancienne_colonne;
```

**Étape 4 — Définir l'ordre de déploiement**

Certains changements imposent un ordre strict :

| Ordre | Étape |
|---|---|
| 1 | Exécuter les scripts DML de pré-migration |
| 2 | Vérifier les résultats des scripts (requêtes de contrôle) |
| 3 | Démarrer l'application (Hibernate applique le DDL) |
| 4 | Vérifier que l'application démarre sans erreur |
| 5 | Exécuter les scripts DML de post-migration si nécessaire (ex. DROP ancienne colonne) |

---

## Format de signalement

Lorsqu'une modification d'entité déclenche un ou plusieurs points de la checklist, **signaler explicitement avant d'écrire le code**, en respectant ce format :

```
⚠️ CHANGEMENT BASE DE DONNÉES DÉTECTÉ

Table(s) concernée(s) : nom_table

─── SCHÉMA (DDL) ───────────────────────────────────
Changement    : [ex. colonne `statut` renommée en `etat`]
Hibernate     : BLOQUÉ / automatique
Risque        : [ex. données perdues si Hibernate crée une nouvelle colonne vide]

─── DIAGNOSTIC (à exécuter sur la prod) ────────────
-- Vérifier l'état avant migration
SELECT COUNT(*) FROM nom_table;
SELECT statut, COUNT(*) FROM nom_table GROUP BY statut;

─── MIGRATION (DML) ─────────────────────────────────
-- [1/2] Pré-migration — exécuter AVANT le démarrage
ALTER TABLE nom_table CHANGE statut etat VARCHAR(20) NOT NULL;
UPDATE nom_table SET etat = statut; -- si applicable

-- [2/2] Contrôle — vérifier que la migration est complète
SELECT COUNT(*) FROM nom_table WHERE etat IS NULL; -- doit retourner 0

─── ORDRE DE DÉPLOIEMENT ───────────────────────────
1. Exécuter les scripts de pré-migration
2. Vérifier les résultats des requêtes de contrôle
3. Démarrer l'application
4. [si post-migration] Exécuter les scripts de nettoyage

⛔ NE PAS CONTINUER SANS VALIDATION EXPLICITE.
```

**Règle absolue** : aucun code Java modifiant le schéma d'une table non vide n'est écrit avant que le signalement soit validé et les scripts confirmés exécutés.

---

## Cas fréquents et solutions

### Nouvelle colonne NOT NULL sur table existante

**Problème :** Hibernate ajoute la colonne mais ne peut pas définir de valeur pour les lignes existantes → erreur au démarrage.

**Solution :**
1. Ajouter la colonne comme `nullable` dans un premier déploiement.
2. Exécuter le script DML de migration des données.
3. Ajouter la contrainte `NOT NULL` dans un second déploiement.

Ou : définir une valeur par défaut temporaire (`@Column(columnDefinition = "... DEFAULT 'VALEUR'")`), migrer, puis retirer le défaut.

### Colonne renommée

**Problème :** Hibernate crée une nouvelle colonne vide — les données de l'ancienne colonne sont perdues.

**Solution :**
1. Exécuter manuellement : `ALTER TABLE t CHANGE ancien_nom nouveau_nom TYPE;`
2. Mettre à jour l'entité avec `@Column(name = "nouveau_nom")`.

### Enum — nouvelle valeur

Hibernate génère un `ALTER TABLE … MODIFY COLUMN … ENUM(…)` incluant la nouvelle valeur. Compatible MariaDB si la syntaxe est correcte. Tester en dev avant de pousser en prod.

### Enum — valeur supprimée

**Problème :** les lignes portant la valeur supprimée violent la contrainte de l'enum.

**Solution :**
1. Identifier et migrer les lignes : `UPDATE t SET colonne = 'VALEUR_VALIDE' WHERE colonne = 'VALEUR_SUPPRIMEE'`.
2. Valider qu'aucune ligne ne porte plus la valeur.
3. Supprimer la valeur de l'enum et redémarrer.

### Mot réservé MariaDB comme nom de colonne

**Problème :** Hibernate génère du DDL invalide — erreur SQL au démarrage.

**Solution :** toujours vérifier que le nom de champ Java ne correspond pas à un mot réservé MariaDB. Si c'est le cas, utiliser `@Column(name = "nom_alternatif")`.

Exemples de mots réservés à éviter : `condition`, `status`, `type`, `key`, `value`, `index`, `order`, `group`, `interval`, `schema`, `row`, `select`.

---

## Mots réservés MariaDB — liste de vigilance

Vérifier systématiquement les noms de champs d'entité contre cette liste avant tout `git push` :

`ADD`, `ALL`, `ALTER`, `AND`, `AS`, `ASC`, `BETWEEN`, `BY`, `CALL`, `CASE`, `CHECK`, `COLUMN`, `CONDITION`, `CONSTRAINT`, `CREATE`, `CROSS`, `CURRENT_DATE`, `CURRENT_TIME`, `CURRENT_TIMESTAMP`, `CURSOR`, `DATABASE`, `DAY_HOUR`, `DAY_MICROSECOND`, `DAY_MINUTE`, `DAY_SECOND`, `DEC`, `DECIMAL`, `DECLARE`, `DEFAULT`, `DELETE`, `DESC`, `DISTINCT`, `DIV`, `DOUBLE`, `DROP`, `DUAL`, `EACH`, `ELSE`, `ELSEIF`, `ENCLOSED`, `ESCAPED`, `EXISTS`, `EXIT`, `EXPLAIN`, `FETCH`, `FLOAT`, `FOR`, `FORCE`, `FOREIGN`, `FROM`, `FULLTEXT`, `GRANT`, `GROUP`, `HAVING`, `HOUR_MICROSECOND`, `HOUR_MINUTE`, `HOUR_SECOND`, `IF`, `IGNORE`, `IN`, `INDEX`, `INFILE`, `INNER`, `INOUT`, `INSENSITIVE`, `INSERT`, `INT`, `INTEGER`, `INTERVAL`, `INTO`, `IS`, `ITERATE`, `JOIN`, `KEY`, `KEYS`, `KILL`, `LEADING`, `LEAVE`, `LEFT`, `LIKE`, `LIMIT`, `LOAD`, `LOCK`, `LONG`, `LOOP`, `MATCH`, `MAXVALUE`, `MEDIUMINT`, `MINUTE_MICROSECOND`, `MINUTE_SECOND`, `MOD`, `MODIFIES`, `NATURAL`, `NOT`, `NULL`, `ON`, `OPTIMIZE`, `OPTION`, `ORDER`, `OUT`, `OUTER`, `OUTFILE`, `PRECISION`, `PRIMARY`, `PROCEDURE`, `PURGE`, `RANGE`, `READ`, `READS`, `REFERENCES`, `RELEASE`, `RENAME`, `REPEAT`, `REPLACE`, `REQUIRE`, `RESIGNAL`, `RESTRICT`, `RETURN`, `RIGHT`, `SCHEMA`, `SCHEMAS`, `SELECT`, `SENSITIVE`, `SET`, `SHOW`, `SIGNAL`, `SMALLINT`, `SQL`, `STATUS`, `TABLE`, `TERMINATED`, `THEN`, `TO`, `TRAILING`, `TRIGGER`, `TYPE`, `UNDO`, `UNION`, `UNIQUE`, `UNLOCK`, `UNSIGNED`, `UPDATE`, `USAGE`, `USE`, `USING`, `UTC_DATE`, `UTC_TIME`, `UTC_TIMESTAMP`, `VALUE`, `VALUES`, `VARBINARY`, `VARCHAR`, `VARYING`, `WHEN`, `WHERE`, `WHILE`, `WITH`, `WRITE`, `XOR`, `ZEROFILL`

---

## Responsabilités

| Qui | Quoi |
|---|---|
| Développeur (IA) | Signaler tout changement DDL ou DML avant de modifier le code |
| Développeur (IA) | Fournir le script SQL si une migration manuelle est nécessaire |
| Développeur (humain) | Valider le changement et exécuter le script sur la base de production |
| Développeur (humain) | Confirmer que la base est cohérente avant le redémarrage de l'application |

---

## Résumé

* Hibernate `update` ajoute mais ne supprime ni ne renomme jamais.
* Toute colonne NOT NULL sur table non vide = migration DML obligatoire.
* Tout renommage = script SQL manuel obligatoire.
* Tout enum réduit = nettoyage DML obligatoire avant le déploiement.
* Tout nom de champ = vérifier contre les mots réservés MariaDB.
* Aucun changement bloquant sans validation humaine explicite.
