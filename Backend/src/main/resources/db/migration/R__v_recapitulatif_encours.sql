-- =============================================================================
-- Vue v_recapitulatif_encours
-- =============================================================================
-- PostgreSQL 18 (10/10/2026) : les littéraux et expressions texte portent COLLATE ci_ai (collation de la base
-- pour le texte libre, insensible à la casse et aux accents) pour que toutes les branches du UNION aient la même
-- collation ; (a || b) remplace CONCAT(a, b) car, comme sous MySQL, il renvoie NULL si b est NULL.
--
-- Migration Flyway RÉPÉTABLE (R__) : ré-appliquée automatiquement au démarrage du backend
-- dès que ce fichier change. Ne plus l'exécuter à la main. Modifier la vue = modifier ce fichier.
--
-- Consolide en une seule requête paginable/triable/agrégeable les 20 sources
-- d'encours (Leasing, CMT, Cheque, Lcn, Virement, VirementInternational,
-- MiseADisposition, Prefinancement, Factoring, DroitConstate, Decompte,
-- Restitution, Commission, Agios, Prelevement, FraisBancaire, ImpotEtTaxe,
-- Impayee, OperationErrone, Provision) qui composent l'écran "Récapitulatifs" (workspace
-- États des encours) et le tableau de bord "Récapitulatif global".
--
-- Mappée en lecture seule par staport.finance.gBanque.recapitulatif.entities
-- .RecapitulatifEncoursView (@Subselect "SELECT * FROM v_recapitulatif_encours").
--
-- Consommateurs confirmés (à ne jamais casser sans mettre à jour le code Java
-- en même temps que cette vue) :
--   - RecapitulatifServiceImpl (écran Récapitulatifs, export PDF)
--   - RecapGeneralDetailleServiceImpl (Récapitulatif global, buildResume /
--     buildDecaissements / buildEncaissements) via RecapitulatifEncoursRepository
--     .sumByBanqueAndSourceBetween / .sumByBanqueAndSensUntil
--
-- Contrat de colonnes attendu par RecapitulatifEncoursView (chaque branche du
-- UNION ALL ci-dessous doit produire exactement ces 14 colonnes — seuls les
-- NOMS comptent pour Hibernate, pas la position) :
--   source_type             -> RecapitulatifEncoursId.sourceType (TransactionSource)
--   entity_id               -> RecapitulatifEncoursId.entityId
--   banque_id               -> banqueId
--   sens                    -> sens (ENCAISSEMENT | DECAISSEMENT)
--   date_reference          -> dateReference (pilote le filtre "du mois en cours")
--   date_emission           -> dateEmission (colonne "Date Émission")
--   date_echeance_affichage -> dateEcheanceAffichage (colonne "Date Échéance",
--                              NULL quand la source n'a pas d'échéance distincte)
--   montant                 -> montant
--   libelle                 -> libelle
--   observation             -> observation
--   numero                  -> numero (nullable — numéro de pièce quand la
--                              source en a un ; NULL sinon)
--   recherche_complementaire -> rechercheComplementaire (nullable, invisible à
--                              l'écran — colonne de recherche uniquement, ajoutée
--                              le 21/09/2026. Porte l'émetteur (Virement, Chèque,
--                              LCN, VirementInternational, MiseADisposition) ou
--                              l'organisme (Leasing) : des valeurs que la vue
--                              perdait ou n'exposait jamais dans libelle/
--                              observation selon le sens, alors que les anciens
--                              écrans par source permettaient de les rechercher.
--                              Combinée par OR avec libelle/observation dans
--                              RecapitulatifEncoursSpecification
--                              .byLibelleOuObservation. NULL pour les sources qui
--                              n'ont ni émetteur ni organisme distinct.
--   commission_operation    -> commissionOperation (nullable, invisible à
--                              l'écran — le motif/type d'une Commission,
--                              ajouté le 21/09/2026 ; NULL hors branche
--                              COMMISSION)
--   impot_taxe_motif        -> impotTaxeMotif (nullable, invisible à l'écran —
--                              le motif d'un Impôt/Taxe, ajouté le 21/09/2026 ;
--                              NULL hors branche IMPOT_TAXE)
--
-- -----------------------------------------------------------------------------
-- IMPORTANT — COLLATE utf8mb4_unicode_ci : NE PAS SUPPRIMER
-- -----------------------------------------------------------------------------
-- Chaque expression de type chaîne (source_type, sens, libelle, observation,
-- numero, recherche_complementaire, commission_operation, impot_taxe_motif)
-- porte un COLLATE utf8mb4_unicode_ci EXPLICITE. Ce n'est pas décoratif : sans
-- lui, les littéraux SQL ('DECAISSEMENT', 'CHEQUE', ...) prennent la collation
-- de la CONNEXION qui exécute ce script (phpMyAdmin ouvre en
-- utf8mb4_general_ci), alors que les colonnes des tables sources sont en
-- utf8mb4_unicode_ci (--collation-server du conteneur MySQL). La colonne de la
-- vue se retrouve alors dans une collation différente de celle du paramètre lié
-- par le driver JDBC, et tout filtre sur une colonne texte échoue à l'exécution
-- avec :
--
--   SQL Error 1267 — Illegal mix of collations
--   (utf8mb4_unicode_ci,COERCIBLE) and (utf8mb4_general_ci,COERCIBLE)
--   for operation '='
--
-- concrètement : le filtre par sens (bySens), par source (bySources) et les
-- LIKE sur libellé/observation/recherche complémentaire
-- (byLibelleOuObservation) ou l'égalité sur motif (byCommissionOperation /
-- byImpotTaxeMotif). Un COLLATE explicite a la coercibilité 0 : il l'emporte
-- sur le paramètre JDBC quelle que soit la collation de connexion, et rend la
-- vue indépendante du client qui la crée. C'est exactement ce que faisait la
-- version d'origine du 17/07/2026.
--
-- -----------------------------------------------------------------------------
-- Historique
-- -----------------------------------------------------------------------------
-- 19/10/2026 (HMAYDA + Claude) — branche LEASING : une avance rattachée à un
--                        chèque / une LCN / un virement non annulé n'est plus
--                        listée (le moyen de paiement la représente) — voir V16.
-- 21/09/2026 (HMAYDA + Claude) — AJOUT de 3 colonnes de recherche invisibles
--                        (`recherche_complementaire`, `commission_operation`,
--                        `impot_taxe_motif`) pour restaurer, dans le récap
--                        unifié, des critères de recherche qui existaient dans
--                        les anciens écrans séparés par source et avaient été
--                        perdus lors de la fusion (émetteur, organisme Leasing,
--                        motif Commission/ImpôtTaxe) — voir le contrat de
--                        colonnes ci-dessus pour le détail par colonne.
-- 17/07/2026 (HMAYDA) — Création manuelle initiale, jamais versionnée (fichier
--                        absent du dépôt malgré la javadoc de l'entité qui le
--                        référençait déjà à l'époque).
-- 06/09/2026 (HMAYDA + Claude) — Recréation depuis zéro, versionnée pour la
--                        première fois. Changements par rapport à la version
--                        du 17/07/2026 (relevée via `SHOW CREATE VIEW`) :
--   1. FIX — branche LEASING : ne sélectionnait que 4 colonnes sur les 10
--            attendues (source_type, entity_id, banque_id, sens), sans montant/
--            libelle/observation/dates. Reconstruite sur le même schéma que la
--            branche CMT, avec les colonnes réelles déjà présentes sur
--            echeance_leasing (montant_ttc, libelle, observation, non nulles).
--   2. FIX — branche CMT : utilisait un texte générique fixe ('Échéance CMT' /
--            'Prélèvement CMT') pour TOUTES les lignes au lieu des colonnes
--            libelle/observation réelles d'echeance_cmt (elles aussi non
--            nulles) — chaque échéance perdait son libellé/observation propre.
--   3. AJOUT — colonne `numero` : expose le numéro de pièce quand la source en
--            porte un nativement (cheques.numero, lcns.numero, virements.numero,
--            mises_a_disposition.numero, impayee.numero, marches.numero pour
--            Prefinancement/Factoring/DroitConstate/Decompte via leur marché,
--            contrat_leasing.numero_contrat pour Leasing) ; NULL pour les
--            sources sans numéro de pièce (CMT, VirementInternational,
--            Restitution, Commission, Agios, Prelevement, FraisBancaire,
--            ImpotEtTaxe, OperationErrone). Auparavant noyé dans du texte libre
--            (ex. "Chèque N° 123" en observation), impossible à filtrer/trier
--            séparément.
--   4. CONSOLIDATION — CHEQUE et LCN étaient chacun scindés en 2 SELECT
--            distincts (un par sens, filtré par `WHERE type = 'X'` en dur) —
--            ramenés à 1 SELECT chacun avec `type` utilisé directement comme
--            `sens` et un CASE pour les colonnes qui varient par sens, sur le
--            modèle déjà utilisé par MiseADisposition/VirementInternational.
--            Sorties strictement identiques à l'ancienne version (mêmes
--            valeurs, mêmes filtres), aucun changement de comportement.
--            NB — Virement N'A PAS été consolidé malgré la même duplication
--            apparente : les 2 branches utilisent 2 colonnes banque_id
--            DIFFÉRENTES (banque_emettrice_id côté décaissement,
--            banque_beneficiare_id côté encaissement), donc un même virement
--            doit légitimement apparaître une fois pour chaque banque — les
--            fusionner ferait disparaître une des deux lignes.
--   7. AJOUT (07/09) — branche PROVISION. Le module Provision (entité, écrans
--            « Provisions » en décaissement et en encaissement, TransactionSource
--            .PROVISION) a été livré après la création de la vue, qui ne l'a
--            jamais couvert : aucune provision n'apparaissait dans l'écran
--            Récapitulatifs, et la catégorie « Provision » déclarée des deux
--            côtés par RecapGeneralDetailleServiceImpl restait à zéro. La table
--            porte sa propre colonne `sens` (SensOperation, non nulle) : elle est
--            reprise telle quelle, comme pour virements_internationaux.
--            NB : provision.date_emission est nullable ; une provision sans date
--            d'émission aura une date_reference nulle et ne sera donc jamais
--            retenue par le filtre de période.
--   6. FIX (07/09) — étiquette de date pour LEASING et CMT. Ces deux sources
--            n'ont pas de date d'émission (une échéance d'amortissement n'est pas
--            « émise ») : la vue rangeait quand même leur date_echeance dans la
--            colonne date_emission, si bien que l'écran affichait une échéance
--            sous l'intitulé « Date d'émission » et un « — » sous « Date
--            d'échéance ». Désormais date_emission = NULL et l'échéance est
--            exposée dans date_echeance_affichage, à sa place.
--            CONSÉQUENCE : le tri par défaut des écrans et de l'export PDF est
--            passé de dateEmission à dateReference — toujours renseignée, et
--            sémantiquement la bonne (date de rattachement à la période). Pour
--            17 sources sur 19 l'ordre affiché est inchangé (dateReference y est
--            égale à dateEmission) ; pour leasing et CMT il reste identique à
--            l'ordre d'avant ce correctif, puisque dateReference vaut l'échéance
--            qui était précédemment affichée en émission. Seules les LCN se
--            réordonnent, sur leur échéance plutôt que sur leur émission.
--   5. FIX (06/09, après le 1er déploiement) — rétablissement des COLLATE
--            explicites sur toutes les expressions texte : leur absence
--            provoquait l'erreur 1267 ci-dessus au moindre filtre par sens.
--
-- =============================================================================

