-- V15 — Réaffectation de la transaction 5303 : du factoring n° 72 vers un nouveau décompte du marché GUERCIF-NADOR
--
-- Objectif        : le factoring n° 72 (déblocage de 6 007 550,15 du marché GUERCIF-NADOR, banque 3) a été saisi par erreur :
--                   c'est l'encaissement d'un décompte. Il est supprimé et remplacé par un décompte de mêmes données, et la
--                   transaction 5303 (rapprochement n° 62, septembre 2026) devient la transaction de ce décompte.
-- Tables          : decompte, transaction, factoring.
-- Impact données  : - décompte créé : PAYE, banque d'encaissement 3, marché 2, montant 6 007 550,15, dates d'émission,
--                     d'opération et de valeur du factoring (07/09, 07/09 et 08/09/2026), libellé « DECOMPTE GUERCIF-NADOR »,
--                     observation « Marche: 04/2023/DRETLO » (forme des autres décomptes du marché) ;
--                   - transaction 5303 : transaction_source FACTORING -> DECOMPTES, referent 72 -> nouveau décompte, libellé et
--                     observation repris du décompte, TVA collectée 0 -> 1 001 258,36 (20 % : montant TTC / 6, comme le
--                     décompte n° 53 du même rapprochement) ; montant, dates et rapprochement inchangés, donc débit, crédit et
--                     soldes du rapprochement 62 inchangés (seul son total de TVA collectée augmente de 1 001 258,36) ;
--                   - factoring 72 supprimé (aucune transaction ne le référence plus ; historique conservé dans
--                     factoring_aud) : il sort de l'encours des agios « factoring » de la banque 3, dont les périodes
--                     ouvertes sont recalculées par le calcul de nuit.
-- Audit           : une migration n'écrit pas dans transaction_aud / decompte_aud (décision HMAYDA 10/10/2026, comme V3 et V6).
-- Garde-fous      : chaque instruction ne s'applique que si l'état attendu est présent (no-op sur une base où le cas
--                   n'existe pas, et en cas de rejeu).
-- Accord          : HMAYDA, 17/10/2026 (libellé « DECOMPTE GUERCIF-NADOR », TVA 20 %), cas découvert en vérifiant les agios.
--
-- Contrôles (après exécution) :
--   SELECT transaction_source, libelle, observation, tva_collectee FROM transaction WHERE id = 5303;
--       -- DECOMPTES, DECOMPTE GUERCIF-NADOR, Marche: 04/2023/DRETLO, 1001258.36
--   SELECT status, montant, date_operation, date_valeur FROM decompte WHERE id = (SELECT referent FROM transaction WHERE id = 5303);
--       -- PAYE, 6007550.15, 2026-09-07, 2026-09-08
--   SELECT count(*) FROM factoring WHERE id = 72;                                                -- 0

-- 1. Le décompte reprend les données du factoring 72 (une seule fois).
INSERT INTO decompte (banque_encaissement_id, marche_id, montant, date_emission, date_operation, date_valeur,
                      libelle, observation, status)
SELECT t.banque_id, f.marche_id, f.montant, f.date_emission, f.date_operation, f.date_valeur,
       'DECOMPTE GUERCIF-NADOR', 'Marche: 04/2023/DRETLO', 'PAYE'
FROM factoring f
JOIN transaction t ON t.transaction_source = 'FACTORING' AND t.referent = f.id
WHERE f.id = 72
  AND f.type = 'DEBLOCAGE'
  AND f.status = 'PAYE'
  AND f.marche_id = 2
  AND f.montant = 6007550.15
  AND t.id = 5303
  AND t.credit = f.montant
  AND NOT EXISTS (SELECT 1 FROM decompte d
                  WHERE d.marche_id = 2
                    AND d.libelle = 'DECOMPTE GUERCIF-NADOR'
                    AND d.montant = 6007550.15
                    AND d.date_emission = f.date_emission);

-- 2. La transaction 5303 change de source et reprend le libellé et l'observation du décompte.
UPDATE transaction t
SET transaction_source = 'DECOMPTES',
    referent           = d.id,
    libelle            = d.libelle,
    observation        = d.observation,
    tva_collectee      = 1001258.36
FROM decompte d
WHERE t.id = 5303
  AND t.transaction_source = 'FACTORING'
  AND t.referent = 72
  AND t.credit = 6007550.15
  AND d.marche_id = 2
  AND d.libelle = 'DECOMPTE GUERCIF-NADOR'
  AND d.montant = t.credit
  AND d.banque_encaissement_id = t.banque_id;

-- 3. Suppression du factoring 72, seulement s'il n'est plus référencé par aucune transaction.
DELETE FROM factoring
WHERE id = 72
  AND marche_id = 2
  AND montant = 6007550.15
  AND NOT EXISTS (SELECT 1 FROM transaction
                  WHERE transaction_source = 'FACTORING' AND referent = 72);
