-- V4 — rapprochement.date_rapprochement : bytea (YearMonth Java sérialisé) -> date (1er jour du mois)
--
-- Objectif        : la colonne contenait la sérialisation binaire Java d'un YearMonth (Hibernate, faute de convertisseur) :
--                   illisible et inutilisable en SQL. L'entité déclare désormais un convertisseur YearMonth <-> date
--                   (YearMonthDateConverter) ; la colonne devient une vraie date, le 1er du mois (2026-01 -> 2026-01-01).
-- Tables          : rapprochement, rapprochement_aud (historique d'audit Envers).
-- Impact données  : aucune donnée perdue — chaque valeur est décodée (année et mois lus dans le flux sérialisé) ;
--                   l'index idx_rapprochement_date est reconstruit automatiquement par PostgreSQL.
--                   Une valeur dont le format n'est pas celui attendu fait échouer la migration (aucune écriture partielle :
--                   Flyway exécute la migration dans une transaction) — jamais de conversion approximative.
-- Diagnostic (production, copie du 10/10/2026, lecture seule) :
--                   rapprochement : 70 lignes, une seule longueur (43 octets) ; rapprochement_aud : 1 089 lignes, une seule
--                   longueur (43 octets). Format : en-tête ACED0005, classe java.time.Ser, bloc de 6 octets
--                   [type 12][année sur 4 octets][mois sur 1 octet], fin de bloc 78.
-- Accord          : HMAYDA, 10/10/2026 (liste de session, point 5).
--
-- Contrôles (après exécution) :
--   SELECT data_type FROM information_schema.columns
--    WHERE table_name IN ('rapprochement', 'rapprochement_aud') AND column_name = 'date_rapprochement';  -- date, date
--   SELECT count(*), min(date_rapprochement), max(date_rapprochement) FROM rapprochement;                 -- 70, 1er du mois
--   SELECT count(*) FROM rapprochement WHERE extract(day FROM date_rapprochement) <> 1;                    -- 0

CREATE FUNCTION pg_temp.yearmonth_java_vers_date(b bytea) RETURNS date
    LANGUAGE plpgsql IMMUTABLE AS
$$
DECLARE
    l   integer;
    an  integer;
    mois integer;
BEGIN
    IF b IS NULL THEN
        RETURN NULL;
    END IF;
    l := length(b);
    IF l < 10
       OR substring(b FROM 1 FOR 4) <> '\xaced0005'::bytea
       OR get_byte(b, l - 1) <> 120   -- 0x78 : fin de bloc
       OR get_byte(b, l - 9) <> 119   -- 0x77 : début de bloc de données
       OR get_byte(b, l - 8) <> 6     -- longueur du bloc : 6 octets
       OR get_byte(b, l - 7) <> 12 THEN  -- type java.time.Ser : YEAR_MONTH
        RAISE EXCEPTION 'date_rapprochement : format binaire inattendu (%)', encode(b, 'hex');
    END IF;
    an   := (get_byte(b, l - 6) << 24) | (get_byte(b, l - 5) << 16) | (get_byte(b, l - 4) << 8) | get_byte(b, l - 3);
    mois := get_byte(b, l - 2);
    RETURN make_date(an, mois, 1);   -- échoue si le mois n'est pas compris entre 1 et 12
END;
$$;

ALTER TABLE rapprochement
    ALTER COLUMN date_rapprochement TYPE date USING pg_temp.yearmonth_java_vers_date(date_rapprochement);

ALTER TABLE rapprochement_aud
    ALTER COLUMN date_rapprochement TYPE date USING pg_temp.yearmonth_java_vers_date(date_rapprochement);

DROP FUNCTION pg_temp.yearmonth_java_vers_date(bytea);
