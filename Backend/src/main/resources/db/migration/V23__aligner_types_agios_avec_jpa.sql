-- Aligne les colonnes période de agios avec les propriétés Java Integer.
-- SMALLINT est conservé dans l'historique V22 ; cette migration additive
-- permet aussi bien l'application séquentielle V22→V23 que la mise à niveau
-- d'une base où V22 est déjà passée.
ALTER TABLE agios
    ALTER COLUMN periode_numero TYPE integer USING periode_numero::integer,
    ALTER COLUMN annee TYPE integer USING annee::integer;

ALTER TABLE agios_aud
    ALTER COLUMN periode_numero TYPE integer USING periode_numero::integer,
    ALTER COLUMN annee TYPE integer USING annee::integer;
