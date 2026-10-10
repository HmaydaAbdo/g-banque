-- =============================================================================
-- Vue v_recap_moyens_paiement
-- =============================================================================
-- PostgreSQL 18 (10/10/2026) : les littéraux et expressions texte portent COLLATE ci_ai (collation de la base
-- pour le texte libre, insensible à la casse et aux accents) pour que toutes les branches du UNION aient la même
-- collation ; (a || b) remplace CONCAT(a, b) car, comme sous MySQL, il renvoie NULL si b est NULL.
--
-- Migration Flyway RÉPÉTABLE (R__) : ré-appliquée automatiquement au démarrage du backend
-- dès que ce fichier change. Ne plus l'exécuter à la main. Modifier la vue = modifier ce fichier.
--
-- Consolide en une seule requête paginable/triable/agrégeable les 5 moyens de
-- paiement de la section "Les moyens de paiement" (Cheque, Lcn, Virement,
-- VirementInternational, MiseADisposition) qui composent les écrans "Recap
-- Encaissement" et "Recap Décaissement" (chapitre 19 de la documentation
-- direction).
--
-- Contrairement à v_recapitulatif_encours (qui se limite aux encours du mois
-- en cours), cette vue est "tous statuts confondus" : aucune clause WHERE sur
-- le statut, chaque ligne source apparaît quel que soit son statut de
-- paiement.
--
-- Mappée en lecture seule par staport.finance.gBanque.moyensDePaiements.recap
-- .entities.RecapMoyenPaiementView (@Subselect "SELECT * FROM
-- v_recap_moyens_paiement").
--
-- Consommateur confirmé (à ne jamais casser sans mettre à jour le code Java en
-- même temps que cette vue) :
--   - RecapMoyenPaiementServiceImpl (écrans Recap Encaissement / Recap
--     Décaissement) via RecapMoyenPaiementRepository
--
-- Contrat de colonnes attendu par RecapMoyenPaiementView (chaque branche du
-- UNION ALL ci-dessous doit produire exactement ces 16 colonnes — seuls les
-- NOMS comptent pour Hibernate, pas la position) :
--   source_type   -> RecapMoyenPaiementId.sourceType (TransactionSource)
--   entity_id     -> RecapMoyenPaiementId.entityId
--   banque_id     -> banqueId
--   banque_nom    -> banqueNom
--   type          -> type (ENCAISSEMENT | DECAISSEMENT)
--   statut        -> statut (StatutMoyenPaiement)
--   numero        -> numero (nullable — NULL pour VirementInternational, qui
--                    n'a pas de colonne numero native)
--   date_emission -> dateEmission
--   date_operation -> dateOperation
--   date_valeur   -> dateValeur
--   date_echeance -> dateEcheance (nullable — seul Cheque et Lcn ont une
--                    échéance native ; NULL pour Virement, VirementInternational
--                    et MiseADisposition)
--   montant       -> montant
--   libelle       -> libelle
--   observation   -> observation
--   beneficiaire  -> beneficiaire
--   emetteur      -> emetteur (emetteur et beneficiaire sont tous les deux
--                    exposés bruts — le frontend choisit lequel afficher selon
--                    l'écran, voir la javadoc de RecapMoyenPaiementItem)
--
-- -----------------------------------------------------------------------------
-- IMPORTANT — COLLATE utf8mb4_unicode_ci : NE PAS SUPPRIMER
-- -----------------------------------------------------------------------------
-- Chaque expression de type chaîne porte un COLLATE utf8mb4_unicode_ci
-- EXPLICITE. Ce n'est pas décoratif : sans lui, les littéraux SQL ('CHEQUE',
-- 'Virement', ...) prennent la collation de la CONNEXION qui exécute ce script
-- (phpMyAdmin ouvre en utf8mb4_general_ci), alors que les colonnes des tables
-- sources sont en utf8mb4_unicode_ci (--collation-server du conteneur MySQL).
-- La colonne de la vue se retrouve alors dans une collation différente de
-- celle du paramètre lié par le driver JDBC, et tout filtre sur une colonne
-- texte échoue à l'exécution avec :
--
--   SQL Error 1267 — Illegal mix of collations
--   (utf8mb4_unicode_ci,COERCIBLE) and (utf8mb4_general_ci,COERCIBLE)
--   for operation '='
--
-- concrètement : les filtres byType, byStatuts, bySources, et les LIKE sur
-- libelle/numero (byLibelle / byNumero). Un COLLATE explicite a la
-- coercibilité 0 : il l'emporte sur le paramètre JDBC quelle que soit la
-- collation de connexion, et rend la vue indépendante du client qui la crée
-- (exactement le même bug que celui rencontré et corrigé sur
-- v_recapitulatif_encours — voir son propre fichier).
--
-- -----------------------------------------------------------------------------
-- Historique
-- -----------------------------------------------------------------------------
-- 31/07/2026 (HMAYDA) — Création manuelle initiale, jamais versionnée. La
--                        javadoc de RecapMoyenPaiementView affirmait pourtant
--                        (à tort) "voir sql/v_recap_moyens_paiement.sql ...
--                        script désormais versionné dans le dépôt" — ce
--                        fichier n'a en réalité jamais existé dans aucun des
--                        3 dépôts avant ce commit.
-- 07/09/2026 (HMAYDA + Claude) — Versionnée pour la première fois, à partir
--                        d'un export `SHOW CREATE VIEW v_recap_moyens_paiement`
--                        vérifié colonne par colonne contre
--                        RecapMoyenPaiementView.java (16 colonnes, tous types
--                        confirmés) et Cheque.java (branche CHEQUE — numero,
--                        date_emission, date_operation, date_valeur,
--                        date_echeance, montant, libelle, observation,
--                        beneficiaire, emetteur sont bien des colonnes
--                        natives de `cheques`, pas des valeurs synthétisées).
--                        Un premier export collé dans la conversation semblait
--                        montrer une branche CHEQUE tronquée à 4 colonnes sur
--                        16 (impossible en pratique : MySQL refuse un
--                        UNION ALL aux branches de largeurs différentes) — un
--                        second export propre a confirmé qu'il s'agissait
--                        d'une troncature de copier-coller, pas d'un défaut en
--                        base.
--                        FIX préventif : `source_type` n'avait de COLLATE explicite
--                        sur AUCUNE des 7 branches (contrairement à toutes les autres
--                        colonnes texte), alors que bySources() filtre exactement
--                        dessus (IN (...)). C'est le même bug 1267 que celui déjà
--                        rencontré et corrigé sur v_recapitulatif_encours, pas encore
--                        déclenché ici faute d'avoir été utilisé avec ce filtre. Un
--                        COLLATE utf8mb4_unicode_ci a été ajouté sur le littéral
--                        source_type des 7 branches. En dehors de ce point, aucun
--                        autre changement de comportement : le reste de la vue
--                        ci-dessous produit exactement les mêmes lignes que celle
--                        déjà en production.
--
-- =============================================================================

DROP VIEW IF EXISTS v_recap_moyens_paiement;

CREATE VIEW v_recap_moyens_paiement AS

-- 1. CHEQUE
SELECT
    'CHEQUE' COLLATE ci_ai AS source_type,
    c.id AS entity_id,
    c.banque_id AS banque_id,
    b.nom COLLATE ci_ai AS banque_nom,
    c.type COLLATE ci_ai AS type,
    c.statut COLLATE ci_ai AS statut,
    c.numero COLLATE ci_ai AS numero,
    c.date_emission AS date_emission,
    c.date_operation AS date_operation,
    c.date_valeur AS date_valeur,
    c.date_echeance AS date_echeance,
    c.montant AS montant,
    c.libelle COLLATE ci_ai AS libelle,
    c.observation COLLATE ci_ai AS observation,
    c.beneficiaire COLLATE ci_ai AS beneficiaire,
    c.emetteur COLLATE ci_ai AS emetteur
FROM cheques c
LEFT JOIN banque b ON b.id = c.banque_id

UNION ALL

-- 2. LCN_ESCOMPTE
SELECT
    'LCN_ESCOMPTE' COLLATE ci_ai AS source_type,
    l.id AS entity_id,
    l.banque_id AS banque_id,
    b.nom COLLATE ci_ai AS banque_nom,
    l.type COLLATE ci_ai AS type,
    l.statut COLLATE ci_ai AS statut,
    l.numero COLLATE ci_ai AS numero,
    l.date_emission AS date_emission,
    l.date_operation AS date_operation,
    l.date_valeur AS date_valeur,
    l.date_echeance AS date_echeance,
    l.montant AS montant,
    l.libelle COLLATE ci_ai AS libelle,
    l.observation COLLATE ci_ai AS observation,
    l.beneficiaire COLLATE ci_ai AS beneficiaire,
    l.emetteur COLLATE ci_ai AS emetteur
FROM lcns l
LEFT JOIN banque b ON b.id = l.banque_id
WHERE l.nature_lcn = 'ESCOMPTE'

UNION ALL

-- 3. LCN_ENCAISSEMENT
SELECT
    'LCN_ENCAISSEMENT' COLLATE ci_ai AS source_type,
    l.id AS entity_id,
    l.banque_id AS banque_id,
    b.nom COLLATE ci_ai AS banque_nom,
    l.type COLLATE ci_ai AS type,
    l.statut COLLATE ci_ai AS statut,
    l.numero COLLATE ci_ai AS numero,
    l.date_emission AS date_emission,
    l.date_operation AS date_operation,
    l.date_valeur AS date_valeur,
    l.date_echeance AS date_echeance,
    l.montant AS montant,
    l.libelle COLLATE ci_ai AS libelle,
    l.observation COLLATE ci_ai AS observation,
    l.beneficiaire COLLATE ci_ai AS beneficiaire,
    l.emetteur COLLATE ci_ai AS emetteur
FROM lcns l
LEFT JOIN banque b ON b.id = l.banque_id
WHERE (l.nature_lcn IS NULL OR l.nature_lcn <> 'ESCOMPTE')
  AND l.type = 'ENCAISSEMENT'

UNION ALL

-- 4. LCN_DECAISSEMENT
SELECT
    'LCN_DECAISSEMENT' COLLATE ci_ai AS source_type,
    l.id AS entity_id,
    l.banque_id AS banque_id,
    b.nom COLLATE ci_ai AS banque_nom,
    l.type COLLATE ci_ai AS type,
    l.statut COLLATE ci_ai AS statut,
    l.numero COLLATE ci_ai AS numero,
    l.date_emission AS date_emission,
    l.date_operation AS date_operation,
    l.date_valeur AS date_valeur,
    l.date_echeance AS date_echeance,
    l.montant AS montant,
    l.libelle COLLATE ci_ai AS libelle,
    l.observation COLLATE ci_ai AS observation,
    l.beneficiaire COLLATE ci_ai AS beneficiaire,
    l.emetteur COLLATE ci_ai AS emetteur
FROM lcns l
LEFT JOIN banque b ON b.id = l.banque_id
WHERE (l.nature_lcn IS NULL OR l.nature_lcn <> 'ESCOMPTE')
  AND l.type = 'DECAISSEMENT'

UNION ALL

-- 5. Virement (une seule branche pour les 2 sens : le CASE choisit la bonne
--    banque, contrairement à v_recapitulatif_encours qui a besoin de 2
--    branches distinctes pour ce même type — voir l'historique de ce dernier
--    fichier pour le pourquoi de cette différence)
SELECT
    'Virement' COLLATE ci_ai AS source_type,
    v.id AS entity_id,
    CASE WHEN v.type = 'DECAISSEMENT' THEN v.banque_emettrice_id ELSE v.banque_beneficiare_id END AS banque_id,
    CASE WHEN v.type = 'DECAISSEMENT' THEN be.nom ELSE bb.nom END COLLATE ci_ai AS banque_nom,
    v.type COLLATE ci_ai AS type,
    v.statut COLLATE ci_ai AS statut,
    v.numero COLLATE ci_ai AS numero,
    v.date_emission AS date_emission,
    v.date_operation AS date_operation,
    v.date_valeur AS date_valeur,
    CAST(NULL AS date) AS date_echeance,
    v.montant AS montant,
    v.libelle COLLATE ci_ai AS libelle,
    v.observation COLLATE ci_ai AS observation,
    v.beneficiaire COLLATE ci_ai AS beneficiaire,
    v.emetteur COLLATE ci_ai AS emetteur
FROM virements v
LEFT JOIN banque be ON be.id = v.banque_emettrice_id
LEFT JOIN banque bb ON bb.id = v.banque_beneficiare_id

UNION ALL

-- 6. VIREMENT_INTERNATIONAL (pas de colonne numero native -> NULL)
SELECT
    'VIREMENT_INTERNATIONAL' COLLATE ci_ai AS source_type,
    vi.id AS entity_id,
    vi.banque_id AS banque_id,
    b.nom COLLATE ci_ai AS banque_nom,
    vi.type COLLATE ci_ai AS type,
    vi.statut COLLATE ci_ai AS statut,
    CAST(NULL AS varchar) COLLATE ci_ai AS numero,
    vi.date_emission AS date_emission,
    vi.date_operation AS date_operation,
    vi.date_valeur AS date_valeur,
    CAST(NULL AS date) AS date_echeance,
    vi.montant AS montant,
    vi.libelle COLLATE ci_ai AS libelle,
    vi.observation COLLATE ci_ai AS observation,
    vi.beneficiaire COLLATE ci_ai AS beneficiaire,
    vi.emetteur COLLATE ci_ai AS emetteur
FROM virements_internationaux vi
LEFT JOIN banque b ON b.id = vi.banque_id

UNION ALL

-- 7. MiseADisposition
SELECT
    'MiseADisposition' COLLATE ci_ai AS source_type,
    m.id AS entity_id,
    m.banque_id AS banque_id,
    b.nom COLLATE ci_ai AS banque_nom,
    m.type COLLATE ci_ai AS type,
    m.statut COLLATE ci_ai AS statut,
    m.numero COLLATE ci_ai AS numero,
    m.date_emission AS date_emission,
    m.date_operation AS date_operation,
    m.date_valeur AS date_valeur,
    CAST(NULL AS date) AS date_echeance,
    m.montant AS montant,
    m.libelle COLLATE ci_ai AS libelle,
    m.observation COLLATE ci_ai AS observation,
    m.beneficiaire COLLATE ci_ai AS beneficiaire,
    m.emetteur COLLATE ci_ai AS emetteur
FROM mises_a_disposition m
LEFT JOIN banque b ON b.id = m.banque_id;
