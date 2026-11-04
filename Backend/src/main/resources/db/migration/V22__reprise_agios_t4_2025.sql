-- V22 — Première tranche des agios unifiés : T4/2025 (factoring M12/2025).
-- Accord Finance : 26/10/2026, IDs 4 à 10, montants calculés = facturés faute
-- d'historique des encours 2025, une transaction propre à chaque opération payée.
-- Tables : agios (+ colonnes agios_aud), commission, transaction.
-- ID 8 / transaction 125 : 3 agios + une commission manuelle CAUTION agrégée,
-- sans rattachement à une caution individuelle non identifiée ; total 178458.60.
-- ID 9 / transaction 136 : 3 agios ; total 105370.74.
-- Les IDs 8/9 et transactions 125/136 restent ceux de la facilité de caisse.
-- Les quatre autres agios et cinq transactions nécessaires utilisent les identités.
-- Dates, banques, clôtures, soldes et totaux débit/crédit/TVA des rapprochements
-- sont conservés et contrôlés dans la même transaction Flyway PostgreSQL.
-- TVA : même extraction TTC que TvaResolver/TVAUtils ; taux historique 10 %
-- des transactions 125/136, arrondi HALF_UP à deux décimales par composante.
-- Les autres transactions conservent leur TVA historique, y compris les taux zéro.
-- Pas de détail journalier artificiel en 2025 ; aucune écriture dans les tables
-- agios_trimestriel/agios_journalier. Pas de nouvelle révision Envers SQL,
-- conformément à la décision documentée dans V21 ; anciennes révisions préservées.
-- Préparation de données : l'API et les écrans ne lisent pas encore les colonnes
-- ajoutées. Les champs sont requis pour la tranche reprise, montant_facture excepté.
-- Les autres IDs restent à classer avant un futur NOT NULL global et la bascule
-- du code applicatif. Une base vierge est sans effet ; un état source divergent
-- ou déjà repris est refusé pour éviter une correction partielle ou des doublons.
-- Contrôles :
-- SELECT id, type_agios, periodicite, periode_numero, annee, montant,
--        montant_facture, montant_calcule FROM agios WHERE annee=2025 ORDER BY id;
-- SELECT a.id, count(t.id) FROM agios a LEFT JOIN transaction t
--   ON t.transaction_source='AGIOS' AND t.referent=a.id WHERE a.annee=2025
--   GROUP BY a.id HAVING count(t.id)<>1; -- 0 ligne

ALTER TABLE agios
    ADD COLUMN type_agios varchar(50),
    ADD COLUMN periodicite varchar(20),
    ADD COLUMN periode_numero smallint,
    ADD COLUMN annee smallint,
    ADD COLUMN montant_facture numeric(19,4),
    ADD COLUMN montant_calcule numeric(19,4);
ALTER TABLE agios_aud
    ADD COLUMN type_agios varchar(50),
    ADD COLUMN periodicite varchar(20),
    ADD COLUMN periode_numero smallint,
    ADD COLUMN annee smallint,
    ADD COLUMN montant_facture numeric(19,4),
    ADD COLUMN montant_calcule numeric(19,4);

-- Les IDs non encore traités peuvent rester sans métadonnées. Toute ligne
-- reprise, originale ou créée, doit porter l'ensemble des champs obligatoires.
ALTER TABLE agios ADD CONSTRAINT ck_agios_reprise_complete CHECK (
    (type_agios IS NULL AND periodicite IS NULL
        AND periode_numero IS NULL AND annee IS NULL
        AND montant_calcule IS NULL AND montant_facture IS NULL)
    OR
    (type_agios IS NOT NULL AND periodicite IS NOT NULL AND periode_numero IS NOT NULL
        AND annee IS NOT NULL AND montant_calcule IS NOT NULL
        AND type_agios IN ('FACILITE_CAISSE','PREFINANCEMENT','DROIT_CONSTATE','FACTORING')
        AND ((type_agios='FACTORING' AND periodicite='MENSUEL' AND periode_numero BETWEEN 1 AND 12)
            OR (type_agios<>'FACTORING' AND periodicite='TRIMESTRIEL' AND periode_numero BETWEEN 1 AND 4)))
) NOT VALID;

