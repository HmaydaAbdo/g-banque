-- V12 — Périodes d'agios calculées sans aucune opération : suppression
--
-- Objectif        : jusqu'ici une banque était calculée pour un type (droits constatés, préfinancement, factoring) dès
--                   qu'un de ses marchés avait une ligne fonctionnelle active, même sans aucune opération de ce type : le
--                   planificateur créait des périodes vides (0 jour, 0 MAD). Les calculateurs exigent désormais au moins une
--                   opération payée ; cette migration supprime les périodes vides déjà créées.
--                   Périodes concernées (copie du 13/10/2026) : droits constatés T3/2026 de BMCI et SAHAM BANK ; factoring de
--                   BP (février, mars, septembre 2026). Le préfinancement T3/2026 de BMCI est conservé (la banque a des
--                   opérations payées : son encours est réellement nul depuis le 18/03/2026).
-- Tables          : agios_trimestriel, agios_journalier.
-- Impact données  : une période n'est supprimée que si elle est à 0 MAD et 0 jour calculé, sans agios définitif, sans ligne
--                   journalière non nulle, ET si la banque n'a aucune opération PAYE du type sur ses marchés. Les lignes
--                   journalières nulles de ces périodes sont supprimées avec elles. Aucun montant n'est perdu.
-- Audit           : une migration n'écrit pas dans agios_trimestriel_aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : règle générale (aucun numéro en dur) ; sans effet sur une base vierge ou déjà nettoyée.
-- Accord          : HMAYDA, 13/10/2026 (option 2 : éligibilité fondée sur les opérations).
--
-- Contrôle (après exécution) : 0 période vide sans opération :
--   SELECT count(*) FROM agios_trimestriel WHERE total_agios_ttc = 0 AND nb_jours_calcules = 0
--     AND type_agios IN ('DROIT_CONSTATE', 'FACTORING');   -- 0 (le préfinancement BMCI T3 reste)

WITH vides AS (
    SELECT t.id
    FROM agios_trimestriel t
    WHERE t.type_agios IN ('DROIT_CONSTATE', 'PREFINANCEMENT', 'FACTORING')
      AND t.total_agios_ttc = 0
      AND t.nb_jours_calcules = 0
      AND t.agios_definitif_id IS NULL
      AND NOT EXISTS (SELECT 1 FROM agios_journalier j WHERE j.trimestre_id = t.id AND j.agios_ttc <> 0)
      AND NOT (
            (t.type_agios = 'DROIT_CONSTATE' AND EXISTS (
                SELECT 1 FROM droit_constate o JOIN marches m ON m.id = o.marche_id
                WHERE m.banque_id = t.banque_id AND o.status = 'PAYE'))
         OR (t.type_agios = 'PREFINANCEMENT' AND EXISTS (
                SELECT 1 FROM prefinancement o JOIN marches m ON m.id = o.marche_id
                WHERE m.banque_id = t.banque_id AND o.status = 'PAYE'))
         OR (t.type_agios = 'FACTORING' AND EXISTS (
                SELECT 1 FROM factoring o JOIN marches m ON m.id = o.marche_id
                WHERE m.banque_id = t.banque_id AND o.status = 'PAYE'))
      )
),
journalier_supprime AS (
    DELETE FROM agios_journalier j
    WHERE j.trimestre_id IN (SELECT id FROM vides)
    RETURNING j.id
)
DELETE FROM agios_trimestriel
WHERE id IN (SELECT id FROM vides);
