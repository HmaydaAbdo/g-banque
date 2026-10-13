-- V7 — Transaction 63 : rattachée à l'agios n° 16 (AWB) au lieu de l'agios n° 4 (SAHAM BANK)
--
-- Objectif        : la transaction 63 (AWB, « INTERET DEBITEURS », 85 784,02, rapprochement n° 2, janvier 2026) référence
--                   par erreur l'agios 4 (SAHAM BANK, 22 856,98), déjà lié à la transaction 20 (rapprochement n° 3). L'agios
--                   16 (AWB, « INTERET DEBITEURS », 85 784,02, PAYE) n'a aucune transaction. La transaction 63 est
--                   rattachée à l'agios 16.
-- Tables          : transaction.
-- Impact données  : une seule ligne, une seule colonne (referent 4 -> 16). Montant, banque, dates, libellé, observation et
--                   rapprochement inchangés (l'agios 16 porte déjà les mêmes dates et le même libellé) : soldes des
--                   rapprochements 2 et 3 (CLOTURE_DEFINITIVE) inchangés. Après correction, chaque agios PAYE a exactement
--                   une transaction, de même montant et de même banque.
-- Audit           : une migration n'écrit pas dans transaction_aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : ne s'applique que si la transaction 63 est bien AWB à 85 784,02 rattachée à l'agios 4, si l'agios 16
--                   est AWB, PAYE, du même montant, et s'il n'a encore aucune transaction : sans effet sur une base
--                   vierge ou déjà corrigée.
-- Accord          : HMAYDA, 12/10/2026.
--
-- Contrôle (après exécution) :
--   SELECT id, referent FROM transaction WHERE transaction_source = 'AGIOS' AND id IN (20, 63);   -- 20 -> 4, 63 -> 16

UPDATE transaction t
SET referent = 16
WHERE t.id = 63
  AND t.transaction_source = 'AGIOS'
  AND t.referent = 4
  AND t.banque_id = 1
  AND t.debit = 85784.02
  AND EXISTS (SELECT 1 FROM agios a
              WHERE a.id = 16 AND a.banque_id = 1 AND a.status = 'PAYE' AND a.montant = 85784.02)
  AND NOT EXISTS (SELECT 1 FROM transaction x
                  WHERE x.transaction_source = 'AGIOS' AND x.referent = 16);
