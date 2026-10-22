-- V21 — Réaffectation de la transaction 4586 : du frais bancaire n° 268 vers un nouvel agios « AGIOS SUR MOBILISATION FACTORING »
--
-- Objectif        : le frais bancaire n° 268 (banque 3, « FRAIS D ENGAGEMENT / MOBILISATION FACTORING », 309 724,83, PAYE)
--                   a été saisi par erreur : ce sont des agios sur mobilisation de factoring. Il est supprimé et remplacé par
--                   un agios de mêmes montant et dates, et la transaction 4586 (rapprochement n° 59, août 2026) devient la
--                   transaction de cet agios.
-- Tables          : agios, transaction, frais_bancaire.
-- Impact données  : - agios créé : PAYE, banque 3, montant, dates d'émission, d'opération et de valeur du frais bancaire
--                     (03/08, 04/08 et 03/08/2026), libellé et observation « AGIOS SUR MOBILISATION FACTORING » ;
--                   - transaction 4586 : transaction_source FRAIS_BANCAIRE -> AGIOS, referent 268 -> nouvel agios, libellé et
--                     observation repris de l'agios, TVA déductible 28 156,80 -> 0 (les agios ne portent pas de TVA) ; montant,
--                     dates et rapprochement inchangés, donc débit, crédit et soldes du rapprochement 59 (CLOTURE_DEFINITIVE)
--                     inchangés (seul son total de TVA déductible diminue de 28 156,80) ;
--                   - frais bancaire 268 supprimé (aucune transaction ne le référence plus ; historique conservé dans
--                     frais_bancaire_aud) : il sort des frais bancaires de la banque 3.
-- Audit           : une migration n'écrit pas dans transaction_aud / agios_aud (décision HMAYDA 10/10/2026, comme V3, V6 et V15).
-- Garde-fous      : chaque instruction ne s'applique que si l'état attendu est présent (no-op sur une base où le cas
--                   n'existe pas, et en cas de rejeu).
-- Accord          : HMAYDA, 22/10/2026 (libellé et observation « AGIOS SUR MOBILISATION FACTORING », TVA 0, suppression du frais).
--
-- Contrôles (après exécution) :
--   SELECT transaction_source, libelle, observation, tva_deductible, debit FROM transaction WHERE id = 4586;
--       -- AGIOS, AGIOS SUR MOBILISATION FACTORING, AGIOS SUR MOBILISATION FACTORING, 0.00, 309724.83
--   SELECT status, montant, date_operation, date_valeur FROM agios WHERE id = (SELECT referent FROM transaction WHERE id = 4586);
--       -- PAYE, 309724.8300, 2026-08-04, 2026-08-03
--   SELECT count(*) FROM frais_bancaire WHERE id = 268;                                          -- 0

-- 1. L'agios reprend les données du frais bancaire 268 (une seule fois).
INSERT INTO agios (banque_id, montant, date_emission, date_operation, date_valeur, libelle, observation, status)
SELECT t.banque_id, f.montant, f.date_emission, f.date_operation, f.date_valeur,
       'AGIOS SUR MOBILISATION FACTORING', 'AGIOS SUR MOBILISATION FACTORING', 'PAYE'
FROM frais_bancaire f
JOIN transaction t ON t.transaction_source = 'FRAIS_BANCAIRE' AND t.referent = f.id
WHERE f.id = 268
  AND f.status = 'PAYE'
  AND f.banque_id = 3
  AND f.montant = 309724.83
  AND t.id = 4586
  AND t.debit = f.montant
  AND NOT EXISTS (SELECT 1 FROM agios a
                  WHERE a.banque_id = 3
                    AND a.libelle = 'AGIOS SUR MOBILISATION FACTORING'
                    AND a.montant = 309724.83
                    AND a.date_emission = f.date_emission);

-- 2. La transaction 4586 change de source et reprend le libellé et l'observation de l'agios.
UPDATE transaction t
SET transaction_source = 'AGIOS',
    referent           = a.id,
    libelle            = a.libelle,
    observation        = a.observation,
    tva_deductible     = 0
FROM agios a
WHERE t.id = 4586
  AND t.transaction_source = 'FRAIS_BANCAIRE'
  AND t.referent = 268
  AND t.debit = 309724.83
  AND a.banque_id = t.banque_id
  AND a.libelle = 'AGIOS SUR MOBILISATION FACTORING'
  AND a.montant = t.debit
  AND a.date_emission = t.date_emission;

-- 3. Suppression du frais bancaire 268, seulement s'il n'est plus référencé par aucune transaction.
DELETE FROM frais_bancaire
WHERE id = 268
  AND banque_id = 3
  AND montant = 309724.83
  AND NOT EXISTS (SELECT 1 FROM transaction
                  WHERE transaction_source = 'FRAIS_BANCAIRE' AND referent = 268);
