-- =============================================================================
-- Backfill libellé — VIREMENTS / CHEQUES / LCNS / MISES_A_DISPOSITION + transaction
-- =============================================================================
--
-- Contexte (diagnostic du 07/09/2026, discuté avec HMAYDA).
--
-- Deux bugs différents, une seule cible de correction (le libellé de ces 4
-- moyens de paiement, et de la Transaction produite à leur validation) :
--
-- 1. Les 4 batch processors FunPaiement (VirementPaymentProcessor,
--    ChequePaymentProcessor, LcnPaymentProcessor,
--    MiseADispositionPaymentProcessor) écrivaient
--      libelle = observation = "<TYPE> N° <numero>"
--    — le numéro seul, alors que bénéficiaire (et, pour Virement, émetteur)
--    étaient déjà disponibles sur l'item importé.
--
-- 2. Même la création manuelle (VirementMapper/ChequeMapper/LcnMapper/
--    MiseADispositionMapper.toEntity), qui a toujours construit un libellé
--    plus riche, affichait les DEUX champs avec un repli "N/A" :
--      "Décaissement - Bénéficiaire: X / Émetteur: N/A"
--    même quand émetteur n'a aucun sens pour un décaissement (repéré via
--    une ligne réelle : id 4670, "TRACTAFRIC EQUIPEMENT / ÉMETTEUR: N/A").
--
-- Gabarit final retenu (07/09/2026, HMAYDA) : un seul des deux champs,
-- déterminé par le sens, jamais "N/A" :
--   Virement/Cheque/Lcn : DECAISSEMENT -> "Décaissement - Bénéficiaire: X"
--                         ENCAISSEMENT -> "Encaissement - Émetteur: Y"
--   MiseADisposition     : DECAISSEMENT -> "Bénéficiaire: X"
--                         ENCAISSEMENT -> "Émetteur: Y"          (pas de préfixe sens)
--
-- Les 8 fichiers (4 processors + 4 mappers manuels) sont corrigés dans le
-- code (commit du 07/09/2026) ; ce script rattrape les données déjà en base,
-- qu'elles viennent du bug 1 (collées au numéro) ou du bug 2 (les deux champs
-- avec un N/A résiduel).
--
-- Portée : Virement, Cheque, LCN_DECAISSEMENT/LCN_ENCAISSEMENT,
-- MiseADisposition uniquement — les autres sources n'ont pas ce bug (audit du
-- 07/09/2026, mémoire projet côté agent).
--
-- Marqueur de détection (couvre les deux bugs à la fois) :
--   libelle = observation         -- bug 1 : collé au numéro
--   OR libelle LIKE '%N/A%'       -- bug 2 : champ non pertinent affiché en N/A
-- Un libellé correctement construit (nouveau gabarit) ne tombe jamais dans
-- l'un de ces deux cas.
--
-- MODE D'EMPLOI :
--   1. Exécuter la section 1 (diagnostic) et vérifier que les volumes sont
--      cohérents avec ce qui est attendu avant de continuer.
--   2. Démarrer une transaction, exécuter la section 2 (entités sources) puis
--      la section 3 (transaction), ré-exécuter la section 1 pour confirmer
--      que les compteurs sont retombés à 0, puis COMMIT. ROLLBACK à tout
--      moment si un chiffre surprend.
--   3. La section 2 doit toujours précéder la section 3 : la section 3 lit
--      le libellé des tables sources, qui doit déjà être corrigé.
--
-- =============================================================================


-- =============================================================================
-- Section 1 — Diagnostic (lecture seule, à exécuter avant ET après)
-- =============================================================================

SELECT 'VIREMENTS'          AS table_name, COUNT(*) AS lignes_a_corriger FROM VIREMENTS          WHERE libelle = observation OR libelle LIKE '%N/A%'
UNION ALL
SELECT 'CHEQUES',                  COUNT(*)                              FROM CHEQUES            WHERE libelle = observation OR libelle LIKE '%N/A%'
UNION ALL
SELECT 'LCNS',                     COUNT(*)                              FROM LCNS                WHERE libelle = observation OR libelle LIKE '%N/A%'
UNION ALL
SELECT 'MISES_A_DISPOSITION',      COUNT(*)                              FROM MISES_A_DISPOSITION WHERE libelle = observation OR libelle LIKE '%N/A%'
UNION ALL
SELECT 'transaction (Virement)',   COUNT(*)                              FROM `transaction`      WHERE (libelle = observation OR libelle LIKE '%N/A%') AND transaction_source = 'Virement'
UNION ALL
SELECT 'transaction (CHEQUE)',     COUNT(*)                              FROM `transaction`      WHERE (libelle = observation OR libelle LIKE '%N/A%') AND transaction_source = 'CHEQUE'
UNION ALL
SELECT 'transaction (LCN)',        COUNT(*)                              FROM `transaction`      WHERE (libelle = observation OR libelle LIKE '%N/A%') AND transaction_source IN ('LCN_DECAISSEMENT', 'LCN_ENCAISSEMENT')
UNION ALL
SELECT 'transaction (MAD)',        COUNT(*)                              FROM `transaction`      WHERE (libelle = observation OR libelle LIKE '%N/A%') AND transaction_source = 'MiseADisposition';


