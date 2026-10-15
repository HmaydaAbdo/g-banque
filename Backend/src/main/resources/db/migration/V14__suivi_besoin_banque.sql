-- V14 — Suivi quotidien du besoin de financement par banque (écran Trésorerie prévisionnelle)
--
-- Objectif        : savoir depuis combien de jours une banque est en besoin de financement. Chaque nuit, l'application
--                   enregistre, pour chaque banque et chaque horizon (fin de mois, fin de trimestre), le besoin du jour
--                   (0 si la banque est couverte). L'ancienneté d'un besoin se déduit de cet historique.
-- Tables          : suivi_besoin_banque (nouvelle).
-- Impact données  : aucun — table vide à la création ; l'historique se constitue à partir de la première nuit.
-- Audit           : table technique alimentée par un traitement automatique, non auditée (comme agios_journalier).
-- Accord          : HMAYDA, 15/10/2026.
--
-- Contrôle (après exécution) :
--   SELECT count(*) FROM suivi_besoin_banque;   -- 0 juste après la migration

CREATE TABLE suivi_besoin_banque (
    id        BIGSERIAL PRIMARY KEY,
    jour      DATE           NOT NULL,
    banque_id BIGINT         NOT NULL REFERENCES banque (id),
    periode   VARCHAR(30)    NOT NULL,
    besoin    NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uk_suivi_besoin_jour_banque_periode UNIQUE (jour, banque_id, periode)
);

CREATE INDEX idx_suivi_besoin_periode_jour ON suivi_besoin_banque (periode, jour);