CREATE TEMP TABLE v22_sources ON COMMIT DROP AS
SELECT * FROM (VALUES
    (4::bigint,20::bigint,5::bigint,3::bigint,22856.98::numeric,2077.91::numeric),
    (5,19,5,3,64858.79,5896.25), (6,62,1,2,19389.83,0),
    (7,68,3,8,230935.73,0), (8,125,7,5,178458.60,16223.51),
    (9,136,4,4,105370.74,9579.16), (10,468,3,8,39469.20,3588.11)
) AS s(agios_id,transaction_id,banque_id,rapprochement_id,montant,tva);

CREATE TEMP TABLE v22_composantes ON COMMIT DROP AS
SELECT * FROM (VALUES
    (4::bigint,1,'AGIOS','FACILITE_CAISSE',22856.98::numeric),
    (5,1,'AGIOS','PREFINANCEMENT',64858.79), (6,1,'AGIOS','DROIT_CONSTATE',19389.83),
    (7,1,'AGIOS','FACTORING',230935.73),
    (8,1,'AGIOS','FACILITE_CAISSE',11902.06), (8,2,'AGIOS','DROIT_CONSTATE',45172.50),
    (8,3,'AGIOS','PREFINANCEMENT',70818.60), (8,4,'COMMISSION',NULL,50565.44),
    (9,1,'AGIOS','FACILITE_CAISSE',30122.29), (9,2,'AGIOS','PREFINANCEMENT',43570.08),
    (9,3,'AGIOS','DROIT_CONSTATE',31678.37), (10,1,'AGIOS','PREFINANCEMENT',39469.20)
) AS p(agios_id,ordre,source,type_agios,montant);

