-- V13 — Le sens d'une opération est obligatoire (encaissement / décaissement, déblocage / remboursement)
--
-- Objectif        : règle métier HMAYDA du 14/10/2026 : une opération a toujours un sens. Les colonnes qui le portent
--                   étaient facultatives en base pour 7 tables ; une ligne sans sens sortait de la vue des encours avec un
--                   sens vide et faisait échouer l'accueil (erreur 500, révélée en CI le 14/10/2026). La contrainte NOT NULL
--                   rend ce cas impossible, quel que soit le chemin d'écriture (application, script, test).
--                   Déjà obligatoires : impayee.type, virements_internationaux.type, provision.sens.
-- Tables          : cheques, lcns, virements, mises_a_disposition (type ENCAISSEMENT / DECAISSEMENT) ;
--                   prefinancement, factoring, droit_constate (type DEBLOCAGE / REMBOURSEMENT).
-- Impact données  : sur la copie de production du 13/10/2026, aucune ligne modifiée : 0 valeur vide sur 5 127 lignes
--                   (cheques 852, lcns 497, virements 3 516, mises_a_disposition 65, prefinancement 51, factoring 68,
--                   droit_constate 78). Les contraintes CHECK de valeurs existantes sont conservées.
-- Reprise          : avant la contrainte, une ligne sans sens reçoit celui de sa transaction de rapprochement (source de
--                   vérité) : débit -> DECAISSEMENT / REMBOURSEMENT, crédit -> ENCAISSEMENT / DEBLOCAGE. Règle vérifiée sur
--                   la copie de production : 2 177 opérations liées à une transaction, 0 exception (cheques 302, lcns 187,
--                   virements 1 589, mises_a_disposition 25, prefinancement 20, factoring 27, droit_constate 27).
-- Garde-fou       : une ligne sans sens ET sans transaction (opération prévue) n'a aucun montant signé dont déduire le sens :
--                   la migration échoue alors sans rien modifier (transaction Flyway) ; la ligne est à qualifier avec Finance.
-- Accord          : HMAYDA, 14/10/2026.

-- 1. Reprise du sens depuis la transaction (sans effet quand aucune ligne n'est vide, cas de la copie du 13/10/2026).
UPDATE cheques o SET type = CASE WHEN t.debit > 0 THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source = 'CHEQUE' AND t.referent = o.id;
UPDATE lcns o SET type = CASE WHEN t.debit > 0 THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source IN ('LCN_ENCAISSEMENT', 'LCN_DECAISSEMENT', 'LCN_ESCOMPTE') AND t.referent = o.id;
UPDATE virements o SET type = CASE WHEN t.debit > 0 THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source = 'Virement' AND t.referent = o.id;
UPDATE mises_a_disposition o SET type = CASE WHEN t.debit > 0 THEN 'DECAISSEMENT' ELSE 'ENCAISSEMENT' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source = 'MiseADisposition' AND t.referent = o.id;
UPDATE prefinancement o SET type = CASE WHEN t.debit > 0 THEN 'REMBOURSEMENT' ELSE 'DEBLOCAGE' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source = 'PREFINANCEMENT' AND t.referent = o.id;
UPDATE factoring o SET type = CASE WHEN t.debit > 0 THEN 'REMBOURSEMENT' ELSE 'DEBLOCAGE' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source = 'FACTORING' AND t.referent = o.id;
UPDATE droit_constate o SET type = CASE WHEN t.debit > 0 THEN 'REMBOURSEMENT' ELSE 'DEBLOCAGE' END
FROM transaction t WHERE o.type IS NULL AND t.transaction_source = 'DROIT_CONSTATE' AND t.referent = o.id;

-- 2. Le sens devient obligatoire.
ALTER TABLE cheques             ALTER COLUMN type SET NOT NULL;
ALTER TABLE lcns                ALTER COLUMN type SET NOT NULL;
ALTER TABLE virements           ALTER COLUMN type SET NOT NULL;
ALTER TABLE mises_a_disposition ALTER COLUMN type SET NOT NULL;
ALTER TABLE prefinancement      ALTER COLUMN type SET NOT NULL;
ALTER TABLE factoring           ALTER COLUMN type SET NOT NULL;
ALTER TABLE droit_constate      ALTER COLUMN type SET NOT NULL;
