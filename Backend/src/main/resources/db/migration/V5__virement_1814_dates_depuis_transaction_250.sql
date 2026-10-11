-- V5 — Virement n° 008981 (id 1814, BP, AGNI BTP, 30 000 MAD) : dates reprises de la transaction 250
--
-- Objectif        : le virement 1814 est « Réglé » mais ses dates d'opération et de valeur sont vides, alors que la
--                   transaction 250 (rapprochement n° 5, BP janvier 2026), qui pointe sur lui, les porte (19/01/2026).
--                   Les deux dates sont reprises de la transaction. Le statut (déjà REGLE) n'est pas modifié.
-- Tables          : virements.
-- Impact données  : une seule ligne, deux colonnes (date_operation, date_valeur) ; aucune autre donnée touchée.
-- Audit           : une migration n'écrit pas dans virements_aud (décision HMAYDA du 10/10/2026, comme V3).
-- Garde-fous      : ne s'applique que si le virement est REGLE avec ses deux dates vides ET si la transaction 250
--                   (source Virement, référent 1814) existe : sans effet sur une base vierge ou déjà corrigée.
-- Diagnostic      : copie de la production du 10/10/2026 — virements : 1 586 liés à une transaction, un seul sans dates
--                   (le 1814) ; chèques, LCN, virements internationaux, mises à disposition, prélèvements : aucun écart.
-- Accord          : HMAYDA, 11/10/2026 (point 11 de la liste de session).
--
-- Contrôle (après exécution) :
--   SELECT statut, date_operation, date_valeur FROM virements WHERE id = 1814;   -- REGLE, 2026-01-19, 2026-01-19

UPDATE virements v
SET date_operation = t.date_operations,
    date_valeur    = t.date_valeur
FROM transaction t
WHERE v.id = 1814
  AND v.statut = 'REGLE'
  AND v.date_operation IS NULL
  AND v.date_valeur IS NULL
  AND t.id = 250
  AND t.transaction_source = 'Virement'
  AND t.referent = v.id;
