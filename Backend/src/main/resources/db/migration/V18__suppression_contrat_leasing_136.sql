-- V18 — Suppression du contrat de leasing n° 136 (contrat de test créé le 19-20/10/2026) et de ses échéances
--
-- Objectif        : retirer le contrat 136, créé par erreur peu après la mise en service de l'avance, avec ses 4 à 5 échéances,
--                   ses phases et ses commissions de prélèvement. S'il avait une avance, la mention « Avance — échéance n°1 du
--                   contrat N° … » est retirée du libellé et de l'observation du chèque, de la LCN ou du virement choisi (et de sa
--                   transaction s'il était réglé), qui retrouvent leurs textes d'origine.
-- Tables          : contrat_leasing, echeance_leasing, contrat_leasing_phase, commission, cheques, lcns, virements, transaction.
-- Impact données  : - supprimés : le contrat 136, ses échéances, ses phases et ses commissions de prélèvement au statut PREVU ;
--                   - le chèque / la LCN / le virement de l'avance n'est PAS supprimé : seuls son libellé et son observation (et
--                     ceux de sa transaction) sont nettoyés ; il redevient un moyen de paiement libre ;
--                   - historique conservé dans contrat_leasing_aud, echeance_leasing_aud, contrat_leasing_phase_aud et commission_aud.
-- Audit           : une migration n'écrit pas dans les tables _aud (décision HMAYDA 10/10/2026, comme V3, V6 et V15).
-- Garde-fous      : rien n'est supprimé (no-op, et en cas de rejeu) si le contrat 136 n'existe pas, s'il a été créé avant le
--                   19/10/2026 (autre contrat que celui de test), si une de ses échéances est payée à la main, si une transaction
--                   LEASING référence une de ses échéances ou si une de ses commissions est payée. Une avance dont le moyen de
--                   paiement est réglé n'empêche pas la suppression : elle n'a pas de transaction LEASING, la seule transaction est
--                   celle du moyen de paiement, qui reste en place.
-- Accord          : HMAYDA, 20/10/2026.
--
-- Contrôles (après exécution) :
--   SELECT count(*) FROM contrat_leasing WHERE id = 136;                                   -- 0
--   SELECT count(*) FROM echeance_leasing WHERE contrat_leasing_id = 136;                  -- 0
--   SELECT count(*) FROM cheques WHERE libelle LIKE '%Avance — échéance n°1 du contrat N°%'; -- 0 pour ce contrat

DO $$
DECLARE
    v_numero TEXT;
