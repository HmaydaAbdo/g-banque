# TODO prioritaire — Intégrité et fiabilité des données

> Écrit le 13/10/2026 à l'issue de la session « hygiène des agios » (migrations V6 à V12, production en V12).
> Objectif : ne plus retomber sur les défauts de données rencontrés, en agissant sur les **règles métier** et
> sur les **contrôles automatiques** de l'application. Rien n'est implémenté à ce jour : chaque point attend
> la décision indiquée.

---

## 1. Ce qui a été rencontré (familles de problèmes)

| Famille | Cas rencontrés | Pourquoi l'application l'a laissé passer |
|---|---|---|
| **A. Doublon ou mauvais type de saisie** | DC 76 doublon du DC 74 (V3) ; DC 81 saisi à la place du décompte 61 (V6) | Rien ne compare une nouvelle saisie à l'existant. |
| **B. Lien qui ne correspond pas** | Transaction 63 (AWB) liée à l'agios 4 (SAHAM) (V7) ; périodes d'agios liées à des agios supprimés (V8) | Aucune unicité (source, référent), pas de clé étrangère, la suppression ne vérifie pas les liens. |
| **C. Dates divergentes entre un référent et sa transaction** | Virement 1814 (V5) ; 3 préfinancements (V10) ; 8 agios (V11) | Les dates sont stockées à deux endroits, recopiées seulement à l'édition d'une transaction. |
| **D. Montant ou encours incohérent** | Remboursement de préfinancement supérieur de 13 376,45 au déblocage (V9) | L'encours négatif est accepté ; capital et agios saisis dans une seule ligne. |
| **E. Calcul vide, en retard ou à l'arrêt** | Périodes à 0 jour (V12) ; périodes T2 restées ouvertes ; calculs figés au 07/09 ; factoring BMCI de mars ouvert depuis mars | Rien n'alerte quand un calcul n'a pas tourné ou qu'une période terminée n'est pas clôturée. |
| **F. Valeur en base inconnue du code** | « Importation » absente de l'énumération (erreur 500 sur 8 commissions) | Rien ne confronte les valeurs réelles de la base au code. |
| **G. Correction impossible dans l'application** | Rapprochements en clôture définitive | Seule une migration SQL peut corriger, sans trace métier. |

---

## 2. Règles et contrôles proposés (du plus rentable au plus structurant)

1. **Contrôle d'intégrité nocturne** *(décision d'architecture requise)* — une tâche planifiée exécute les contrôles
   déjà validés sur la copie de production : agios sans transaction, référents multiples, dates ou montants qui
   divergent entre référent et transaction, liens morts, encours négatifs, périodes vides, période terminée non
   clôturée depuis N jours, dernier calcul de plus de 48 h. Résultat dans le module **Anomalies** existant, avec
   alerte à Finance. Couvre les familles B, C, D, E, F.
   *À décider :* écran (module Anomalies existant ou écran dédié) ; destinataires de l'alerte (Finance seule ou aussi
   l'administrateur par e-mail).
2. **Encours jamais négatif** *(règle métier à valider avec Finance)* — à la validation d'un remboursement de
   préfinancement, de droit constaté ou de factoring, refuser (ou avertir) si le cumul dépasse les déblocages ;
   message : saisir le surplus comme agios ou frais. Famille D.
3. **Avertissement de doublon à la saisie** *(règle métier à valider avec Finance)* — avant d'enregistrer un droit
   constaté, un décompte ou un préfinancement, chercher une saisie voisine (même marché, même montant, dates
   proches, y compris dans l'autre type) et avertir ; blocage facultatif. Familles A.
4. **Intégrité du lien référent ↔ transaction** *(décision d'architecture requise)* — index unique
   (source, référent) sur les transactions (vérifier d'abord que toutes les sources le respectent) ; interdire la
   suppression d'une opération déjà liée à une transaction ; supprimer `agios_definitif_id` (ou le placer sous
   clé étrangère). Famille B.
5. **Une seule source de vérité pour les dates** — la transaction fait foi : chaque modification de transaction et
   chaque bascule de statut recopie les dates dans le référent, avec un test par type de source. Famille C.
6. **Test « valeurs de la base ↔ code »** — vérifier, sur une copie de production, que toute valeur d'énumération
   présente en base existe dans le code. Famille F.
7. **Corrections de données encadrées** *(décision d'architecture requise)* — garder la discipline actuelle
   (migration Flyway gardée par l'état attendu, test, rejeu sur copie de production) et ajouter un journal des
   corrections pour la trace métier absente de l'audit. Famille G.
8. **Modèle d'agios unifié** (décidé, voir `CHANTIER-AGIOS-UNIFIES.md`) — calculé et facturé côte à côte, écart
   toujours visible, la clôture ne crée plus d'agios en doublon. Familles B, E.

**Ordre de travail recommandé :** 1, puis 2 et 3, puis 4, 5 et 6, puis 7. Le point 8 suit son propre plan.

---

## 3. Décisions déjà prises dans la session (à ne pas rouvrir)

- Les dates de la **transaction** font foi pour tout référent (agios, préfinancement…).
- Les migrations de données restent des migrations Flyway, gardées par l'état attendu, testées et rejouées sur une
  copie de production ; elles n'écrivent pas dans les tables d'audit (décision HMAYDA du 10/10/2026).
- Une banque n'est calculée pour le préfinancement, les droits constatés ou le factoring que si l'un de ses marchés a
  une ligne active **et** au moins une opération payée du type (V12 et calculateurs).
