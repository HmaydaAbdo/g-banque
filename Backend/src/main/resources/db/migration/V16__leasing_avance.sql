-- V16 — Avance optionnelle d'un contrat de leasing, réglée par un chèque, une LCN ou un virement
--
-- Objectif        : un contrat de leasing peut démarrer par une avance (échéance n°1, à la date de début du contrat),
--                   incluse dans le prix d'achat et dans la durée du contrat. Elle est réglée par un chèque, une LCN
--                   ou un virement saisi dans son propre module, puis rattaché à l'échéance n°1 depuis le contrat.
--                   L'échéance n°1 ne porte aucun statut propre une fois rattachée : elle est représentée par son
--                   moyen de paiement (statut lu en direct dans v_echeance_leasing_moyen), donc jamais comptée deux fois.
-- Tables          : contrat_leasing (+ _aud) : avance_ttc ; echeance_leasing (+ _aud) : moyen_paiement_type, moyen_paiement_id ;
--                   nouvelle vue v_echeance_leasing_moyen.
-- Impact données  : aucun — colonnes ajoutées à NULL ; les contrats et échéances existants restent inchangés.
-- Audit           : colonnes ajoutées aux tables d'audit Envers.
-- Accord          : HMAYDA, 19/10/2026.
--
-- Contrôle (après exécution) :
--   SELECT count(*) FROM contrat_leasing WHERE avance_ttc IS NOT NULL;            -- 0
--   SELECT count(*) FROM echeance_leasing WHERE moyen_paiement_id IS NOT NULL;    -- 0

ALTER TABLE contrat_leasing
    ADD COLUMN avance_ttc NUMERIC(38, 2),
    ADD CONSTRAINT ck_contrat_leasing_avance_positive CHECK (avance_ttc IS NULL OR avance_ttc > 0);

ALTER TABLE contrat_leasing_aud
    ADD COLUMN avance_ttc NUMERIC(38, 2);

ALTER TABLE echeance_leasing
    ADD COLUMN moyen_paiement_type VARCHAR(20),
    ADD COLUMN moyen_paiement_id   BIGINT,
    ADD CONSTRAINT ck_echeance_leasing_moyen_paiement_type
        CHECK (moyen_paiement_type IN ('CHEQUE', 'LCN', 'VIREMENT')),
    ADD CONSTRAINT ck_echeance_leasing_moyen_paiement_complet
        CHECK ((moyen_paiement_type IS NULL) = (moyen_paiement_id IS NULL));

ALTER TABLE echeance_leasing_aud
    ADD COLUMN moyen_paiement_type VARCHAR(20),
    ADD COLUMN moyen_paiement_id   BIGINT;

-- Un même chèque / LCN / virement ne peut régler qu'une seule avance.
CREATE UNIQUE INDEX uk_echeance_leasing_moyen_paiement
    ON echeance_leasing (moyen_paiement_type, moyen_paiement_id)
    WHERE moyen_paiement_id IS NOT NULL;

-- Statut du moyen de paiement rattaché à une échéance (une ligne par échéance rattachée).
CREATE VIEW v_echeance_leasing_moyen AS
SELECT el.id AS echeance_id,
       m.statut AS statut_moyen
FROM echeance_leasing el
JOIN (SELECT 'CHEQUE'   AS type, id, statut FROM cheques
      UNION ALL
      SELECT 'LCN'      AS type, id, statut FROM lcns
      UNION ALL
      SELECT 'VIREMENT' AS type, id, statut FROM virements) m
  ON m.type = el.moyen_paiement_type AND m.id = el.moyen_paiement_id;
