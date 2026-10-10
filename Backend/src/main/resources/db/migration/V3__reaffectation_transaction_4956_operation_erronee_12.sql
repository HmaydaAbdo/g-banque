-- V3 — Réaffectation de la transaction 4956 : du droit constaté n° 76 vers l'opération erronée n° 12
--
-- Objectif        : le DC n° 76 (remboursement du marché ASSAZAG, 3 026 943,58) est un doublon du DC n° 74.
--                   La transaction 4956 (rapprochement CDM 08/2026, n° 57) cesse de provenir de ce DC et
--                   devient la transaction de l'opération erronée n° 12 ; le DC n° 76 est supprimé.
-- Tables          : transaction, operation_errone, droit_constate.
-- Impact données  : - transaction 4956 : transaction_source DROIT_CONSTATE -> OPERATION_ERRONE, referent 76 -> 12,
--                     libellé et observation repris de l'opération (« ECRITURE ERRONEE ») ; montant, TVA, dates
--                     et rapprochement inchangés, donc soldes du rapprochement 57 (CLOTURE_DEFINITIVE) inchangés ;
--                   - opération erronée 12 : PREVU -> PAYE, dates d'opération et de valeur reprises de la
--                     transaction, libellé et observation conservés ;
--                   - DC 76 supprimé (aucune clé étrangère ne le référence ; historique conservé dans
--                     droit_constate_aud). Les agios DC (T2/T3 2026 OUVERT) sont recalculés par le batch de 02h00.
-- Audit           : une migration n'écrit pas dans transaction_aud / operation_errone_aud (décision HMAYDA 10/10/2026).
-- Garde-fous      : chaque instruction ne s'applique que si l'état attendu est présent (no-op sur une base où
--                   l'opération erronée 12 n'existe pas, par exemple une base vierge ou une copie plus ancienne).
-- Accord          : HMAYDA, 10/10/2026.
--
-- Contrôles (après exécution) :
--   SELECT transaction_source, referent, libelle, observation FROM transaction WHERE id = 4956;  -- OPERATION_ERRONE, 12, ECRITURE ERRONEE
--   SELECT status, date_operation, date_valeur FROM operation_errone WHERE id = 12;               -- PAYE, 2026-08-28, 2026-08-31
--   SELECT count(*) FROM droit_constate WHERE id = 76;                                            -- 0

-- 1. La transaction 4956 change de source et reprend le libellé et l'observation de l'opération 12.
UPDATE transaction t
SET transaction_source = 'OPERATION_ERRONE',
    referent           = o.id,
    libelle            = o.libelle,
    observation        = o.observation
FROM operation_errone o
WHERE t.id = 4956
  AND t.transaction_source = 'DROIT_CONSTATE'
  AND t.referent = 76
  AND o.id = 12
  AND o.status = 'PREVU'
  AND o.banque_id = 4
  AND o.montant = 3026943.58;

-- 2. L'opération 12 passe à PAYE avec les dates de la transaction (libellé et observation conservés).
UPDATE operation_errone o
SET status         = 'PAYE',
    date_operation = t.date_operations,
    date_valeur    = t.date_valeur
FROM transaction t
WHERE o.id = 12
  AND o.status = 'PREVU'
  AND t.id = 4956
  AND t.transaction_source = 'OPERATION_ERRONE'
  AND t.referent = 12;

-- 3. Suppression du DC 76, seulement s'il n'est plus référencé par aucune transaction.
DELETE FROM droit_constate
WHERE id = 76
  AND NOT EXISTS (SELECT 1 FROM transaction
                  WHERE transaction_source = 'DROIT_CONSTATE' AND referent = 76);
