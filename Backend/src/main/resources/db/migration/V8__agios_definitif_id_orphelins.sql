-- V8 — Périodes d'agios clôturées : suppression des liens vers des agios définitifs qui n'existent plus
--
-- Objectif        : la clôture d'une période crée une ligne d'agios au montant calculé et en garde le numéro dans
--                   agios_trimestriel.agios_definitif_id. Finance a ensuite remplacé ces lignes par les montants réels
--                   débités par la banque : les lignes ont été supprimées, les numéros sont restés. Périodes concernées
--                   (copie du 12/10/2026) : facilité de caisse T1 et T2 2026 des 6 banques (agios 39 à 44 et 73 à 78),
--                   préfinancement T2 2026 de BMCE et CDM (agios 79 et 80), puis les périodes T2 2026 clôturées le
--                   12/10/2026 (droits constatés, préfinancement) dont l'agios créé à la clôture a aussi été supprimé.
--                   La règle est générale (aucun numéro en dur) : toute période dans ce cas est traitée.
-- Tables          : agios_trimestriel.
-- Impact données  : agios_definitif_id passe à NULL, uniquement là où l'agios référencé n'existe pas ; aucun autre champ.
--                   Aucune clé étrangère ni écran ne lisait ce champ.
-- Audit           : une migration n'écrit pas dans agios_trimestriel_aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : une période dont l'agios définitif existe encore garde son lien ; sans effet sur une base vierge ou
--                   déjà nettoyée.
-- Accord          : HMAYDA, 12/10/2026.
--
-- Contrôle (après exécution) :
--   SELECT count(*) FROM agios_trimestriel t WHERE t.agios_definitif_id IS NOT NULL
--     AND NOT EXISTS (SELECT 1 FROM agios a WHERE a.id = t.agios_definitif_id);   -- 0

UPDATE agios_trimestriel t
SET agios_definitif_id = NULL
WHERE t.agios_definitif_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM agios a WHERE a.id = t.agios_definitif_id);