-- =============================================================================
-- Section 2 — Backfill des entités sources (VIREMENTS / CHEQUES / LCNS / MAD)
-- =============================================================================
-- Tous statuts confondus (ENCOURS, REGLE, ANNULE) : un enregistrement déjà
-- validé (REGLE) a vu son libelle/observation réécrits par son
-- *ValidationService avec les valeurs de la Transaction créée (voir
-- VirementValidationService.setLibelle(savedTransaction.getLibelle()), même
-- pattern pour Cheque/Lcn/MiseADisposition) — donc lui aussi peut porter un
-- libellé fautif, et doit être corrigé pour rester cohérent avec sa propre
-- Transaction (corrigée en section 3).

START TRANSACTION;

UPDATE VIREMENTS
SET libelle = CASE
        WHEN type = 'DECAISSEMENT' THEN CONCAT('Décaissement - Bénéficiaire: ', COALESCE(beneficiaire, ''))
        WHEN type = 'ENCAISSEMENT' THEN CONCAT('Encaissement - Émetteur: ',     COALESCE(emetteur, ''))
        ELSE libelle
      END
WHERE libelle = observation OR libelle LIKE '%N/A%';

UPDATE CHEQUES
SET libelle = CASE
        WHEN type = 'DECAISSEMENT' THEN CONCAT('Décaissement - Bénéficiaire: ', COALESCE(beneficiaire, ''))
        WHEN type = 'ENCAISSEMENT' THEN CONCAT('Encaissement - Émetteur: ',     COALESCE(emetteur, ''))
        ELSE libelle
      END
WHERE libelle = observation OR libelle LIKE '%N/A%';

UPDATE LCNS
SET libelle = CASE
        WHEN type = 'DECAISSEMENT' THEN CONCAT('Décaissement - Bénéficiaire: ', COALESCE(beneficiaire, ''))
        WHEN type = 'ENCAISSEMENT' THEN CONCAT('Encaissement - Émetteur: ',     COALESCE(emetteur, ''))
        ELSE libelle
      END
WHERE libelle = observation OR libelle LIKE '%N/A%';

-- MiseADisposition : pas de préfixe Décaissement/Encaissement, gabarit propre
-- au module (voir MiseADispositionMapper.toEntity).
UPDATE MISES_A_DISPOSITION
SET libelle = CASE
        WHEN type = 'DECAISSEMENT' THEN CONCAT('Bénéficiaire: ', COALESCE(beneficiaire, ''))
        WHEN type = 'ENCAISSEMENT' THEN CONCAT('Émetteur: ',     COALESCE(emetteur, ''))
        ELSE libelle
      END
WHERE libelle = observation OR libelle LIKE '%N/A%';


-- =============================================================================
-- Section 3 — Backfill de la Transaction validée, à partir de l'entité source
-- =============================================================================
-- transaction.referent porte l'id de l'entité source (Virement/Cheque/Lcn/
-- MiseADisposition) — voir *TransactionMapper.toEntity : transaction.setReferent(x.getId()).
-- Ne touche que les lignes encore fautives, donc jamais une ligne dont le
-- libellé aurait été délibérément personnalisé à la validation unitaire
-- (saisie utilisateur différente du gabarit auto-généré).

UPDATE `transaction` t
JOIN VIREMENTS v ON v.id = t.referent
SET t.libelle = v.libelle
WHERE t.transaction_source = 'Virement'
  AND (t.libelle = t.observation OR t.libelle LIKE '%N/A%');

UPDATE `transaction` t
JOIN CHEQUES c ON c.id = t.referent
SET t.libelle = c.libelle
WHERE t.transaction_source = 'CHEQUE'
  AND (t.libelle = t.observation OR t.libelle LIKE '%N/A%');

UPDATE `transaction` t
JOIN LCNS l ON l.id = t.referent
SET t.libelle = l.libelle
WHERE t.transaction_source IN ('LCN_DECAISSEMENT', 'LCN_ENCAISSEMENT')
  AND (t.libelle = t.observation OR t.libelle LIKE '%N/A%');

UPDATE `transaction` t
JOIN MISES_A_DISPOSITION m ON m.id = t.referent
SET t.libelle = m.libelle
WHERE t.transaction_source = 'MiseADisposition'
  AND (t.libelle = t.observation OR t.libelle LIKE '%N/A%');


-- Ré-exécuter la Section 1 ici. Tous les compteurs doivent être à 0 (ou très
-- proches — une ligne restante signale soit un `type` NULL ou inattendu sur
-- la source (branche ELSE : libelle laissé tel quel, à corriger à la main),
-- soit un libellé qui contenait "N/A" pour une tout autre raison qu'un champ
-- manquant — cas à inspecter avant de committer si le compteur ne tombe pas
-- à 0).

-- COMMIT;
-- ROLLBACK;