DROP VIEW IF EXISTS v_recapitulatif_encours;

CREATE VIEW v_recapitulatif_encours AS

-- 1. LEASING (corrigée — voir historique point 1)
SELECT
    'LEASING' COLLATE ci_ai AS source_type,
    el.id AS entity_id,
    cl.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    el.date_echeance AS date_reference,
    CAST(NULL AS date) AS date_emission,
    el.date_echeance AS date_echeance_affichage,
    CAST(el.montant_ttc AS DECIMAL(19,2)) AS montant,
    el.libelle COLLATE ci_ai AS libelle,
    el.observation COLLATE ci_ai AS observation,
    cl.numero_contrat COLLATE ci_ai AS numero,
    ol.nom COLLATE ci_ai AS recherche_complementaire,
    CAST(NULL AS varchar) AS commission_operation,
    CAST(NULL AS varchar) AS impot_taxe_motif
FROM echeance_leasing el
JOIN contrat_leasing cl ON cl.id = el.contrat_leasing_id
JOIN contrat_cadre cc ON cc.id = cl.contrat_cadre_id
JOIN organisme_leasing ol ON ol.id = cc.organisme_id
WHERE el.statut_paiement = 'PREVU'
  -- Avance rattachée à un chèque / une LCN / un virement non annulé : c'est ce moyen de paiement qui la représente
  -- dans les encours (branches 3 à 5) — l'échéance n°1 n'est donc jamais comptée deux fois.
  AND NOT EXISTS (SELECT 1 FROM v_echeance_leasing_moyen vm
                  WHERE vm.echeance_id = el.id AND vm.statut_moyen <> 'ANNULE')

