# Chantier — Agios unifiés (calculé vs facturé)

> Plan de travail du module Agios. Écrit le 09/10/2026 pour une **session dédiée** : ce fichier doit suffire
> pour reprendre sans l'historique de la session qui l'a produit. Les décisions déjà prises par HMAYDA sont
> marquées **[DÉCIDÉ]**, celles qui restent à trancher **[À TRANCHER]**. Aucune modification de code ni de
> base n'a encore été faite pour ce chantier.

---

# 1. Objectif

- Remplacer la saisie manuelle des agios et la clôture de période par **un seul objet métier** portant un
  **montant calculé** et un **montant facturé** — pour mesurer en permanence l'exactitude du calcul. **[DÉCIDÉ]**
- **Tout type d'agios est calculé** ; plus aucune création manuelle à terme. Tout type non couvert
  aujourd'hui doit être couvert, **cas par cas**. **[DÉCIDÉ]**
- Le **calcul journalier est conservé** (`agios_journalier`) partout où il s'applique. **[DÉCIDÉ]**
- Les **encours** affichent le **cumul calculé à ce jour** ; le **facturé remplace le calculé** dès qu'il est
  saisi (c'est ce que la banque débite réellement). **[DÉCIDÉ]**
- Le montant facturé exact est **précisé à la validation** (rapprochement), en regard du calculé affiché.
  L'**écart** (MAD et %) est recalculé à l'affichage, **jamais enregistré**. **[DÉCIDÉ]**
- Modèle : **option A** (une ligne par banque, type et période, avec calculé + facturé). **[DÉCIDÉ]**
- Les deux lignes `PREVU` (BMCI 300 000, AWB 23 251,30) sont remplacées par le calcul détaillé en **dernière
  étape**. **[DÉCIDÉ]**
- Rétroactif : l'historique n'existe que depuis 2026 ; les trimestres antérieurs ne sont pas calculables. **[DÉCIDÉ]**

---

# 2. État des lieux (dump du 09/10/2026)

## 2.1 Données

| Table | Lignes | Rôle |
|---|---|---|
| `agios` | 77 (75 `PAYE`, 2 `PREVU`) — 4 992 792,83 MAD | saisie manuelle des agios réels de la banque |
| `agios_trimestriel` | 50 | une période calculée (type × banque × trimestre/mois), statut `OUVERT`/`CLOTURE` |
| `agios_journalier` | 5 626 | calcul jour par jour (type, banque, marché, base, taux, HT, TVA, TTC) |

- 75 transactions de rapprochement ont pour source `AGIOS` (`transaction.referent` = `agios.id`).
- Types calculés : `FACILITE_CAISSE`, `PREFINANCEMENT`, `DROIT_CONSTATE` (trimestriels), `FACTORING` (mensuel).
  TVA fixe de 10 %, base 360 jours. Facilité de caisse : solde du rapprochement ; préfinancement/DC : encours
  par marché reconstitué depuis les opérations `PAYE`.
- Aucun calcul pour : escompte LCN, frais fixes, frais d'engagement factoring, T4/2025, préfinancement et DC du
  T1/2026, factoring BMCI hors mars et septembre.

## 2.2 Ce que le calcul retrouve ou pas (voir `sql/analyse_agios/`)

- **Colle à la banque (± centimes)** : facilité de caisse BMCI T1/T2, CIH T1–T3, SAHAM T1–T3, CDM T2 ;
  préfinancement BMCE T2, CDM T2, SAHAM T3, CIH T3 (somme de deux lignes : 100 524,48 + 1 502,81).
- **S'écarte** : facilité de caisse AWB (T1 122 647 / 118 207 ; T2 74 392 / 71 696), CDM T1 (20 174 / 19 941),
  factoring BMCI mars (211 825,80 / 208 449,22, période restée `OUVERT`).
- **Agrégé côté banque** : BP, une seule ligne « intérêts débiteurs » (79 675 T1 ; 106 627 T2).
- **Sans équivalent calculé** : 8 lignes escompte LCN (une par remise, pas par période), petits frais fixes
  (AWB 110,00 ×8, 718,98, 678,27 ; SAHAM 6,41 ; CDM 5,34), droits constatés (aucun total ne retrouve une ligne).
- Plusieurs lignes bancaires par (banque, type, période) existent aujourd'hui : CIH T3 (préfinancement ×2),
  AWB T1/T2/T3 (4 à 7 lignes), CIH T1 (4), CDM T1 (4).

## 2.3 Anomalies de données à traiter avant migration

1. La transaction **63** (AWB, 85 784,02, rapprochement 2) référence l'agios **4** (SAHAM) au lieu de l'agios
   **16** (AWB « INTERET DEBITEURS », `PAYE` sans transaction). Correction proposée : un `UPDATE` du `referent`
   de la transaction 63 vers 16 (montants/soldes inchangés). **[À TRANCHER : accord explicite]**
2. Les **13 périodes clôturées** pointent vers un `agios_definitif_id` inexistant (agios définitifs supprimés et
   remplacés par les lignes réelles). Recommandation : ignorer à la migration. **[À TRANCHER]**

## 2.4 Code existant (module `Agios/`)

- **Backend** : entités `Agios`, `AgiosTrimestriel`, `AgiosJournalier` ; calculateurs
  `FcAgiosCalculateur`, `PrefinancementAgiosCalculateur`, `DroitConstateAgiosCalculateur`,
  `FactoringAgiosCalculateur` (interfaces `AgiosCalculateur` / `AgiosCalculateurMultiMarche`) ;
  `AgiosOrchestrateurService` (scheduler 02h00 Casablanca, recalcul manuel, clôture, PDF) ; `AgiosService` ;
  validation unifiée : `AgiosValidationService`, `AgiosSyncHandler`, `AgiosReversalHandler`,
  `AgiosBatchValidationHandler` (`TransactionSource.AGIOS`).
- **Endpoints** : `/agios` (CRUD, recherche, PDF, `toggle-status` et `DELETE` réservés `SYSTEM_ADMIN`) et
  `/agios/calcul` (types, trimestres, détail, agrégats, marchés, PDF, recalculer, clôturer) — `FINANCE_OR_ADMIN`.
- **Alimentent `agios` aujourd'hui (4 chemins)** : saisie manuelle (`AgiosController.createAgios`) ; clôture
  (`AgiosOrchestrateurService.cloturerTrimestre`) ; **`ChargesFixesScheduler`** (dernier jour du mois 07h00,
  ligne `PREVU` pour chaque charge fixe de type `AGIOS` — une seule configuration : BMCI « AGIOS UTILISATION
  LIGNE ASMN FACTORING », 300 000,00) ; modification de statut/dates par les services de validation.
- **Frontend** : `portail/agios` (liste, formulaire) ; `portail/agios-calcul` (dashboard, détail) ;
  `portail/etatsDesEncours/dtos/agios` ; `admin/charges-fixes-config` (type `AGIOS`) ; badges et routes
  (`TransactionSource.ts`, sidebar, `portail.routes.ts`, recapitulatif).
- **Vue SQL** `sql/v_recapitulatif_encours.sql` : branche 14 (`AGIOS`) lit la table `agios`.
- **Docs à mettre à jour** : `docs/modules` (chapitre 23 — Agios), `docs/direction`, `docs/guide-utilisateur`
  (admin.md/finance.md), `docs/rbac` (chapitre 20 — Agios), `conventions/` si un pattern change.
- Aucun test automatisé du module (seul `GBanqueApplicationTests` existe) : la recette repose sur la matrice
  calculé/facturé (§5, étape 1).

---

# 3. Modèle cible (à valider avant tout code)

Une seule table métier `agios`, une ligne par **(banque, type_agios, période)** :

| Champ | Rôle |
|---|---|
| `banque_id`, `type_agios`, `periode` (trimestre 1–4 ou mois 1–12), `annee`, `date_debut`, `date_fin` | clé fonctionnelle (unique) |
| `montant_calcule_ht`, `montant_calcule_tva`, `montant_calcule_ttc` | cumul des lignes journalières jusqu'à aujourd'hui (ou jusqu'à `date_fin`) |
| `nb_jours_calcules`, `nb_jours_total`, `date_dernier_calcul` | suivi du calcul |
| `montant_facture` (nullable) | montant exact débité par la banque, saisi **à la validation** |
| `statut` | voir cycle de vie ci-dessous |
| `date_operation`, `date_valeur`, `libelle`, `observation` | alimentés par la validation (comme les autres sources) |

- **Écart** (MAD et %) : calculé à l'affichage, jamais stocké.
- **Montant retenu pour les encours** : `montant_facture` s'il existe, sinon `montant_calcule_ttc`.
- **Cycle de vie proposé** : `OUVERT` (période en cours, cumul recalculé chaque nuit) → `CLOTURE` (période
  terminée, calcul figé, prêt à valider) → `PAYE` (validé dans un rapprochement avec le montant facturé). Le
  recalcul d'une période reste possible tant qu'elle n'est pas `CLOTURE`. **[À CONFIRMER]**
- `agios_journalier` : conservé ; son lien `trimestre_id` est remplacé par le lien vers la nouvelle ligne.
- `transaction.referent` continue de pointer sur `agios.id` (source `AGIOS`) — donc **ids conservés** ou
  remappés dans le script de migration.

**Points ouverts du modèle :**

- **[À TRANCHER] Plusieurs débits pour un même (banque, type, période)** : le modèle impose un facturé par
  ligne. Les cas existants (§2.2) se règlent en **définissant assez de types** (ex. `FRAIS_FIXES`,
  `ESCOMPTE_LCN`) pour qu'un type n'apparaisse qu'une fois par période ; sinon prévoir plusieurs
  transactions pour une même ligne (relation n–1). À fixer lors du typage des 77 lignes.
- **[À TRANCHER] Escompte LCN** : la banque débite **par remise** (une ligne par LCN), pas par période. Le
  grain de ce type est probablement « un LCN escompté », pas « un trimestre ». Règle de calcul à découvrir.
- **[À TRANCHER] Granularité marché** : le calcul préfinancement/DC reste détaillé par marché dans
  `agios_journalier` ; la ligne `agios` est agrégée par banque.
- **[À TRANCHER] Type des lignes historiques non classables** (46 « arrêtés de trimestre ») : type
  « historique » dédié ou classement manuel par Finance.

---

# 4. Questions restant à poser à Finance

| # | Sujet | État |
|---|---|---|
| 4 | Règle de calcul de l'escompte LCN (jours remise → échéance, taux, commission, TVA, où vit le taux) | à découvrir le 10/10 |
| 5 | Petits frais fixes : montant, périodicité, par compte ou par banque ? (AWB 110,00, 718,98, 678,27 ; SAHAM 6,41 ; CDM 5,34) | aucune idée à ce jour |
| 3 | Droits constatés : facturés ? dans quelle ligne ? | règle de calcul existante, correspondance bancaire non établie |
| 1 | BP : ventilation de la ligne « intérêts débiteurs » agrégée par type | à demander |
| 6 | Seuil d'écart acceptable (MAD / %) et qui l'analyse | à demander |
| — | AWB facilité de caisse : cause de l'écart (taux ? date de valeur ?) | à investiguer |

---

# 5. Plan de travail

## Étape 0 — Cadrage (10/10, avec Finance)
Obtenir les réponses du §4, le **catalogue des types d'agios par banque** (fréquence mensuelle/trimestrielle) et
la règle de chaque type. Livrable : tableau banque × type × fréquence × règle.

## Étape 1 — Préparation des données (aucun code applicatif)
1. Rejouer l'analyse (`sql/analyse_agios/README.md`) sur la copie de base la plus récente.
2. Typer avec Finance les 77 lignes → **(banque, type, période)** ; produire la **matrice banque × type × période**
   (✔ couvert, ◐ écart, ✘ non calculé). Elle devient le **jeu de recette** de tout le chantier.
3. Corriger les anomalies §2.3 (scripts SQL transactionnels, test à blanc sur copie, garde-fous `WHERE`,
   exécution **en une seule requête** dans phpMyAdmin, sauvegarde préalable — procédure utilisée pour les
   corrections du 09/10/2026).

## Étape 2 — Couvrir les types manquants, un par un
Chacun = un nouveau `AgiosCalculateur` (ou une évolution) + une valeur de `TypeAgios` + règle validée par
Finance + comparaison avec la matrice. Ordre par enjeu financier :

1. **Factoring** (2,28 M MAD) : BMCI en mensuel rétroactif sur 2026 (janvier→septembre), frais d'engagement AWB.
   Clôturer proprement mars (`OUVERT` aujourd'hui).
2. **BP** : comprendre la ligne agrégée (§4.1) ; adapter les types ou le rapprochement en conséquence.
3. **AWB facilité de caisse** : expliquer l'écart (T1 3,6 %, T2 3,8 %).
4. **Droits constatés** : établir s'ils sont facturés et où.
5. **Escompte LCN** : nouveau calculateur (grain : remise), selon la règle du 10/10.
6. **Frais fixes** : paramétrage par banque/période (remplace la configuration `AGIOS` des charges fixes).
7. **Rétroactif 2026** : recalculer ce que les données permettent (préfinancement/DC T1/2026) ; T4/2025 non calculable.

## Étape 3 — Modèle unifié (après validation des points ouverts §3)
1. Validation d'architecture (CLAUDE.md : **attendre l'accord** avant de modifier le modèle).
2. Script de migration versionné dans `sql/` : création de la table unifiée, reprise des 77 lignes (ids
   conservés pour `transaction.referent`), reprise des périodes calculées, rattachement des lignes journalières,
   suppression des anciennes tables **après** recette.
3. Backend : entité, repository, spécifications, DTO, mapper, service ; adaptation de l'orchestrateur
   (clôture ne crée plus d'agios), de la validation unifiée (le montant facturé est saisi à la validation, le
   calculé affiché), des handlers de synchro/annulation/lot, de la vue `v_recapitulatif_encours` (branche 14),
   des PDF/Excel, des métriques, de l'audit.
4. Retrait de la **création manuelle** (`POST /agios`) et du type `AGIOS` dans les charges fixes
   (`ChargesFixesScheduler`, écran de configuration) — **en dernière étape**, avec le remplacement des deux
   lignes `PREVU`.
5. Frontend : un écran unique (liste par période avec calculé, facturé, écart MAD/%), détail journalier
   conservé, formulaire de validation avec calculé affiché et facturé saisi, adaptation des encours.
6. RBAC : revoir les `@PreAuthorize` et les guards ; mettre à jour `docs/rbac` dans le même changement.

## Étape 4 — Temps réel dans les encours
Les encours lisent le montant retenu (§3). Le cumul calculé est rafraîchi par le scheduler nocturne ; décider si
un recalcul à la demande (ou un recalcul à chaque opération sur l'encours d'un marché) est nécessaire pour un
affichage « temps réel » au sens strict. **[À TRANCHER avec Finance]**

## Étape 5 — Recette et bascule
- Comparer calculé et facturé sur tout l'historique 2026 migré, banque par banque, avec le seuil de l'étape 0.
- Vérifier les soldes de rapprochement et les encours avant/après migration (aucun écart toléré).
- Documentation : `docs/modules` (ch. 23), `docs/direction`, `docs/guide-utilisateur`, `docs/rbac` (ch. 20),
  conventions, `TODO.md` — **tâche non terminée tant que les quatre niveaux ne sont pas synchronisés**.
- Supprimer le dead code (anciens DTO/endpoints/écrans) et lancer `graphify update .`.

---

# 6. Risques

- **Liens transaction ↔ agios** (75 transactions, rapprochements définitivement clôturés) : toute migration doit
  conserver ces liens ; ne jamais modifier une transaction d'un rapprochement `CLOTURE_DEFINITIVE` par
  l'application (verrouillée), uniquement par SQL contrôlé.
- **Lignes bancaires multiples** par type/période : peut forcer plusieurs types ou une relation n–1.
- **Écarts persistants** (AWB, BP) : le calcul ne sera jamais « juste » tant que la règle bancaire réelle n'est
  pas comprise ; ne pas masquer l'écart.
- **Absence de tests** sur le module : la matrice de l'étape 1 est la seule référence ; écrire des tests des
  calculateurs au fil de l'eau.
- **Tâche de nuit** : en septembre/octobre 2026 le recalcul n'a pas tourné pour tous les types
  (cause non établie, voir `Backend/logs/` depuis la mise en place du fichier de logs) — à surveiller avant de
  s'appuyer sur un affichage temps réel.

---

# 7. Démarrage de la session dédiée

1. Lire : ce fichier, `CLAUDE.md`, `conventions/architecture.md`, `conventions/design-patterns.md`,
   `conventions/jpa.md`, `conventions/database.md`, `TODO.md` (section « Chantier — Agios unifiés »).
2. Obtenir la base à jour (dump) et la charger dans un schéma d'analyse **séparé** ; rejouer
   `sql/analyse_agios/`.
3. Reprendre au point : décisions du 10/10 avec Finance (§4) → étape 1 → étape 2.
4. `graphify query` avant toute lecture de code ; `graphify update .` après modification.