DO $$
DECLARE
    composante record;
    source_agios agios%ROWTYPE;
    source_transaction transaction%ROWTYPE;
    nouvelle_source bigint;
    nouvelle_transaction bigint;
    montant_tva numeric;
    titre text;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM agios WHERE id BETWEEN 4 AND 10) THEN
        RETURN;
    END IF;
    -- Les lignes sources et leurs transactions sont verrouillées pendant la reprise.
    PERFORM a.id FROM agios a JOIN v22_sources s ON s.agios_id=a.id FOR UPDATE OF a;
    PERFORM t.id FROM transaction t JOIN v22_sources s ON s.transaction_id=t.id FOR UPDATE OF t;

    IF EXISTS (
        SELECT 1 FROM v22_sources s LEFT JOIN agios a ON a.id=s.agios_id
        LEFT JOIN transaction t ON t.id=s.transaction_id
        LEFT JOIN rapprochement r ON r.id=s.rapprochement_id
        WHERE a.id IS NULL OR t.id IS NULL OR r.id IS NULL
           OR a.banque_id IS DISTINCT FROM s.banque_id OR a.status IS DISTINCT FROM 'PAYE'
           OR a.montant IS DISTINCT FROM s.montant OR a.type_agios IS NOT NULL
           OR t.transaction_source IS DISTINCT FROM 'AGIOS' OR t.referent IS DISTINCT FROM a.id
           OR t.banque_id IS DISTINCT FROM s.banque_id OR r.banque_id IS DISTINCT FROM s.banque_id
           OR t.rapprochement_id IS DISTINCT FROM s.rapprochement_id
           OR t.debit IS DISTINCT FROM s.montant OR t.credit IS DISTINCT FROM 0::numeric
           OR t.tva_deductible IS DISTINCT FROM s.tva
           OR COALESCE(t.tva_collectee,0)<>0
           OR (SELECT count(*) FROM transaction x WHERE x.transaction_source='AGIOS' AND x.referent=a.id)<>1
    ) THEN
        RAISE EXCEPTION 'V22 : état source différent de la sauvegarde validée, reprise annulée';
    END IF;
    IF EXISTS (SELECT 1 FROM v22_sources s JOIN v22_composantes p ON p.agios_id=s.agios_id
               GROUP BY s.agios_id,s.montant HAVING sum(p.montant)<>s.montant) THEN
        RAISE EXCEPTION 'V22 : somme de ventilation incorrecte';
    END IF;

    -- Captures avant toute modification pour contrôler tous les totaux des rapprochements.
    CREATE TEMP TABLE v22_avant ON COMMIT DROP AS
    SELECT r.id,r.banque_id,r.solde_debut,r.statut,r.date_rapprochement,
           sum(COALESCE(t.debit,0)) debit,sum(COALESCE(t.credit,0)) credit,
           sum(COALESCE(t.tva_deductible,0)) tva,sum(COALESCE(t.tva_collectee,0)) tva_collectee
    FROM rapprochement r LEFT JOIN transaction t ON t.rapprochement_id=r.id
    WHERE r.id IN (SELECT rapprochement_id FROM v22_sources)
    GROUP BY r.id;
    CREATE TEMP TABLE v22_liens (source varchar(20),referent bigint,transaction_id bigint,
                               montant numeric,tva numeric) ON COMMIT DROP;

    FOR source_agios IN SELECT x.* FROM agios x JOIN v22_sources s ON s.agios_id=x.id ORDER BY x.id LOOP
        SELECT x.* INTO STRICT source_transaction FROM transaction x JOIN v22_sources s ON s.transaction_id=x.id WHERE s.agios_id=source_agios.id;
        FOR composante IN SELECT * FROM v22_composantes WHERE agios_id=source_agios.id ORDER BY ordre LOOP
            montant_tva := CASE WHEN source_agios.id IN (8,9) THEN round(composante.montant-composante.montant/1.10,2)
                                ELSE source_transaction.tva_deductible END;
            titre := CASE WHEN composante.source='COMMISSION' THEN 'COMMISSION SUR CAUTION T4 2025'
                          ELSE 'AGIOS T4 2025 - ' || CASE composante.type_agios
                              WHEN 'FACILITE_CAISSE' THEN 'FACILITE DE CAISSE'
                              WHEN 'DROIT_CONSTATE' THEN 'DROITS CONSTATES'
                              ELSE 'PREFINANCEMENT' END END;
            IF composante.source='COMMISSION' THEN
                INSERT INTO commission (banque_id,montant,date_emission,date_operation,date_valeur,
                                        libelle,observation,status,operation)
                VALUES (source_agios.banque_id,composante.montant,source_transaction.date_emission,source_transaction.date_operations,source_transaction.date_valeur,
                        titre,'Commission manuelle agrégée du relevé T4 2025 ; ventilation de l''agios 8.',
                        'PAYE','CAUTION') RETURNING id INTO nouvelle_source;
            ELSIF composante.ordre=1 THEN
                nouvelle_source := source_agios.id;
                UPDATE agios SET montant=composante.montant,type_agios=composante.type_agios,
                    periodicite=CASE WHEN composante.type_agios='FACTORING' THEN 'MENSUEL' ELSE 'TRIMESTRIEL' END,
                    periode_numero=CASE WHEN composante.type_agios='FACTORING' THEN 12 ELSE 4 END,
                    annee=2025,montant_facture=composante.montant,montant_calcule=composante.montant,
                    libelle=CASE WHEN source_agios.id IN (8,9) THEN titre ELSE source_agios.libelle END,
                    observation=CASE WHEN source_agios.id IN (8,9) THEN titre ELSE source_agios.observation END
                WHERE id=source_agios.id;
            ELSE
                INSERT INTO agios (banque_id,montant,date_emission,date_operation,date_valeur,
                                   libelle,observation,status,type_agios,periodicite,periode_numero,
                                   annee,montant_facture,montant_calcule)
                VALUES (source_agios.banque_id,composante.montant,source_transaction.date_emission,source_transaction.date_operations,source_transaction.date_valeur,
                        titre,titre,'PAYE',composante.type_agios,'TRIMESTRIEL',4,2025,composante.montant,composante.montant)
                RETURNING id INTO nouvelle_source;
            END IF;

            IF composante.ordre=1 THEN
                nouvelle_transaction := source_transaction.id;
                IF source_agios.id IN (8,9) THEN
                    UPDATE transaction SET debit=composante.montant,tva_deductible=montant_tva,
                        libelle=titre,observation=titre,updated_at=CURRENT_TIMESTAMP WHERE id=source_transaction.id;
                END IF;
            ELSE
                INSERT INTO transaction (banque_id,rapprochement_id,transaction_source,referent,
                    debit,credit,tva_deductible,tva_collectee,date_emission,date_operations,date_valeur,
                    libelle,observation,created_at,updated_at)
                VALUES (source_transaction.banque_id,source_transaction.rapprochement_id,composante.source,nouvelle_source,composante.montant,0,
                        montant_tva,0,source_transaction.date_emission,source_transaction.date_operations,source_transaction.date_valeur,
                        titre,CASE WHEN composante.source='COMMISSION'
                            THEN 'Commission manuelle agrégée du relevé T4 2025 ; ventilation de l''agios 8.'
                            ELSE titre END,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
                RETURNING id INTO nouvelle_transaction;
            END IF;
            INSERT INTO v22_liens VALUES (composante.source,nouvelle_source,nouvelle_transaction,composante.montant,montant_tva);
        END LOOP;
    END LOOP;

    IF EXISTS (
        SELECT 1 FROM v22_liens l LEFT JOIN transaction t ON t.id=l.transaction_id
        WHERE t.id IS NULL OR t.transaction_source IS DISTINCT FROM l.source
           OR t.referent IS DISTINCT FROM l.referent OR t.debit IS DISTINCT FROM l.montant
           OR t.tva_deductible IS DISTINCT FROM l.tva
           OR (SELECT count(*) FROM transaction x WHERE x.transaction_source=l.source AND x.referent=l.referent)<>1
    ) OR EXISTS (
        SELECT 1 FROM v22_liens l JOIN agios a ON l.source='AGIOS' AND a.id=l.referent
        WHERE a.montant<>l.montant OR a.montant_calcule IS DISTINCT FROM l.montant
           OR a.montant_facture IS DISTINCT FROM l.montant OR a.type_agios IS NULL
           OR a.annee IS DISTINCT FROM 2025::smallint
    ) THEN
        RAISE EXCEPTION 'V22 : une source ne possède pas sa transaction propre et les montants attendus';
    END IF;
    IF EXISTS (
        SELECT 1 FROM v22_avant b JOIN rapprochement r ON r.id=b.id
        LEFT JOIN transaction t ON t.rapprochement_id=r.id
        GROUP BY b.id,b.banque_id,b.solde_debut,b.statut,b.date_rapprochement,
                 b.debit,b.credit,b.tva,b.tva_collectee,r.id
        HAVING sum(COALESCE(t.debit,0)) IS DISTINCT FROM b.debit
            OR sum(COALESCE(t.credit,0)) IS DISTINCT FROM b.credit
            OR sum(COALESCE(t.tva_deductible,0)) IS DISTINCT FROM b.tva
            OR sum(COALESCE(t.tva_collectee,0)) IS DISTINCT FROM b.tva_collectee
            OR r.banque_id IS DISTINCT FROM b.banque_id OR r.solde_debut IS DISTINCT FROM b.solde_debut
            OR r.statut IS DISTINCT FROM b.statut OR r.date_rapprochement IS DISTINCT FROM b.date_rapprochement
    ) THEN
        RAISE EXCEPTION 'V22 : totaux ou clôture de rapprochement modifiés, reprise annulée';
    END IF;
END $$;

ALTER TABLE agios VALIDATE CONSTRAINT ck_agios_reprise_complete;
