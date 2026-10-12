-- V6 — Réaffectation de la transaction 5727 : du droit constaté n° 81 vers le décompte n° 61
--
-- Objectif        : le DC n° 81 (déblocage du marché RABAT-OUJDA, 1 307 057,94) a été saisi par erreur : il s'agit de
--                   l'encaissement du décompte n° 61 (même marché, même banque, même montant, même date d'émission).
--                   La transaction 5727 (rapprochement n° 65, septembre 2026, CLOTURE_DEFINITIVE) cesse de provenir du
--                   DC et devient la transaction du décompte n° 61 ; le DC n° 81 est supprimé.
-- Tables          : transaction, decompte, droit_constate.
-- Impact données  : - transaction 5727 : transaction_source DROIT_CONSTATE -> DECOMPTES, referent 81 -> 61,
--                     libellé et observation repris du décompte (« ENCAISSEMENT DIRECT / DECOMPTE MARCHÉ RABAT-OUJDA »,
--                     « Marche: M0108/25/S »), TVA collectée 0 -> 217 842,99 (20 % : montant TTC / 6, comme les autres
--                     décomptes du marché) ; montant, dates et rapprochement inchangés, donc débit, crédit et soldes du
--                     rapprochement 65 inchangés (seul son total de TVA collectée augmente de 217 842,99) ;
--                   - décompte 61 : PREVU -> PAYE, dates d'opération et de valeur reprises de la transaction (29/09 et
--                     30/09/2026), libellé et observation conservés ; il sort des encours prévus (vue des encours) ;
--                   - DC 81 supprimé (aucune clé étrangère ne le référence ; historique conservé dans
--                     droit_constate_aud) : il n'entre donc plus dans l'encours des agios « droits constatés » (aucun
--                     agios journalier n'a encore été calculé avec lui : trimestre 3 de la banque 1 calculé le 07/09/2026,
--                     tous les trimestres DC sont OUVERT).
-- Audit           : une migration n'écrit pas dans transaction_aud / decompte_aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : chaque instruction ne s'applique que si l'état attendu est présent (no-op sur une base où le cas
--                   n'existe pas, par exemple une base vierge ou une copie plus ancienne, et en cas de rejeu).
-- Accord          : HMAYDA, 12/10/2026 (TVA 20 %, libellé et observation du décompte).
--
-- Contrôles (après exécution) :
--   SELECT transaction_source, referent, libelle, observation, tva_collectee FROM transaction WHERE id = 5727;
--       -- DECOMPTES, 61, ENCAISSEMENT DIRECT / DECOMPTE MARCHÉ RABAT-OUJDA, Marche: M0108/25/S, 217842.99
--   SELECT status, date_operation, date_valeur FROM decompte WHERE id = 61;                      -- PAYE, 2026-09-29, 2026-09-30
--   SELECT count(*) FROM droit_constate WHERE id = 81;                                           -- 0

-- 1. La transaction 5727 change de source et reprend le libellé et l'observation du décompte 61.
UPDATE transaction t
SET transaction_source = 'DECOMPTES',
    referent           = d.id,
    libelle            = d.libelle,
    observation        = d.observation,
    tva_collectee      = 217842.99
FROM decompte d
WHERE t.id = 5727
  AND t.transaction_source = 'DROIT_CONSTATE'
  AND t.referent = 81
  AND t.credit = 1307057.94
  AND d.id = 61
  AND d.status = 'PREVU'
  AND d.banque_encaissement_id = t.banque_id
  AND d.marche_id = 13
  AND d.montant = t.credit;

-- 2. Le décompte 61 passe à PAYE avec les dates de la transaction (libellé et observation conservés).
UPDATE decompte d
SET status         = 'PAYE',
    date_operation = t.date_operations,
    date_valeur    = t.date_valeur
FROM transaction t
WHERE d.id = 61
  AND d.status = 'PREVU'
  AND t.id = 5727
  AND t.transaction_source = 'DECOMPTES'
  AND t.referent = d.id;

-- 3. Suppression du DC 81, seulement s'il n'est plus référencé par aucune transaction.
DELETE FROM droit_constate
WHERE id = 81
  AND marche_id = 13
  AND montant = 1307057.94
  AND NOT EXISTS (SELECT 1 FROM transaction
                  WHERE transaction_source = 'DROIT_CONSTATE' AND referent = 81);