BEGIN
    SELECT c.numero_contrat INTO v_numero
    FROM contrat_leasing c
    WHERE c.id = 136
      AND c.date_creation >= TIMESTAMPTZ '2026-10-19 00:00:00+00'
      AND NOT EXISTS (SELECT 1 FROM echeance_leasing e
                      WHERE e.contrat_leasing_id = c.id AND e.moyen_paiement_id IS NULL AND e.statut_paiement = 'PAYE')
      AND NOT EXISTS (SELECT 1 FROM transaction t
                      JOIN echeance_leasing e ON e.id = t.referent
                      WHERE e.contrat_leasing_id = c.id AND t.transaction_source = 'LEASING')
      AND NOT EXISTS (SELECT 1 FROM commission k
                      WHERE k.operation = 'LEASING' AND k.status = 'PAYE'
                        AND k.libelle = 'Commission Prelevement Leasing contrat N°' || c.numero_contrat);

    IF v_numero IS NULL THEN
        RAISE NOTICE 'V18 : contrat de leasing 136 absent ou protégé par un garde-fou, aucune suppression.';
        RETURN;
    END IF;

    -- 1. Mention d'avance retirée du moyen de paiement et de sa transaction (textes comparés sans collation « ci_ai »,
    --    dont les comparaisons ne supportent pas replace()).
    UPDATE cheques m
    SET libelle     = replace(m.libelle COLLATE "C", ' | Avance — échéance n°1 du contrat N° ' || v_numero, ''),
        observation = replace(m.observation COLLATE "C",
                              ' | Avance réglée via chèque' || coalesce(' n° ' || nullif(trim(m.numero::text COLLATE "C"), ''), '')
                              || ' — échéance n°1 du contrat N° ' || v_numero, '')
    FROM echeance_leasing e
    WHERE e.contrat_leasing_id = 136 AND e.moyen_paiement_type = 'CHEQUE' AND e.moyen_paiement_id = m.id;

    UPDATE transaction t
    SET libelle     = replace(t.libelle COLLATE "C", ' | Avance — échéance n°1 du contrat N° ' || v_numero, ''),
        observation = replace(t.observation COLLATE "C",
                              ' | Avance réglée via chèque' || coalesce(' n° ' || nullif(trim(m.numero::text COLLATE "C"), ''), '')
                              || ' — échéance n°1 du contrat N° ' || v_numero, '')
    FROM cheques m
    JOIN echeance_leasing e ON e.moyen_paiement_type = 'CHEQUE' AND e.moyen_paiement_id = m.id
    WHERE e.contrat_leasing_id = 136 AND t.transaction_source = 'CHEQUE' AND t.referent = m.id;

    UPDATE lcns m
    SET libelle     = replace(m.libelle COLLATE "C", ' | Avance — échéance n°1 du contrat N° ' || v_numero, ''),
        observation = replace(m.observation COLLATE "C",
                              ' | Avance réglée via LCN' || coalesce(' n° ' || nullif(trim(m.numero::text COLLATE "C"), ''), '')
                              || ' — échéance n°1 du contrat N° ' || v_numero, '')
    FROM echeance_leasing e
    WHERE e.contrat_leasing_id = 136 AND e.moyen_paiement_type = 'LCN' AND e.moyen_paiement_id = m.id;

    UPDATE transaction t
    SET libelle     = replace(t.libelle COLLATE "C", ' | Avance — échéance n°1 du contrat N° ' || v_numero, ''),
        observation = replace(t.observation COLLATE "C",
                              ' | Avance réglée via LCN' || coalesce(' n° ' || nullif(trim(m.numero::text COLLATE "C"), ''), '')
                              || ' — échéance n°1 du contrat N° ' || v_numero, '')
    FROM lcns m
    JOIN echeance_leasing e ON e.moyen_paiement_type = 'LCN' AND e.moyen_paiement_id = m.id
    WHERE e.contrat_leasing_id = 136 AND t.transaction_source IN ('LCN_DECAISSEMENT', 'LCN') AND t.referent = m.id;

    UPDATE virements m
    SET libelle     = replace(m.libelle COLLATE "C", ' | Avance — échéance n°1 du contrat N° ' || v_numero, ''),
        observation = replace(m.observation COLLATE "C",
                              ' | Avance réglée via virement' || coalesce(' n° ' || nullif(trim(m.numero::text COLLATE "C"), ''), '')
                              || ' — échéance n°1 du contrat N° ' || v_numero, '')
    FROM echeance_leasing e
    WHERE e.contrat_leasing_id = 136 AND e.moyen_paiement_type = 'VIREMENT' AND e.moyen_paiement_id = m.id;

    UPDATE transaction t
    SET libelle     = replace(t.libelle COLLATE "C", ' | Avance — échéance n°1 du contrat N° ' || v_numero, ''),
        observation = replace(t.observation COLLATE "C",
                              ' | Avance réglée via virement' || coalesce(' n° ' || nullif(trim(m.numero::text COLLATE "C"), ''), '')
                              || ' — échéance n°1 du contrat N° ' || v_numero, '')
    FROM virements m
    JOIN echeance_leasing e ON e.moyen_paiement_type = 'VIREMENT' AND e.moyen_paiement_id = m.id
    WHERE e.contrat_leasing_id = 136 AND t.transaction_source = 'Virement' AND t.referent = m.id;

    -- 2. Commissions de prélèvement du contrat (libellé exact, statut PREVU : les payées ont arrêté la migration plus haut).
    DELETE FROM commission k
    WHERE k.operation = 'LEASING' AND k.status = 'PREVU'
      AND k.libelle = 'Commission Prelevement Leasing contrat N°' || v_numero;

    -- 3. Échéances, phases, puis contrat.
    DELETE FROM echeance_leasing WHERE contrat_leasing_id = 136;
    DELETE FROM contrat_leasing_phase WHERE contrat_leasing_id = 136;
    DELETE FROM contrat_leasing WHERE id = 136;

    RAISE NOTICE 'V18 : contrat de leasing 136 (N° %) supprimé avec ses échéances, phases et commissions.', v_numero;
END $$;
