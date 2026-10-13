-- V9 — Remboursement de préfinancement n° 34 (BMCI, GUERCIF-NADOR) : scission capital / agios
--
-- Objectif        : le remboursement 34 (3 993 140,60) dépasse de 13 376,45 le déblocage qu'il solde (3 979 764,16 ; le
--                   centime d'écart vient du report des cents des tranches précédentes). Ce surplus est un agios
--                   prélevé par la banque avec le remboursement. Il est scindé en deux opérations validées, de mêmes dates :
--                   - le remboursement 34 : 3 979 764,15 (capital) ;
--                   - un nouvel agios PAYE de 13 376,45 (TVA déductible 10 % incluse : 1 216,04), avec sa transaction.
--                   L'encours de préfinancement du marché GUERCIF-NADOR (marché 2) retombe ainsi à 0,00 (il était à −13 376,45).
-- Tables          : prefinancement, transaction, agios.
-- Impact données  : - transaction 1187 (rapprochement 18, CLOTURE_DEFINITIVE) : débit 3 993 140,60 -> 3 979 764,15 ;
--                   - nouvelle transaction AGIOS (rapprochement 18, banque 3) : débit 13 376,45, TVA déductible 1 216,04,
--                     dates (émission 12/03, opération 13/03, valeur 18/02/2026), libellé et observation de l'agios 22 ;
--                   - nouvel agios PAYE (banque 3, 13 376,45), mêmes dates et mêmes textes que sa transaction ;
--                   - préfinancement 34 : montant 3 979 764,15 ; dates d'opération et de valeur alignées sur la transaction
--                     1187, source de vérité (13/03 et 18/02/2026 ; elles étaient inversées).
--                   Total débité du rapprochement 18 inchangé (3 979 764,15 + 13 376,45 = 3 993 140,60) : soldes inchangés.
-- Audit           : une migration n'écrit pas dans les tables _aud (décision HMAYDA 10/10/2026, comme V3).
-- Garde-fous      : tout est conditionné à l'état d'origine (transaction 1187 à 3 993 140,60 rattachée au remboursement 34
--                   de même montant, agios 22 présent) : sans effet sur une base vierge ou déjà corrigée (rejeu sûr).
-- Accord          : HMAYDA, 13/10/2026 (dates de la transaction, TVA 10 % incluse, textes de l'agios 22).
--
-- Contrôles (après exécution) :
--   SELECT montant, date_operation, date_valeur FROM prefinancement WHERE id = 34;     -- 3979764.15, 2026-03-13, 2026-02-18
--   SELECT id, debit, tva_deductible, referent FROM transaction WHERE rapprochement_id = 18 AND transaction_source IN ('PREFINANCEMENT', 'AGIOS') AND debit IN (3979764.15, 13376.45);
--   SELECT sum(CASE WHEN type = 'DEBLOCAGE' THEN montant ELSE -montant END) FROM prefinancement WHERE marche_id = 2 AND status = 'PAYE';   -- 0.00

-- 1. Nouvel agios et sa transaction (une seule instruction : la transaction reprend l'identifiant généré).
WITH source AS (
    SELECT t.date_emission, t.date_operations, t.date_valeur, a.libelle, a.observation
    FROM transaction t
    JOIN prefinancement p ON p.id = t.referent
    JOIN agios a ON a.id = 22
    WHERE t.id = 1187
      AND t.transaction_source = 'PREFINANCEMENT'
      AND t.banque_id = 3
      AND t.rapprochement_id = 18
      AND t.debit = 3993140.60
      AND p.id = 34
      AND p.type = 'REMBOURSEMENT'
      AND p.status = 'PAYE'
      AND p.montant = 3993140.60
),
nouvel_agios AS (
    INSERT INTO agios (banque_id, status, montant, date_emission, date_operation, date_valeur, libelle, observation)
    SELECT 3, 'PAYE', 13376.45, date_emission, date_operations, date_valeur, libelle, observation
    FROM source
    RETURNING id, date_emission, date_operation, date_valeur, libelle, observation
)
INSERT INTO transaction (transaction_source, referent, banque_id, rapprochement_id, debit, credit,
                         tva_deductible, tva_collectee, date_emission, date_operations, date_valeur,
                         libelle, observation, created_at, updated_at)
SELECT 'AGIOS', id, 3, 18, 13376.45, 0.00,
       1216.04, 0.00, date_emission, date_operation, date_valeur,
       libelle, observation, now(), now()
FROM nouvel_agios;

-- 2. La transaction 1187 ne porte plus que le capital (seulement si l'agios vient d'être créé : jamais deux fois).
UPDATE transaction t
SET debit = 3979764.15
WHERE t.id = 1187
  AND t.transaction_source = 'PREFINANCEMENT'
  AND t.referent = 34
  AND t.debit = 3993140.60
  AND EXISTS (SELECT 1 FROM transaction x
              WHERE x.transaction_source = 'AGIOS' AND x.rapprochement_id = 18 AND x.banque_id = 3
                AND x.debit = 13376.45 AND x.date_valeur = t.date_valeur);

-- 3. Le remboursement 34 reprend le montant et les dates de sa transaction.
UPDATE prefinancement p
SET montant        = t.debit,
    date_operation = t.date_operations,
    date_valeur    = t.date_valeur
FROM transaction t
WHERE p.id = 34
  AND p.montant = 3993140.60
  AND t.id = 1187
  AND t.transaction_source = 'PREFINANCEMENT'
  AND t.referent = p.id
  AND t.debit = 3979764.15;
