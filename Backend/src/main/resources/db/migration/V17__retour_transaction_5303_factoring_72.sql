-- V17 — Retour arrière de V15 : la transaction 5303 redevient le déblocage du factoring n° 72 (GUERCIF-NADOR)
--
-- Objectif        : annuler V15. Le décompte « DECOMPTE GUERCIF-NADOR » créé par V15 est supprimé, le factoring n° 72
--                   (déblocage de 6 007 550,15, marché 2, banque 3) est recréé avec son identifiant d'origine, et la transaction
--                   5303 (rapprochement n° 62, septembre 2026) lui est rattachée de nouveau.
-- Tables          : factoring, transaction, decompte (lecture de factoring_aud et transaction_aud).
-- Impact données  : - factoring 72 recréé : DEBLOCAGE, PAYE, marché 2, montant, dates d'émission, d'opération et de valeur
--                     repris du décompte (identiques au factoring d'origine) ; libellé, observation et lignes fonctionnelles
--                     repris de la dernière version du factoring 72 dans factoring_aud ;
--                   - transaction 5303 : transaction_source DECOMPTES -> FACTORING, referent -> 72 ; libellé, observation et
--                     TVA collectée repris de sa dernière version « FACTORING / 72 » dans transaction_aud (TVA collectée 0 si
--                     l'audit n'en porte pas) ; montant, dates et rapprochement inchangés, donc débit, crédit et soldes du
--                     rapprochement 62 inchangés (son total de TVA collectée diminue de 1 001 258,36) ;
--                   - décompte supprimé (aucune transaction ne le référence plus) ;
--                   - le factoring 72 revient dans l'encours des agios « factoring » de la banque 3 : lancer « Recalculer tout »
--                     après déploiement.
-- Audit           : une migration n'écrit pas dans factoring_aud / transaction_aud (décision HMAYDA 10/10/2026, comme V3, V6, V15).
-- Garde-fous      : chaque instruction ne s'applique que si l'état attendu (celui laissé par V15) est présent : no-op sur une
--                   base où V15 n'a rien changé, et en cas de rejeu. Le décompte est retrouvé par ses données et par la
--                   transaction 5303, jamais par son identifiant.
-- Accord          : HMAYDA, 19/10/2026.
--
-- Contrôles (après exécution) :
--   SELECT transaction_source, referent, libelle, observation, tva_collectee FROM transaction WHERE id = 5303;
--       -- FACTORING, 72, libellé et observation d'origine, 0.00
--   SELECT type, status, montant, date_operation, date_valeur FROM factoring WHERE id = 72;
--       -- DEBLOCAGE, PAYE, 6007550.15, 2026-09-07, 2026-09-08
--   SELECT count(*) FROM decompte WHERE marche_id = 2 AND libelle = 'DECOMPTE GUERCIF-NADOR';   -- 0

-- 1. Le factoring 72 est recréé à partir du décompte que la transaction 5303 référence (une seule fois).
INSERT INTO factoring (id, marche_id, montant, date_emission, date_operation, date_valeur, libelle, observation, status, type,
                       ligne_fonctionnelle_id, ligne_fonctionnelle_specifique_id)
SELECT 72, d.marche_id, d.montant, d.date_emission, d.date_operation, d.date_valeur,
       COALESCE(a.libelle, d.libelle), COALESCE(a.observation, d.observation), 'PAYE', 'DEBLOCAGE',
       a.ligne_fonctionnelle_id, a.ligne_fonctionnelle_specifique_id
FROM transaction t
JOIN decompte d ON t.transaction_source = 'DECOMPTES' AND t.referent = d.id
LEFT JOIN LATERAL (SELECT fa.* FROM factoring_aud fa
                   WHERE fa.id = 72 AND fa.revtype <> 2
                   ORDER BY fa.rev DESC LIMIT 1) a ON true
WHERE t.id = 5303
  AND t.credit = 6007550.15
  AND d.marche_id = 2
  AND d.libelle = 'DECOMPTE GUERCIF-NADOR'
  AND d.montant = t.credit
  AND d.banque_encaissement_id = t.banque_id
  AND NOT EXISTS (SELECT 1 FROM factoring WHERE id = 72);

-- 2. La transaction 5303 redevient celle du factoring 72, avec son libellé, son observation et sa TVA d'origine.
UPDATE transaction t
SET transaction_source = 'FACTORING',
    referent           = 72,
    libelle            = COALESCE(a.libelle, f.libelle),
    observation        = COALESCE(a.observation, f.observation),
    tva_collectee      = COALESCE(a.tva_collectee, 0)
FROM factoring f
LEFT JOIN LATERAL (SELECT ta.libelle, ta.observation, ta.tva_collectee FROM transaction_aud ta
                   WHERE ta.id = 5303 AND ta.revtype <> 2
                     AND ta.transaction_source = 'FACTORING' AND ta.referent = 72
                   ORDER BY ta.rev DESC LIMIT 1) a ON true
WHERE t.id = 5303
  AND t.transaction_source = 'DECOMPTES'
  AND t.credit = 6007550.15
  AND f.id = 72
  AND f.type = 'DEBLOCAGE'
  AND f.montant = t.credit;

-- 3. Suppression du décompte créé par V15, seulement s'il n'est plus référencé par aucune transaction.
DELETE FROM decompte d
WHERE d.marche_id = 2
  AND d.libelle = 'DECOMPTE GUERCIF-NADOR'
  AND d.montant = 6007550.15
  AND d.banque_encaissement_id = 3
  AND EXISTS (SELECT 1 FROM transaction WHERE id = 5303 AND transaction_source = 'FACTORING' AND referent = 72)
  AND NOT EXISTS (SELECT 1 FROM transaction
                  WHERE transaction_source = 'DECOMPTES' AND referent = d.id);
