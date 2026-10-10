-- =============================================================================
-- V2 — Colonnes obligatoires alignées sur les entités (10/10/2026)
-- =============================================================================
--
-- La V1 avait laissé facultatives 60 colonnes déclarées obligatoires dans les entités, parce que le schéma MySQL
-- de production les autorisait à NULL. Analyse des données de production du 10/10/2026 : une seule valeur NULL
-- (decompte.libelle, décompte n° 3), sans transaction liée. Décision HMAYDA : suivre le format des autres décomptes
-- du même marché (3/2024/ASB, SIDI BERNOUSSI) — le n° 4 du 12/11/2025 est « DP N°2 SIDI BERNOUSSI », le n° 3 du
-- 06/05/2025 devient donc « DP N°1 SIDI BERNOUSSI ». Les DTO et l'interface exigent déjà ces champs.
--
-- Contrôle avant (doit renvoyer 0 après l'UPDATE) :
--   SELECT count(*) FROM decompte WHERE libelle IS NULL;
-- Sur une base vierge (tests), l'UPDATE ne touche aucune ligne.
-- L'historique d'audit (decompte_aud) n'est pas réécrit : il reflète l'état passé.
-- =============================================================================

UPDATE decompte SET libelle = 'DP N°1 SIDI BERNOUSSI' WHERE id = 3 AND libelle IS NULL;

ALTER TABLE agios ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE agios ALTER COLUMN observation SET NOT NULL;
ALTER TABLE cautions ALTER COLUMN date_de_prise SET NOT NULL;
ALTER TABLE cautions ALTER COLUMN numero_caution SET NOT NULL;
ALTER TABLE cheques ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE cheques ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE cheques ALTER COLUMN observation SET NOT NULL;
ALTER TABLE commission ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE commission ALTER COLUMN observation SET NOT NULL;
ALTER TABLE contrat_leasing ALTER COLUMN date_debut SET NOT NULL;
ALTER TABLE credit_moyen_terme ALTER COLUMN date_debut SET NOT NULL;
ALTER TABLE decompte ALTER COLUMN banque_encaissement_id SET NOT NULL;
ALTER TABLE decompte ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE decompte ALTER COLUMN observation SET NOT NULL;
ALTER TABLE droit_constate ALTER COLUMN banque_id SET NOT NULL;
ALTER TABLE droit_constate ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE droit_constate ALTER COLUMN observation SET NOT NULL;
ALTER TABLE echeance_cmt ALTER COLUMN date_echeance SET NOT NULL;
ALTER TABLE echeance_cmt ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE echeance_cmt ALTER COLUMN observation SET NOT NULL;
ALTER TABLE echeance_leasing ALTER COLUMN date_echeance SET NOT NULL;
ALTER TABLE echeance_leasing ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE echeance_leasing ALTER COLUMN observation SET NOT NULL;
ALTER TABLE factoring ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE factoring ALTER COLUMN observation SET NOT NULL;
ALTER TABLE frais_bancaire ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE frais_bancaire ALTER COLUMN observation SET NOT NULL;
ALTER TABLE impayee ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE impayee ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE impayee ALTER COLUMN observation SET NOT NULL;
ALTER TABLE impayee ALTER COLUMN type SET NOT NULL;
ALTER TABLE impot_et_taxe ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE impot_et_taxe ALTER COLUMN observation SET NOT NULL;
ALTER TABLE lcns ALTER COLUMN date_echeance SET NOT NULL;
ALTER TABLE lcns ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE lcns ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE lcns ALTER COLUMN observation SET NOT NULL;
ALTER TABLE ligne_fonctionnelle ALTER COLUMN date_depart SET NOT NULL;
ALTER TABLE ligne_fonctionnelle ALTER COLUMN date_fin SET NOT NULL;
ALTER TABLE ligne_fonctionnelle_specifique ALTER COLUMN date_depart SET NOT NULL;
ALTER TABLE ligne_fonctionnelle_specifique ALTER COLUMN date_fin SET NOT NULL;
ALTER TABLE mises_a_disposition ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE mises_a_disposition ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE mises_a_disposition ALTER COLUMN observation SET NOT NULL;
ALTER TABLE operation_errone ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE operation_errone ALTER COLUMN observation SET NOT NULL;
ALTER TABLE prefinancement ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE prefinancement ALTER COLUMN observation SET NOT NULL;
ALTER TABLE prelevement ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE prelevement ALTER COLUMN observation SET NOT NULL;
ALTER TABLE restitution ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE restitution ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE restitution ALTER COLUMN observation SET NOT NULL;
ALTER TABLE users ALTER COLUMN password SET NOT NULL;
ALTER TABLE virements ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE virements ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE virements ALTER COLUMN observation SET NOT NULL;
ALTER TABLE virements_internationaux ALTER COLUMN date_emission SET NOT NULL;
ALTER TABLE virements_internationaux ALTER COLUMN libelle SET NOT NULL;
ALTER TABLE virements_internationaux ALTER COLUMN observation SET NOT NULL;
