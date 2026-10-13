-- V10 — Préfinancements 36 et 38 : dates d'opération et de valeur reprises de leur transaction
--
-- Objectif        : pour deux remboursements, la ligne de préfinancement et sa transaction se contredisent (dates
--                   d'opération et de valeur inversées ou différentes). La transaction du rapprochement est la source de
--                   vérité : la ligne en reprend les dates.
--                   - préfinancement 36 (BMCI, 2 048 512,49) : opération 18/03/2026, valeur 16/03/2026 ;
--                   - préfinancement 38 (BP, 327 200,00)     : opération 19/03/2026, valeur 24/03/2026.
--                   (Le 34 est traité par V9.)
-- Tables          : prefinancement.
-- Impact données  : deux lignes, deux colonnes chacune (date_operation, date_valeur) ; montant, statut, libellé inchangés.
--                   La date de valeur du préfinancement est celle que lit le calcul des agios de préfinancement : l'encours
--                   de BMCI (nul depuis le 18/03) et de BP (aucun calcul T1/2026) n'est pas modifié sur une période calculée.
-- Audit           : une migration n'écrit pas dans prefinancement_aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : une ligne n'est modifiée que si sa transaction existe et si ses dates diffèrent : rejeu sûr.
-- Accord          : HMAYDA, 13/10/2026 (principe : les dates de la transaction font foi).
--
-- Contrôle (après exécution) :
--   SELECT id, date_operation, date_valeur FROM prefinancement WHERE id IN (36, 38);
--   -- 36 : 2026-03-18 / 2026-03-16 ; 38 : 2026-03-19 / 2026-03-24

UPDATE prefinancement p
SET date_operation = t.date_operations,
    date_valeur    = t.date_valeur
FROM transaction t
WHERE p.id IN (36, 38)
  AND p.status = 'PAYE'
  AND t.transaction_source = 'PREFINANCEMENT'
  AND t.referent = p.id
  AND t.debit = p.montant
  AND (p.date_operation IS DISTINCT FROM t.date_operations OR p.date_valeur IS DISTINCT FROM t.date_valeur);