UNION ALL

-- 2. CMT (corrigée — voir historique point 2)
SELECT
    'CMT' COLLATE ci_ai AS source_type,
    ec.id AS entity_id,
    cmt.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    ec.date_echeance AS date_reference,
    NULL AS date_emission,
    ec.date_echeance AS date_echeance_affichage,
    CAST(ec.montant AS DECIMAL(19,2)) AS montant,
    ec.libelle COLLATE ci_ai AS libelle,
    ec.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM echeance_cmt ec
JOIN credit_moyen_terme cmt ON cmt.id = ec.credit_moyen_terme_id
WHERE ec.statut_paiement = 'PREVU'

UNION ALL

-- 3. CHEQUE (consolidée — voir historique point 4 ; sorties identiques à l'ancienne version)
SELECT
    'CHEQUE' COLLATE ci_ai AS source_type,
    c.id AS entity_id,
    c.banque_id AS banque_id,
    c.type COLLATE ci_ai AS sens,
    c.date_emission AS date_reference,
    c.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(c.montant AS DECIMAL(19,2)) AS montant,
    CASE WHEN c.type = 'ENCAISSEMENT' THEN ('Bénéficiaire ' || c.beneficiaire) ELSE c.beneficiaire END COLLATE ci_ai AS libelle,
    CASE WHEN c.type = 'ENCAISSEMENT' THEN ('emetteur  ' || c.emetteur) ELSE ('Chèque N° ' || c.numero) END COLLATE ci_ai AS observation,
    c.numero COLLATE ci_ai AS numero,
    c.emetteur COLLATE ci_ai AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM cheques c
WHERE c.statut = 'ENCOURS'

UNION ALL

-- 4. LCN (consolidée — voir historique point 4 ; sorties identiques à l'ancienne version,
--    y compris la distinction LCN_ENCAISSEMENT / LCN_ESCOMPTE selon nature_lcn)
SELECT
    CASE
        WHEN l.type = 'DECAISSEMENT' THEN 'LCN_DECAISSEMENT'
        WHEN l.nature_lcn = 'ENCAISSEMENT' THEN 'LCN_ENCAISSEMENT'
        ELSE 'LCN_ESCOMPTE'
    END COLLATE ci_ai AS source_type,
    l.id AS entity_id,
    l.banque_id AS banque_id,
    l.type COLLATE ci_ai AS sens,
    CASE WHEN l.type = 'ENCAISSEMENT' AND l.nature_lcn = 'ESCOMPTE' THEN l.date_emission ELSE l.date_echeance END AS date_reference,
    l.date_emission AS date_emission,
    l.date_echeance AS date_echeance_affichage,
    CAST(l.montant AS DECIMAL(19,2)) AS montant,
    l.beneficiaire COLLATE ci_ai AS libelle,
    CASE WHEN l.type = 'DECAISSEMENT' THEN ('LCN N° ' || l.numero) ELSE l.emetteur END COLLATE ci_ai AS observation,
    l.numero COLLATE ci_ai AS numero,
    l.emetteur COLLATE ci_ai AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM lcns l
WHERE l.statut = 'ENCOURS'
  AND (l.type = 'DECAISSEMENT' OR (l.type = 'ENCAISSEMENT' AND l.nature_lcn IN ('ENCAISSEMENT', 'ESCOMPTE')))

UNION ALL

-- 5a. Virement — décaissement (banque émettrice). NON consolidée avec 5b, volontairement —
--     voir historique point 4 (NB).
SELECT
    'Virement' COLLATE ci_ai AS source_type,
    v.id AS entity_id,
    v.banque_emettrice_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    v.date_emission AS date_reference,
    v.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(v.montant AS DECIMAL(19,2)) AS montant,
    v.beneficiaire COLLATE ci_ai AS libelle,
    ('Virement N° ' || v.numero) COLLATE ci_ai AS observation,
    v.numero COLLATE ci_ai AS numero,
    v.emetteur COLLATE ci_ai AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM virements v
WHERE v.statut = 'ENCOURS' AND v.type = 'DECAISSEMENT'

UNION ALL

-- 5b. Virement — encaissement (banque bénéficiaire)
SELECT
    'Virement' COLLATE ci_ai AS source_type,
    v.id AS entity_id,
    v.banque_beneficiare_id AS banque_id,
    'ENCAISSEMENT' COLLATE ci_ai AS sens,
    v.date_emission AS date_reference,
    v.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(v.montant AS DECIMAL(19,2)) AS montant,
    v.beneficiaire COLLATE ci_ai AS libelle,
    v.emetteur COLLATE ci_ai AS observation,
    v.numero COLLATE ci_ai AS numero,
    v.emetteur COLLATE ci_ai AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM virements v
WHERE v.statut = 'ENCOURS' AND v.type = 'ENCAISSEMENT'

UNION ALL

-- 6. VIREMENT_INTERNATIONAL (inchangée + numero, + recherche_complementaire = bénéficiaire/émetteur)
SELECT
    'VIREMENT_INTERNATIONAL' COLLATE ci_ai AS source_type,
    vi.id AS entity_id,
    vi.banque_id AS banque_id,
    vi.type COLLATE ci_ai AS sens,
    vi.date_emission AS date_reference,
    vi.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(vi.montant AS DECIMAL(19,2)) AS montant,
    vi.libelle COLLATE ci_ai AS libelle,
    vi.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    CONCAT_WS(' ', vi.beneficiaire, vi.emetteur) COLLATE ci_ai AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM virements_internationaux vi
WHERE vi.statut = 'ENCOURS'

UNION ALL

-- 7. MiseADisposition (inchangée + numero, + recherche_complementaire = émetteur)
SELECT
    'MiseADisposition' COLLATE ci_ai AS source_type,
    m.id AS entity_id,
    m.banque_id AS banque_id,
    m.type COLLATE ci_ai AS sens,
    m.date_emission AS date_reference,
    m.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(m.montant AS DECIMAL(19,2)) AS montant,
    CASE WHEN m.type = 'ENCAISSEMENT' THEN ('Bénéficiaire ' || m.beneficiaire) ELSE m.beneficiaire END COLLATE ci_ai AS libelle,
    ('Mise à disposition N° ' || m.numero) COLLATE ci_ai AS observation,
    m.numero COLLATE ci_ai AS numero,
    m.emetteur COLLATE ci_ai AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM mises_a_disposition m
WHERE m.statut = 'ENCOURS'

UNION ALL

-- 8. PREFINANCEMENT (inchangée + numero = numéro du marché)
SELECT
    'PREFINANCEMENT' COLLATE ci_ai AS source_type,
    p.id AS entity_id,
    mc.banque_id AS banque_id,
    CASE WHEN p.type = 'REMBOURSEMENT' THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END COLLATE ci_ai AS sens,
    p.date_emission AS date_reference,
    p.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(p.montant AS DECIMAL(19,2)) AS montant,
    ('Marché N° : ' || mc.numero) COLLATE ci_ai AS libelle,
    CASE WHEN p.type = 'REMBOURSEMENT' THEN 'Remboursement du préfinancement' ELSE 'Déblocage du préfinancement' END COLLATE ci_ai AS observation,
    mc.numero COLLATE ci_ai AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM prefinancement p
JOIN marches mc ON mc.id = p.marche_id
WHERE p.status = 'PREVU'

UNION ALL

-- 9. FACTORING (inchangée + numero = numéro du marché)
SELECT
    'FACTORING' COLLATE ci_ai AS source_type,
    f.id AS entity_id,
    mc.banque_id AS banque_id,
    CASE WHEN f.type = 'REMBOURSEMENT' THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END COLLATE ci_ai AS sens,
    f.date_emission AS date_reference,
    f.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(f.montant AS DECIMAL(19,2)) AS montant,
    ('Marché N° : ' || mc.numero) COLLATE ci_ai AS libelle,
    CASE WHEN f.type = 'REMBOURSEMENT' THEN 'Remboursement du factoring' ELSE 'Déblocage du factoring' END COLLATE ci_ai AS observation,
    mc.numero COLLATE ci_ai AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM factoring f
JOIN marches mc ON mc.id = f.marche_id
WHERE f.status = 'PREVU'

UNION ALL

-- 10. DROIT_CONSTATE (inchangée + numero = numéro du marché)
SELECT
    'DROIT_CONSTATE' COLLATE ci_ai AS source_type,
    dc.id AS entity_id,
    mc.banque_id AS banque_id,
    CASE WHEN dc.type = 'REMBOURSEMENT' THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END COLLATE ci_ai AS sens,
    dc.date_emission AS date_reference,
    dc.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(dc.montant AS DECIMAL(19,2)) AS montant,
    ('Marché N° : ' || mc.numero) COLLATE ci_ai AS libelle,
    CASE WHEN dc.type = 'REMBOURSEMENT' THEN 'Remboursement du droit constaté' ELSE 'Déblocage du droit constaté' END COLLATE ci_ai AS observation,
    mc.numero COLLATE ci_ai AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM droit_constate dc
JOIN marches mc ON mc.id = dc.marche_id
WHERE dc.status = 'PREVU'

UNION ALL

-- 11. DECOMPTES (inchangée + numero = numéro du marché)
SELECT
    'DECOMPTES' COLLATE ci_ai AS source_type,
    d.id AS entity_id,
    d.banque_encaissement_id AS banque_id,
    'ENCAISSEMENT' COLLATE ci_ai AS sens,
    d.date_emission AS date_reference,
    d.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(d.montant AS DECIMAL(19,2)) AS montant,
    ('Marché N° : ' || mc.numero) COLLATE ci_ai AS libelle,
    d.observation COLLATE ci_ai AS observation,
    mc.numero COLLATE ci_ai AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM decompte d
JOIN marches mc ON mc.id = d.marche_id
WHERE d.status = 'PREVU'

UNION ALL

-- 12. RESTITUTION (inchangée, pas de numéro de pièce)
SELECT
    'RESTITUTION' COLLATE ci_ai AS source_type,
    r.id AS entity_id,
    r.banque_id AS banque_id,
    'ENCAISSEMENT' COLLATE ci_ai AS sens,
    r.date_emission AS date_reference,
    r.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(r.montant AS DECIMAL(19,2)) AS montant,
    r.libelle COLLATE ci_ai AS libelle,
    r.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM restitution r
WHERE r.status = 'PREVU'

UNION ALL

-- 13. COMMISSION (inchangée, pas de numéro de pièce ; + commission_operation = motif)
SELECT
    'COMMISSION' COLLATE ci_ai AS source_type,
    co.id AS entity_id,
    co.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    co.date_emission AS date_reference,
    co.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(co.montant AS DECIMAL(19,2)) AS montant,
    co.libelle COLLATE ci_ai AS libelle,
    co.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    co.operation COLLATE ci_ai AS commission_operation,
    NULL AS impot_taxe_motif
FROM commission co
WHERE co.status = 'PREVU'

UNION ALL

-- 14. AGIOS (inchangée, pas de numéro de pièce)
SELECT
    'AGIOS' COLLATE ci_ai AS source_type,
    a.id AS entity_id,
    a.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    a.date_emission AS date_reference,
    a.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(a.montant AS DECIMAL(19,2)) AS montant,
    a.libelle COLLATE ci_ai AS libelle,
    a.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM agios a
WHERE a.status = 'PREVU'

UNION ALL

-- 15. PRELEVEMENT (inchangée, pas de numéro de pièce)
SELECT
    'PRELEVEMENT' COLLATE ci_ai AS source_type,
    pr.id AS entity_id,
    pr.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    pr.date_emission AS date_reference,
    pr.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(pr.montant AS DECIMAL(19,2)) AS montant,
    pr.libelle COLLATE ci_ai AS libelle,
    pr.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM prelevement pr
WHERE pr.status = 'PREVU'

UNION ALL

-- 16. FRAIS_BANCAIRE (inchangée, pas de numéro de pièce)
SELECT
    'FRAIS_BANCAIRE' COLLATE ci_ai AS source_type,
    fb.id AS entity_id,
    fb.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    fb.date_emission AS date_reference,
    fb.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(fb.montant AS DECIMAL(19,2)) AS montant,
    fb.libelle COLLATE ci_ai AS libelle,
    fb.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM frais_bancaire fb
WHERE fb.status = 'PREVU'

UNION ALL

-- 17. IMPOT_TAXE (inchangée, pas de numéro de pièce ; + impot_taxe_motif = motif)
SELECT
    'IMPOT_TAXE' COLLATE ci_ai AS source_type,
    it.id AS entity_id,
    it.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    it.date_emission AS date_reference,
    it.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(it.montant AS DECIMAL(19,2)) AS montant,
    it.libelle COLLATE ci_ai AS libelle,
    it.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    it.motif COLLATE ci_ai AS impot_taxe_motif
FROM impot_et_taxe it
WHERE it.status = 'PREVU'

UNION ALL

-- 18. IMPAYEE (inchangée + numero = im.numero, colonne déjà existante sur la table)
SELECT
    'IMPAYEE' COLLATE ci_ai AS source_type,
    im.id AS entity_id,
    im.banque_id AS banque_id,
    im.type COLLATE ci_ai AS sens,
    im.date_emission AS date_reference,
    im.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(im.montant AS DECIMAL(19,2)) AS montant,
    im.libelle COLLATE ci_ai AS libelle,
    im.observation COLLATE ci_ai AS observation,
    im.numero COLLATE ci_ai AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM impayee im
WHERE im.status = 'PREVU'

UNION ALL

-- 19. OPERATION_ERRONE (inchangée, pas de numéro de pièce)
SELECT
    'OPERATION_ERRONE' COLLATE ci_ai AS source_type,
    oe.id AS entity_id,
    oe.banque_id AS banque_id,
    'DECAISSEMENT' COLLATE ci_ai AS sens,
    oe.date_emission AS date_reference,
    oe.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(oe.montant AS DECIMAL(19,2)) AS montant,
    oe.libelle COLLATE ci_ai AS libelle,
    oe.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM operation_errone oe
WHERE oe.status = 'PREVU'

UNION ALL

-- 20. PROVISION (ajoutée le 07/09/2026 — le module Provision a été créé après
--     la vue d'origine du 17/07, qui l'ignorait donc ; la catégorie « Provision »
--     de RecapGeneralDetailleServiceImpl affichait zéro en permanence)
SELECT
    'PROVISION' COLLATE ci_ai AS source_type,
    pv.id AS entity_id,
    pv.banque_id AS banque_id,
    pv.sens COLLATE ci_ai AS sens,
    pv.date_emission AS date_reference,
    pv.date_emission AS date_emission,
    NULL AS date_echeance_affichage,
    CAST(pv.montant AS DECIMAL(19,2)) AS montant,
    pv.libelle COLLATE ci_ai AS libelle,
    pv.observation COLLATE ci_ai AS observation,
    NULL AS numero,
    NULL AS recherche_complementaire,
    NULL AS commission_operation,
    NULL AS impot_taxe_motif
FROM provision pv
WHERE pv.status = 'PREVU';
