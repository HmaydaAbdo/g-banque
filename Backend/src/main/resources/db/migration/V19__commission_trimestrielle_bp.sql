-- V19 — Commissions de caution trimestrielles : nouveau type TRIMESTRIELLE et configuration de la banque BP
--
-- Objectif        : la banque BP est facturée par trimestre civil (premier cas de facturation non mensuelle). Les commissions
--                   générées pour elle portent le type TRIMESTRIELLE, à côté de PRORATA_ENTREE, MENSUELLE et CLOTURE. BP a
--                   aussi besoin de sa ligne de configuration (diviseur 36 000, aucun minimum) pour que sa stratégie calcule.
-- Tables          : commission, commission_aud (contrainte sur type_commission) ; caution_commission_config (ligne BP).
-- Impact données  : - aucune commission existante modifiée ;
--                   - ligne ajoutée à caution_commission_config pour la banque BP : diviseur 36 000, minimum 0 (BP n'applique
--                     aucun minimum), une seule fois (rien si la ligne existe déjà ou si la banque BP est absente).
-- Audit           : une migration n'écrit pas dans les tables _aud (décision HMAYDA 10/10/2026, comme V3, V6, V15 et V18).
-- Accord          : HMAYDA, 21/10/2026 (facturation trimestrielle civile à partir du 31/12/2026, aucun minimum pour BP).
--
-- Contrôles (après exécution) :
--   SELECT diviseur, montant_minimum FROM caution_commission_config WHERE banque_id = (SELECT id FROM banque WHERE nom = 'BP');
--       -- 36000.00, 0.00
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'commission_type_commission_check';
--       -- contient 'TRIMESTRIELLE'

ALTER TABLE commission DROP CONSTRAINT commission_type_commission_check;
ALTER TABLE commission ADD CONSTRAINT commission_type_commission_check
    CHECK (type_commission IN ('PRORATA_ENTREE', 'MENSUELLE', 'TRIMESTRIELLE', 'CLOTURE'));

ALTER TABLE commission_aud DROP CONSTRAINT commission_aud_type_commission_check;
ALTER TABLE commission_aud ADD CONSTRAINT commission_aud_type_commission_check
    CHECK (type_commission IN ('PRORATA_ENTREE', 'MENSUELLE', 'TRIMESTRIELLE', 'CLOTURE'));

INSERT INTO caution_commission_config (banque_id, diviseur, montant_minimum)
SELECT b.id, 36000.00, 0.00
FROM banque b
WHERE b.nom = 'BP'
  AND NOT EXISTS (SELECT 1 FROM caution_commission_config c WHERE c.banque_id = b.id);
