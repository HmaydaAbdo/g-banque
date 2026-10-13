-- V11 — Agios 8, 20, 25, 36, 37, 38, 45, 46 : dates d'opération et de valeur reprises de leur transaction
--
-- Objectif        : pour huit agios PAYE, les dates de la ligne diffèrent de celles de la transaction du rapprochement qui
--                   les porte. La transaction est la source de vérité : l'agios en reprend les dates d'opération et de valeur.
--                   Agios concernés (copie du 13/10/2026) : 8 (BP), 20 (CDM), 25 (CIH), 36, 37 et 38 (SAHAM BANK), 45 et 46 (BMCE).
-- Tables          : agios.
-- Impact données  : huit lignes, deux colonnes (date_operation, date_valeur) ; montant, banque, statut, libellé, date
--                   d'émission inchangés (les dates d'émission sont déjà identiques). Aucune transaction modifiée : soldes
--                   des rapprochements inchangés.
-- Audit           : une migration n'écrit pas dans agios_aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : un agios n'est modifié que s'il est PAYE, lié à exactement une transaction AGIOS de même montant et de
--                   même banque, et si ses dates diffèrent : rejeu sûr, sans effet sur une base vierge.
-- Accord          : HMAYDA, 13/10/2026 (principe : les dates de la transaction font foi).
--
-- Contrôle (après exécution) : aucun agios PAYE dont les dates diffèrent de sa transaction :
--   SELECT count(*) FROM agios a JOIN transaction t ON t.transaction_source = 'AGIOS' AND t.referent = a.id
--   WHERE a.date_valeur IS DISTINCT FROM t.date_valeur OR a.date_operation IS DISTINCT FROM t.date_operations;   -- 0

UPDATE agios a
SET date_operation = t.date_operations,
    date_valeur    = t.date_valeur
FROM transaction t
WHERE a.id IN (8, 20, 25, 36, 37, 38, 45, 46)
  AND a.status = 'PAYE'
  AND t.transaction_source = 'AGIOS'
  AND t.referent = a.id
  AND t.debit = a.montant
  AND t.banque_id = a.banque_id
  AND (SELECT count(*) FROM transaction x WHERE x.transaction_source = 'AGIOS' AND x.referent = a.id) = 1
  AND (a.date_operation IS DISTINCT FROM t.date_operations OR a.date_valeur IS DISTINCT FROM t.date_valeur);
