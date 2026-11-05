-- Objectif : aligner les motifs autorisés par Provision sur TransactionSource, y compris Importation.
-- Tables concernées : provision, provision_aud.
-- Impact sur les données existantes : aucun ; les valeurs actuellement autorisées restent acceptées.
-- Contrôle après migration : les deux contraintes doivent contenir Importation et tous les motifs de TransactionSource.
-- SELECT conrelid::regclass, pg_get_constraintdef(oid) FROM pg_constraint
-- WHERE conname IN ('provision_motif_check', 'provision_aud_motif_check');

ALTER TABLE provision DROP CONSTRAINT provision_motif_check;
ALTER TABLE provision
    ADD CONSTRAINT provision_motif_check CHECK (motif IN (
        'LEASING', 'CMT', 'CHEQUE', 'LCN', 'LCN_DECAISSEMENT', 'LCN_ENCAISSEMENT', 'LCN_ESCOMPTE',
        'Virement', 'MiseADisposition', 'PREFINANCEMENT', 'DROIT_CONSTATE', 'DECOMPTES', 'COMMISSION',
        'AGIOS', 'FRAIS_BANCAIRE', 'FACTORING', 'PRELEVEMENT', 'CAUTION', 'IMPOT_TAXE', 'RESTITUTION',
        'OPERATION_ERRONE', 'VIREMENT_INTERNATIONAL', 'IMPAYEE', 'PROVISION', 'Importation'
    ));

ALTER TABLE provision_aud DROP CONSTRAINT provision_aud_motif_check;
ALTER TABLE provision_aud
    ADD CONSTRAINT provision_aud_motif_check CHECK (motif IN (
        'LEASING', 'CMT', 'CHEQUE', 'LCN', 'LCN_DECAISSEMENT', 'LCN_ENCAISSEMENT', 'LCN_ESCOMPTE',
        'Virement', 'MiseADisposition', 'PREFINANCEMENT', 'DROIT_CONSTATE', 'DECOMPTES', 'COMMISSION',
        'AGIOS', 'FRAIS_BANCAIRE', 'FACTORING', 'PRELEVEMENT', 'CAUTION', 'IMPOT_TAXE', 'RESTITUTION',
        'OPERATION_ERRONE', 'VIREMENT_INTERNATIONAL', 'IMPAYEE', 'PROVISION', 'Importation'
    ));
