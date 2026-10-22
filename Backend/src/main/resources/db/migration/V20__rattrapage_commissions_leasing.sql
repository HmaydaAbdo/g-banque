-- V20 — Rattrapage des commissions leasing des échéances prévues existantes
--
-- Objectif        : les commissions leasing ne sont créées qu'à la saisie (ou à la régénération) d'un contrat. Une règle de
--                   commission ajoutée ensuite (ex. BMCE, 11 DH) ne s'appliquait donc pas aux échéances déjà générées. Cette
--                   migration crée, une seule fois, la commission manquante de chaque échéance PRÉVUE, avec la règle en vigueur
--                   (même résolution que l'application : SI_MATCH sur l'organisme, sinon SI_NON_MATCH si la banque a une règle
--                   SI_MATCH, sinon DEFAULT).
-- Tables          : commission (insertions) ; lecture de echeance_leasing, contrat_leasing, contrat_cadre, organisme_leasing,
--                   commission_config.
-- Impact données  : - uniquement des échéances au statut PREVU sans moyen de paiement rattaché ; les échéances PAYE ne sont
--                     jamais touchées (HMAYDA, 22/10/2026) ;
--                   - l'avance (échéance n°1 d'un contrat avec avance) est exclue : elle est réglée par un chèque / une LCN / un
--                     virement ;
--                   - une échéance qui a déjà sa commission (opération LEASING, même libellé, même date d'émission) est ignorée :
--                     la migration est idempotente ;
--                   - banques sans règle leasing (AWB, CDM) : rien n'est créé ; aucune commission existante n'est modifiée.
-- Audit           : une migration n'écrit pas dans les tables _aud (décision HMAYDA 10/10/2026, comme V3, V6, V15, V18 et V19).
-- Accord          : HMAYDA, 22/10/2026 (échéances prévues uniquement, migration + action d'administration).
--
-- Contrôle (après exécution) : aucune échéance prévue d'une banque dotée d'une règle leasing ne reste sans commission :
--   SELECT count(*) FROM echeance_leasing e JOIN contrat_leasing c ON c.id = e.contrat_leasing_id
--   WHERE e.statut_paiement = 'PREVU' AND e.moyen_paiement_type IS NULL
--     AND EXISTS (SELECT 1 FROM commission_config r WHERE r.banque_id = c.banque_id AND r.type = 'LEASING')
--     AND NOT EXISTS (SELECT 1 FROM commission k WHERE k.operation = 'LEASING' AND k.banque_id = c.banque_id
--                       AND k.libelle = 'Commission Prelevement Leasing contrat N°' || c.numero_contrat
--                       AND k.date_emission = e.date_echeance);
--       -- 0, sauf contrat dont l'organisme n'a aucune règle applicable

INSERT INTO commission (date_emission, montant, banque_id, operation, status, libelle, observation)
SELECT e.date_echeance,
       m.montant,
       c.banque_id,
       'LEASING',
       'PREVU',
       'Commission Prelevement Leasing contrat N°' || c.numero_contrat,
       'Commission Prelevement Leasing contrat N°' || c.numero_contrat
FROM echeance_leasing e
JOIN contrat_leasing c ON c.id = e.contrat_leasing_id
JOIN contrat_cadre cc ON cc.id = c.contrat_cadre_id
JOIN organisme_leasing o ON o.id = cc.organisme_id
CROSS JOIN LATERAL (
    SELECT COALESCE(
        (SELECT r.montant FROM commission_config r
          WHERE r.banque_id = c.banque_id AND r.type = 'LEASING'
            AND r.commission_condition = 'SI_MATCH' AND lower(r.organisme_nom) = lower(o.nom)
          ORDER BY r.id LIMIT 1),
        CASE WHEN EXISTS (SELECT 1 FROM commission_config r
                           WHERE r.banque_id = c.banque_id AND r.type = 'LEASING' AND r.commission_condition = 'SI_MATCH')
             THEN (SELECT r.montant FROM commission_config r
                    WHERE r.banque_id = c.banque_id AND r.type = 'LEASING' AND r.commission_condition = 'SI_NON_MATCH'
                    ORDER BY r.id LIMIT 1)
             ELSE (SELECT r.montant FROM commission_config r
                    WHERE r.banque_id = c.banque_id AND r.type = 'LEASING' AND r.commission_condition = 'DEFAULT'
                    ORDER BY r.id LIMIT 1)
        END) AS montant
) m
WHERE e.statut_paiement = 'PREVU'
  AND e.moyen_paiement_type IS NULL
  AND m.montant IS NOT NULL
  AND NOT (c.avance_ttc IS NOT NULL AND e.numero_echeance = 1)
  AND NOT EXISTS (SELECT 1 FROM commission k
                   WHERE k.operation = 'LEASING'
                     AND k.libelle = 'Commission Prelevement Leasing contrat N°' || c.numero_contrat
                     AND k.date_emission = e.date_echeance);
