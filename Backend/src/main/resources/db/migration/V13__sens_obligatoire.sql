-- V13 — Le sens d'une opération est obligatoire (encaissement / décaissement, déblocage / remboursement)
--
-- Objectif        : règle métier HMAYDA du 14/10/2026 : une opération a toujours un sens. Les colonnes qui le portent
--                   étaient facultatives en base pour 7 tables ; une ligne sans sens sortait de la vue des encours avec un
--                   sens vide et faisait échouer l'accueil (erreur 500, révélée en CI le 14/10/2026). La contrainte NOT NULL
--                   rend ce cas impossible, quel que soit le chemin d'écriture (application, script, test).
--                   Déjà obligatoires : impayee.type, virements_internationaux.type, provision.sens.
-- Tables          : cheques, lcns, virements, mises_a_disposition (type ENCAISSEMENT / DECAISSEMENT) ;
--                   prefinancement, factoring, droit_constate (type DEBLOCAGE / REMBOURSEMENT).
-- Impact données  : aucune ligne modifiée. Copie de production du 13/10/2026 : 0 valeur vide sur 5 127 lignes
--                   (cheques 852, lcns 497, virements 3 516, mises_a_disposition 65, prefinancement 51, factoring 68,
--                   droit_constate 78). Les contraintes CHECK de valeurs existantes sont conservées.
-- Garde-fou       : si une ligne sans sens existait, la migration échouerait sans rien modifier (transaction Flyway) ; elle
--                   serait alors à corriger d'abord, avec Finance, par une migration de données.
-- Accord          : HMAYDA, 14/10/2026.

ALTER TABLE cheques             ALTER COLUMN type SET NOT NULL;
ALTER TABLE lcns                ALTER COLUMN type SET NOT NULL;
ALTER TABLE virements           ALTER COLUMN type SET NOT NULL;
ALTER TABLE mises_a_disposition ALTER COLUMN type SET NOT NULL;
ALTER TABLE prefinancement      ALTER COLUMN type SET NOT NULL;
ALTER TABLE factoring           ALTER COLUMN type SET NOT NULL;
ALTER TABLE droit_constate      ALTER COLUMN type SET NOT NULL;
