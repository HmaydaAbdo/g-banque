# Refactoring — Écran de validation unifié (encaissement/décaissement)

> Fichier de suivi de ce chantier. Complète `TODO.md` (qui reste le suivi général du projet) —
> celui-ci est dédié au refactoring de l'écran de validation et au chantier repo unique / GitHub
> qui le précède. Mis à jour au fil des sessions, dans le même esprit que `TODO.md`.

---

## Objectif métier

Aujourd'hui, chaque source (Virement, Chèque, LCN, Agios, …) a son propre écran de validation,
dupliqué en décaissement et encaissement quand les deux sens existent (~30 écrans Angular, 22
endpoints REST unitaires + `POST /rapprochement/process/lot`).

Cible discutée le 12/09/2026 : un écran de récapitulatif unique pour la validation, quel que soit
le type et le sens de l'opération. Le formulaire d'édition (TVA, dates, libellé…) et l'action de
validation se déclenchent via l'`id` de la source sélectionnée dans le récap ; deux boutons
contextuels (Encaisser / Décaisser) apparaissent seulement quand le sens est réellement un choix
utilisateur (voir `sensPolicy` par source ci-dessous — pour la majorité des sources le sens est
fixe ou porté par la donnée, pas un choix).

**Contrainte non négociable :** l'application est en production. Le modèle de données
(`Transaction`, tables des 20+ sources) ne doit pas être modifié, sauf nécessité identifiée et
validée explicitement avec HMAYDA avant toute migration. Objectif : ramener la base à un état
stable *après* tout le refactoring, pas la faire évoluer en continu pendant.

---

## Chantier préalable : repo unique + migration GitLab → GitHub — ✅ TERMINÉ (14/09/2026)

Décidé le 14/09/2026 : ce chantier passe **avant** le début du refactoring applicatif. Terminé le
même jour.

### Résultat

- **Dépôt unifié** : https://github.com/HmaydaAbdo/g-banque (privé), branche `main`.
- **Historique intégralement conservé** via `git subtree` — 444 commits Backend + 291 commits
  Frontend fusionnés sous les préfixes `Backend/` et `Frontend/`, rattachés à l'historique
  préexistant du dépôt racine (docs/conventions/sql/infra, initialisé le 07/09/2026). Aucun
  squash, aucune réécriture de hash — `git log` du dépôt unifié remonte jusqu'au tout premier
  commit de chaque projet.
- **Aucun secret** (`.env`, `env.dev`) trouvé dans l'historique des 3 dépôts avant push (vérifié
  via `git rev-list --objects --all`).
- **Bug corrigé** : `graphify-out/` était suivi par erreur dans Backend et Frontend (règle
  `.gitignore` `src/graphify-out/` qui ne matchait jamais le dossier, réellement à la racine de
  chaque projet). 848 fichiers retirés du suivi (historique existant non réécrit, juste arrêt du
  suivi). Remplacé par une règle unique `/graphify-out/` à la racine du dépôt unifié — un seul
  graphe pour tout le monorepo, à régénérer via `graphify update .` depuis la racine, jamais
  commité.
- **`.gitignore` des 3 emplacements consolidé** : suppression de la règle "dépôts imbriqués"
  (obsolète, Backend/ et Frontend/ sont maintenant de vrais sous-dossiers suivis).
- **Vérification post-fusion réussie** : `mvnw.cmd compile` (Backend) et `npm ci && ng build`
  (Frontend) fonctionnent tous les deux sans erreur depuis le dépôt fusionné — seuls des warnings
  préexistants sans rapport (budgets de taille de bundle, vulnérabilités npm déjà présentes avant
  la fusion, dépréciations de sous-dépendances).

### Étapes (toutes cochées)

- [x] Sauvegarde complète de `G-Banque/` avant toute opération (`G-Banque-BACKUP-20260914`)
- [x] Inspection en lecture seule des 3 dépôts (remotes, branche par défaut `main` partout,
      aucun changement non commité perdu — Backend et Frontend étaient propres et à jour)
- [x] Confirmation du dépôt GitHub cible (`HmaydaAbdo/g-banque`, privé, créé via l'UI web)
- [x] Fusion `Backend/` dans le dépôt racine via `git subtree` (444 commits)
- [x] Fusion `Frontend/` dans le dépôt racine via `git subtree` (291 commits)
- [x] Retrait de `graphify-out/` (suivi par erreur) + nouveau `.gitignore` consolidé
- [x] Vérification qu'aucun secret n'a jamais été commité dans l'historique des 3 dépôts
- [x] Création du dépôt GitHub et push de `main` (historique complet, 25423 objets)
- [x] Vérification post-fusion : build Backend (`mvn`) et Frontend (`ng build`) réussis depuis
      le dépôt unifié
- [ ] Retrait des anciens dépôts GitLab / anciens `.git` locaux — **volontairement pas encore
      fait**, gardés en sécurité (`Backend-old-standalone-repo/`, `Frontend-old-standalone-repo/`
      à côté de `G-Banque/`, + backup complet `G-Banque-BACKUP-20260914`). À faire seulement
      quand HMAYDA confirme que tout est stable après un usage réel du dépôt fusionné.

### Reste en suspens (mineur, non bloquant)

- Les branches `feat/recap-provisions-ui` (Backend et Frontend) et `claude/admiring-murdock`
  (Frontend, utilisée dans un git worktree local) n'ont pas été recréées dans le dépôt unifié —
  seule `main` a été fusionnée. Leur historique reste présent dans les objets git locaux (remotes
  `backend-local`/`frontend-local`) et dans les backups, donc rien n'est perdu ; à recréer comme
  vraies branches du dépôt unifié si besoin plus tard.
- Remotes locaux `backend-local`/`frontend-local` (pointant vers les anciens dossiers) encore
  présents dans le dépôt racine — à nettoyer (`git remote remove`) une fois les anciens dépôts
  définitivement retirés.

---

## Chantier applicatif : validation unifiée

### Décisions arbitrées

- **14/09/2026 — Option A retenue** (parmi 2-3 options proposées) : le récap se lit via l'endpoint
  `/recapitulatif` existant (déjà quasi complet côté lecture — vue `v_recapitulatif_encours` +
  `RecapitulatifController`) ; les 22 endpoints unitaires de `ValidationController` restent
  inchangés ; l'unification se fait uniquement côté frontend, au niveau sélection/UX de l'écran
  Récapitulatif. Aucune migration du modèle de données.
- **14/09/2026** — Ordre d'attaque : backend d'abord (le strict nécessaire), puis frontend.
  Exigence explicite : toute fonctionnalité offerte aujourd'hui par les écrans de validation
  séparés doit rester disponible depuis le futur écran de récap unique, qui doit à terme être le
  seul écran de validation.
- **14/09/2026** — Liberté accordée de modifier le code/les vues existants (pas seulement
  additif), du moment que les conventions Spring/Angular du projet sont respectées.
- **14/09/2026 — TVA Leasing** : reste verrouillée à 20 % (non éditable) dans le formulaire de
  validation unifié, comme sur l'écran dédié actuel — décision explicite de HMAYDA contre ma
  recommandation d'ouvrir la saisie libre. **⚠️ Réversée le 14/09/2026 (session CMT), voir
  ci-dessous.**
- **14/09/2026 (session CMT) — TVA Leasing rouverte** : HMAYDA revient sur la décision ci-dessus —
  le champ TVA du formulaire de validation unifié Leasing devient **éditable** : 20 % par défaut,
  mais l'utilisateur peut choisir « aucun » (0 %, via le dropdown des taux) ou saisir un montant
  libre. Décision explicite, verbatim : « le choix 1 et aussi pour leasing aulieu d etre 0 par
  defaut, il doit etre 20% mais aussi la possibilite de choisir aucun et saisir montant ».
- **14/09/2026 (session CMT) — TVA CMT** : saisie libre/optionnelle (taux OU montant, défaut 0 %),
  comme sur l'écran dédié CMT actuel — confirmé par lecture de `CmtValidationService.java` et
  `CmtTransactionRequestDto.isSaisieTvaCoherente()` avant de proposer l'option à HMAYDA (règle non
  négociable : ne jamais supposer une règle métier).
- **14/09/2026 (session CMT) — `IEcheanceCMTService` ajoutée** : à la demande explicite de HMAYDA
  (répond au point d'ambiguïté laissé en suspens dans la note de session précédente), pour la
  cohérence avec le patron Leasing (`IEcheanceLeasingService`). `CreditMoyenTermeService` bascule
  également sur l'interface plutôt que la classe concrète — même patron que
  `ContratLeasingServiceImpl` → `IEcheanceLeasingService`, constaté par lecture du code Leasing.
- **14/09/2026 (session CMT, après coup) — Un seul dialogue de validation unitaire, pas un par
  source** : question posée par HMAYDA après avoir vu deux `<p-dialog>` quasi identiques
  (Leasing + CMT). Proposé 3 options (fusionner en un dialogue générique piloté par config ;
  factoriser seulement le template HTML ; garder séparé, par cohérence avec le choix backend
  d'un `*ValidationService` par source) — HMAYDA retient la fusion complète. Le récap Leasing du
  pilote initial est donc lui-même généralisé par cette session (voir Journal, suite 4) ; le choix
  backend (services séparés par source) reste inchangé — c'est une décision de bean Spring/cycle
  d'injection, sans rapport avec la duplication de formulaire côté frontend.
- **15/09/2026 — Aller plus loin côté backend : mesure du risque, puis choix d'un périmètre
  restreint et additif.** HMAYDA demande si le backend peut être unifié de la même façon (un seul
  dialogue + un seul service de validation pour toutes les sources). Lecture de
  `ValidationController.java` (31 Ko, 22 endpoints) et surtout `BatchValidationService.java`
  (switch de 500 lignes sur `TransactionSource`) avant de répondre — constats : (1) ce switch EST
  déjà l'abstraction partagée du backend, juste sous forme procédurale plutôt qu'un registre ;
  (2) les 22 DTOs de requête n'ont aucune forme commune (constructeurs différents, `sens` requis
  seulement pour 4 sources, `banqueId` requis pour 4 autres, CMT/Leasing sont les seuls à résoudre
  une entité parente) ; (3) **la TVA en saisie libre (`montantTva`) n'existe que sur les DTOs
  Leasing et CMT** — les 20 autres n'ont qu'un `tauxTva`. Sur cette base, 3 options proposées
  (tout laisser tel quel ; extraction incrémentale d'une interface partagée, Leasing/CMT
  d'abord ; fusion complète immédiate en un endpoint/DTO générique) — HMAYDA choisit
  l'extraction incrémentale, puis précise vouloir aussi le câblage réel (pas juste
  l'interface sans l'utiliser) pour ces deux sources.
- **19/09/2026 (pilote Chèque) — Champ « Date d'émission » verrouillé, comme Leasing/CMT** :
  proposé 2 options (verrouiller comme Leasing/CMT, recommandation contraire = ajouter un flag par
  source pour garder le champ éditable comme sur les 2 écrans dédiés Chèque actuels) — HMAYDA choisit
  de verrouiller, contre ma recommandation. Le champ « Date d'émission » du dialogue unitaire reste
  donc désactivé pour Chèque aussi, prérempli avec la vraie `dateEmission` du chèque (via
  `UnitValidationDetail.dateEcheance`, nom générique du champ de référence). Changement de
  comportement assumé par rapport aux 2 écrans dédiés (qui laissaient ce champ éditable) : la
  date d'émission d'un chèque n'est plus modifiable au moment de la validation depuis le récap.
- **19/09/2026 (pilote Chèque) — Documentation non mise à jour, cohérence avec le précédent** :
  proposé 2 options (rester cohérent avec Leasing/CMT qui n'ont pas mis à jour `docs/modules`,
  `docs/direction`, `docs/guide-utilisateur`, `docs/rbac` ; ou appliquer la règle CLAUDE.md dès ce
  pilote) — HMAYDA choisit de rester cohérent avec le précédent. Écart connu à rattraper plus tard
  en une fois pour toutes les sources migrées, pas source par source.
- **19/09/2026 (pilote LCN/Virement) — Champ « Date d'émission » verrouillé aussi, cohérence avec
  Chèque** : même situation que Chèque (les 4 écrans dédiés LCN/Virement ont ce champ éditable —
  LCN le préremplit même avec `dateEcheance`, pas la vraie `dateEmission`) — HMAYDA confirme le même
  choix que pour Chèque : verrouiller, sans ajouter de flag par source. Décision de politique
  générale actée : ce champ reste verrouillé pour toute nouvelle source migrée, sauf demande
  contraire explicite.
- **14/09/2026 — Vue `v_recapitulatif_encours` non modifiée** : malgré la liberté accordée
  ci-dessus, gardée intacte pour préserver son contrat de colonnes documenté ; le détail
  nécessaire à la validation (ex. `contratLeasingId` pour Leasing) est résolu via un endpoint GET
  dédié par source plutôt qu'en enrichissant la vue partagée.
- **14/09/2026 — Portée du pilote Leasing** : le bouton « Valider » ligne par ligne n'apparaît que
  sur les lignes Leasing dans le récap (les 23 autres sources gardent leur écran dédié comme seul
  moyen de validation unitaire, jusqu'à leur propre migration). En revanche, « Valider en lot »
  fonctionne dès maintenant pour **toutes** les sources : `POST /rapprochement/process/lot` existait
  déjà par source (utilisé par chaque écran séparé) — le frontend du récap découpe simplement une
  sélection multi-sources par (source, sens) et enchaîne un appel par groupe.
- **19/09/2026 — Décision d'architecture différée puis traitée le jour même : un seul
  endpoint/service de validation par source à deux sens.** ✅ **Résolu (suite 8)** — voir Journal.
  HMAYDA avait initialement différé la question (« par la suite on pensera »), non traitée pendant
  le lot suite 7. Rouverte plus tard dans la même journée : options soumises via `AskUserQuestion`
  (une seule méthode sans paramètre sens vs. une seule méthode avec sens explicite en interne) —
  HMAYDA retient la première (recommandée), et confirme l'extension du périmètre à LCN (5 sources
  au total, pas seulement les 4 initialement citées).
- **19/09/2026 — Fusion complète validation unitaire + lot, mandat élargi explicite.** ✅ **Résolu
  (suite 9)** — voir Journal. Après suite 8, HMAYDA pose une question exploratoire (« is there
  smthng we can do better to better organize the front and back... like what other finance system
  do »). Recommandation donnée en 3 phrases (étendre `BatchValidationHandler` à la validation
  unitaire, ~23 endpoints/DTOs/méthodes frontend vers 1). HMAYDA répond en écartant explicitement
  toute nouvelle question d'arbitrage, verbatim : « dont bother ur self to the amount of work we
  should do, just pick the best one wich will allows us to minimize amount of code and make the
  system more clean and organized and avoiding redundancy in logique, let's mimic what banking and
  finance system do and start doing thing the right way, tell claud md this as a rule ». Traduit
  en mandat : (1) une seule architecture choisie, sans nouvelle question ; (2) exécution immédiate,
  pas seulement une proposition ; (3) la règle inscrite dans la documentation d'architecture du
  projet — voir `conventions/architecture.md`, Règle 1 (réécrite intégralement).

### Source pilote

**Leasing**, choisi le 14/09/2026 — sens fixe (toujours décaissement), un seul écran existant
(`echeances-leasing.component.ts`), bon candidat pour poser le patron avant les sources à sens
porté par la ligne (Préfinancement/Droit constaté/Factoring).

### Tableau des 24 sources — statut

| Source | Sens | Politique de sens | Endpoint(s) actuel(s) | Écran(s) actuel(s) | Statut |
|---|---|---|---|---|---|
| Virement | D/E | choix utilisateur | `process/virement` (endpoint unique depuis le 19/09/2026, ex `{decaissement,encaissement}/virement`) | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Chèque | D/E | choix utilisateur | `process/cheque` (endpoint unique depuis le 19/09/2026, ex `{decaissement,encaissement}/cheque`) | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| LCN | D/E (+ nature ENCAISSEMENT/ESCOMPTE) | choix utilisateur | `process/lcn` (endpoint unique depuis le 19/09/2026, ex `{decaissement,encaissement}/lcn`) | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Mise à disposition | D/E | choix utilisateur | `process/mise-a-disposition` (endpoint unique depuis le 19/09/2026, ex `{decaissement,encaissement}/mise-a-disposition`) | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Virement international | D/E | choix utilisateur | `process/virement-international` (endpoint unique depuis le 19/09/2026, ex `{decaissement,encaissement}/virement-international`) | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Préfinancement | D/E | porté par la ligne (`Prefinancement.type`) | `process/prefinancement` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Droit constaté | D/E | porté par la ligne (`DroitConstate.type`) | `process/droit-constate` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Factoring | D/E | porté par la ligne (`Factoring.type`) | `process/factoring` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Provision | D/E | porté par l'enregistrement (`Provision.sens`) | `process/provision` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Impayé | D/E | porté par l'enregistrement (`Impayee.type`) | `process/impayee` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + anciens écrans supprimés (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| CMT (échéance) | D | fixe | `process/echeance-cmt` + `GET /echeances-cmt/{id}` | 1 (ancien écran encore présent) | **Récap opérationnel (backend + frontend, 14/09/2026) — en attente de validation HMAYDA (build + test manuel) avant retrait de l'ancien écran** |
| Leasing (échéance) | D | fixe | `process/echeance-leasing` | 0 (récap uniquement) | **Pilote fait + ancien écran supprimé (14/09/2026)** |
| Agios | D | fixe | `process/agios` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Commission | D | fixe | `process/commission` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Frais bancaire | D | fixe | `process/frais-bancaire` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Impôt / Taxe | D | fixe | `process/impot-taxe` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Prélèvement | D | fixe | `process/prelevement` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Opération erronée | D | fixe | `process/operation-errone` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Décomptes | E | fixe | `process/decompte` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |
| Restitution | E | fixe | `process/restitution` | 0 (récap uniquement) | **Récap opérationnel (backend + frontend) + ancien écran supprimé (19/09/2026) — en attente de validation HMAYDA (build + test manuel)** |

*Sens réel de Préfinancement/DroitConstaté/Factoring confirmé le 19/09/2026 par lecture du code
(voir Journal) : porté par le champ `Type` (REMBOURSEMENT/DEBLOCAGE) de chaque entité, jamais un
choix transmis à la validation — un seul endpoint/une seule méthode de service par source, le
mapper décide du débit/crédit à partir de ce champ.*

**19/09/2026 (suite 9) — Colonne « Endpoint(s) actuel(s) » obsolète pour les 24 sources.** Tous les
endpoints listés ci-dessus (`process/virement`, `process/cheque`, `process/echeance-cmt`,
`process/echeance-leasing`, etc.) sont supprimés et remplacés par le seul
`POST /rapprochement/process` (`ids` à un élément pour une opération unique, à plusieurs pour un
lot — voir suite 9 ci-dessous et `conventions/architecture.md`, Règle 1). Tableau conservé tel
quel pour l'historique (sens/politique de sens toujours d'actualité), colonne Endpoint non
réécrite ligne par ligne pour éviter la répétition — cette note fait foi.

---

## Journal de session

**20/09/2026 (suite 11) — Édition d'une transaction déjà validée : mêmes champs que la validation,
synchronisation garantie avec l'entité source.** HMAYDA demande d'gérer proprement l'édition d'une
transaction déjà validée (existait déjà via `PATCH /transactions/{id}`, mais limitée à
dates+TVA brute, sans synchronisation du libellé/observation ni recalcul de la TVA). Questions
posées avant tout code (CLAUDE.md — jamais de règle métier supposée) :
1. Quels champs éditer ? → Réponse : reproduire le même dialogue de validation, seulement les
   champs qui y sont réellement éditables — explicitement **pas** la banque.
2. Édition depuis la transaction, depuis l'entité source, ou les deux ? → Réponse : les deux.
3. Une modification de montant doit-elle redéclencher le recalcul de la cascade des soldes ? →
   Réponse : oui, par principe (« le système doit toujours être synchronisé, pas de décalage, pas
   de données corrompues »).
- **Investigation avant conception** : lecture de l'entité `Cheque` (aucun champ TVA — le montant
  est fixe, la TVA n'existe que sur `Transaction`) et de `ChequeTransactionMapper` (débit/crédit =
  montant de l'entité, TVA posée à part) — a confirmé que le champ Q3 imaginé (« modifier le
  montant ») n'existe en réalité pas dans le dialogue de validation reproduit : seuls
  dateValeur/dateOperation/libellé/observation/TVA y sont éditables, jamais le montant. Recalcul de
  cascade des soldes donc **non nécessaire** pour ce périmètre (le débit/crédit ne change jamais) —
  signalé explicitement à HMAYDA avant d'implémenter, pas juste supposé.
- **Bug latent trouvé et corrigé au passage** : l'ancien `PATCH` acceptait `tvaDeductible` et
  `tvaCollecte` comme deux champs bruts indépendants, sans lien avec le sens réel de la transaction
  ni avec son montant — rien n'empêchait d'écraser les deux à la fois, ou de dépasser le montant
  réel. Remplacé par la même politique que partout ailleurs (`tauxTva`/`montantTva` mutuellement
  exclusifs, résolus par `TvaResolver` contre le montant TTC réel de la transaction, posés sur le
  seul côté — déductible ou collecté — que porte réellement cette transaction).
- **Backend** : `TransactionUpdateRequestDto` réécrit (dateValeur, dateOperation, libelle,
  observation, tauxTva, montantTva + `isSaisieTvaCoherente()`). Les 20 `syncXxxFromTransaction`
  (interfaces + implémentations) étendus avec `libelle`/`observation`. Troisième registre de
  handlers créé, `TransactionSyncHandler` (miroir exact de `BatchValidationHandler`/
  `TransactionReversalHandler`), 22 implémentations `XxxSyncHandler` (une par source, LCN×3
  partageant le même appel de service, Agios délégant à son service existant). L'if-chain de 20
  branches dans `TransactionServiceImpl.updateTransactionById` supprimé au profit d'un lookup dans
  le registre ; les ~20 `IXxxValidationService` injectés directement dans le constructeur
  supprimés (remplacés par `List<TransactionSyncHandler>`).
- **Frontend** : les deux dialogues d'édition existants (`rapprochemnt-encours.component.ts` et
  `details-rapprochement.component.ts` — un dans le récap En cours, l'autre dans le détail/
  historique de rapprochement, exactement les « deux entrées » demandées) reconstruits pour
  reproduire le formulaire du dialogue de validation générique du récap : `<app-tva-input>`
  (composant déjà partagé, réutilisé tel quel), libellé, observation, dateValeur, dateOperation,
  dateEmission verrouillée à titre informatif — jamais de champ banque. Label du champ TVA
  (« Déductible »/« Collectée ») déterminé dynamiquement selon le sens réel de la transaction
  (`debit > 0`).
- **Vérifié** : `mvnw compile` + `mvnw test-compile` (backend, propres) ; `npx tsc --noEmit`
  (frontend, seulement les 4 erreurs `.spec.ts` préexistantes sans rapport) ; grep de sweep sur les
  deux anciens champs bruts `tvaDeductible`/`tvaCollecte` côté formulaire (aucune référence
  restante).
- **Documentation mise à jour dans le même changement** : `conventions/architecture.md` (Règle 2 —
  nouveau mécanisme de registre, nouvelle section sur les champs synchronisés vs. volontairement
  non éditables, table de cohérence mise à jour) ; `docs/rbac/README.md` (description de l'endpoint
  `PATCH /transactions/{id}` mise à jour, pas de changement d'autorisation).

**20/09/2026 (suite 10) — Même registre appliqué au domaine symétrique : l'annulation.** HMAYDA
confirme la validation manuelle de suite 9, puis pose une question exploratoire : "is there smthng
we can also do to make our system more flexible and extensible with minimal code ... in terms of
validation domaine validate and also reverse." Investigation :
`ReverseTransactionService.resetEntiteSource()` avait exactement le même défaut que
`BatchValidationService` avant sa propre migration — un `switch` de 20 cas sur `TransactionSource`,
une méthode privée `resetXxx` par source, ~20 repositories injectés directement dans le
constructeur. Recommandation donnée en 2-3 phrases (appliquer le même patron
`BatchValidationHandler`, sous le nom `TransactionReversalHandler`) — HMAYDA confirme sans autre
question d'arbitrage (voir [[feedback-gbanque-architecture-rules]] dans la mémoire de session : ce
type de décision — dupliquer un patron déjà validé sur un domaine symétrique — ne repasse plus par
`AskUserQuestion`).
- **`TransactionReversalHandler`** créé dans `validation/services/handlers/` (miroir exact de
  `BatchValidationHandler` : `source()` + `reset(Long referentId)`).
- **22 implémentations** créées, une par source, chacune dans le même package que le
  `XxxBatchValidationHandler` correspondant (même module source, même logique deux fois : valider
  et annuler). Chaque corps reprend à l'identique l'ancienne méthode privée `resetXxx` — aucun
  changement de comportement, seulement de mécanisme. Cas LCN : 3 handlers
  (`LcnDecaissementReversalHandler`/`LcnEncaissementReversalHandler`/`LcnEscompteReversalHandler`,
  même raison que côté validation) partageant leur logique de reset via un petit helper non-Spring
  `LcnResetSupport` plutôt que de dupliquer le corps 3 fois. Cas Agios : suivait déjà le bon patron
  avant cette migration (déléguait à `IAgiosValidationService.resetAgios` plutôt que de toucher un
  repository directement) — `AgiosReversalHandler` se contente de déléguer à ce même service.
- **`ReverseTransactionService` réduit** à `TransactionRepository` + `SoldeCascadeService` +
  `List<TransactionReversalHandler>` (construit en `Map<TransactionSource, TransactionReversalHandler>`
  au constructeur, même détection de doublon que `BatchValidationService`) — les ~20 repositories
  et `IAgiosValidationService` retirés du constructeur, `resetEntiteSource` n'est plus qu'un lookup
  dans le registre. `annulerTransaction()` (guard rapprochement clôturé, suppression, cascade des
  soldes) inchangé.
- **Aucun changement d'endpoint ni d'autorisation** : `DELETE /rapprochement/annuler/{id}` et son
  `@PreAuthorize` de classe restent identiques — `docs/rbac/README.md` n'a donc pas besoin d'être
  retouché pour ce chantier (contrairement à suite 9).
- **`conventions/architecture.md` mis à jour dans le même changement** : nouvelle sous-section
  "Le même patron s'applique à l'annulation" sous la Règle 1, documentant `TransactionReversalHandler`
  comme extension explicite du même principe. Corrigé au passage une référence obsolète à
  `ValidationService.validerEnLot` (renommée `valider` en suite 9, jamais mise à jour dans ce doc).
- **Vérifié** : `mvnw compile` et `mvnw test-compile` sans erreur ; grep de sweep sur tout le repo
  pour d'anciennes références aux méthodes `resetXxx` privées ou à l'ancien constructeur de
  `ReverseTransactionService` (aucune trouvée).

**19/09/2026 (suite 9) — Fusion complète validation unitaire + lot : un seul endpoint pour toute
l'application.** Chantier distinct de suite 8 (celle-ci fusionnait les *méthodes de service* à deux
sens ; celle-ci fusionne la *validation unitaire elle-même dans le mécanisme de lot*). Déclenché par
une question exploratoire de HMAYDA sur l'organisation front/back, suivie d'un mandat explicite
sans plus de question d'arbitrage (voir Décisions arbitrées ci-dessus pour le verbatim complet).
- **Constat de départ** : le dialogue de validation unitaire du récap et `POST /process/lot`
  couvraient déjà, à quelques champs près, exactement le même besoin — une opération unique n'est
  qu'un lot de taille 1. `OperationValideeDto` (réponse du lot) suffisait déjà pour l'unitaire : le
  callback `submitUnitValidation()` du frontend ignorait de toute façon le contenu de la réponse.
  Seuls 3 champs manquaient à `BatchValidationRequestDto` pour couvrir aussi l'unitaire :
  `montantTva` (saisie libre, jamais sensée sur un vrai lot), `observation`, `libelle` (propres à
  une opération). `dateEmission` n'a pas eu besoin d'être ajoutée : le dialogue unitaire la
  verrouille déjà sur l'entité pour toutes les sources (politique actée le 19/09/2026, voir
  Décisions arbitrées plus haut), le fallback existant du mapper sur l'entité produit donc déjà
  le même résultat.
- **Backend** : `BatchValidationRequestDto` étendu (3 champs + `isSaisieTvaCoherente()` corrigée
  moi-même avant tout test — j'avais d'abord laissé `@NotNull` sur `tauxTva`, ce qui aurait cassé
  toute saisie de `montantTva` ; remplacé par le même `@AssertTrue` XOR que tous les DTO unitaires
  historiques). `BatchValidationService.processBatch` refuse désormais les 3 nouveaux champs dès
  que `ids` contient plus d'un élément (`requireSingleOperationForPerOperationFields`). Les 21
  `BatchValidationHandler` existants mis à jour pour transmettre ces 3 champs à leur DTO unitaire
  respectif (record ou setter selon le module, vérifié un par un par lecture du DTO cible — jamais
  supposé). `ValidationController` réduit de ~22 endpoints à 2 : `POST /rapprochement/process`
  (renommé depuis `/process/lot`) et `DELETE /rapprochement/annuler/{id}` (inchangé). Tous les
  champs/services `IXxxValidationService` désormais injectés uniquement via leur
  `BatchValidationHandler`, plus via le controller — vérifié qu'aucun n'est devenu un bean orphelin
  (tous encore utilisés par `TransactionServiceImpl` pour la synchronisation entité↔transaction).
- **Trou d'architecture trouvé et comblé, pas seulement supprimé** : `TransactionSource.LCN_ESCOMPTE`
  n'avait jamais eu de `BatchValidationHandler` (exclu du lot depuis toujours, exception
  volontaire) mais avait bien sa propre validation unitaire via l'ancien endpoint dédié `/process/lcn`
  (indifférent à la nature du LCN). Supprimer cet endpoint sans rien ajouter aurait cassé la
  validation des LCN d'escompte — `LcnEscompteBatchValidationHandler` ajouté pour préserver ce qui
  fonctionnait déjà, pas pour introduire un nouveau comportement.
- **Frontend** : `ValidationService` réduit à 2 méthodes (`valider`, `rollBackTransaction`) — les
  ~20 méthodes `xxxToRapprochement` par source supprimées. `UnitValidationConfig` allégée :
  `buildAndSubmit` (qui construisait toute la requête + appelait le bon endpoint, dupliqué 22 fois)
  remplacé par `toRoutingFields` (ne fournit plus que `transactionSource`/`sens`/`banqueId` — le
  reste de `BatchValidationRequestDto` est désormais construit une seule fois, en commun, par
  `submitUnitValidation()`). `sens` toujours lu sur `d.type` pour Virement/Chèque/MiseADisposition/
  VirementInternational (`requireSensIfNeeded` l'exige encore côté backend pour l'homogénéité de la
  sélection en lot, même si le handler ne le consomme plus). `banqueId` toujours lu sur le détail
  pour Préfinancement/Factoring/DroitConstaté/Décomptes.
- **Vérifié avant de considérer terminé** : `mvnw compile` (backend) et `npx tsc --noEmit`
  (frontend) sans erreur introduite (4 erreurs `tsc` préexistantes, sans rapport, dans des fichiers
  `.spec.ts` jamais touchés) ; grep de sweep sur tout le repo pour d'anciennes références aux
  endpoints/méthodes supprimés (aucune trouvée, hors historique du présent journal et de
  `docs/rbac/README.md` — corrigées).
- **Documentation mise à jour dans le même changement** : `conventions/architecture.md` (Règle 1,
  réécrite intégralement — c'est la règle demandée explicitement par HMAYDA, « tell claud md this
  as a rule »), `docs/rbac/README.md` (toutes les références aux anciens endpoints par source
  corrigées vers le nouveau point d'entrée unique). `docs/modules`, `docs/direction`,
  `docs/guide-utilisateur` restent volontairement non touchés (déférés en bloc, décision HMAYDA du
  19/09/2026, voir Décisions arbitrées).

**19/09/2026 (suite 8) — Fusion des méthodes de validation à deux sens.** Chantier distinct de la
migration récap (celle-ci était déjà terminée, voir suite 7 ci-dessous) : à la demande explicite de
HMAYDA (« let's start two-methods-to-one architecture question »), fusion des services de
validation qui exposaient encore deux méthodes publiques quasi identiques (une par sens) en une
seule par source.
- **Investigation avant tout code** (règle CLAUDE.md — jamais de décision d'architecture sans
  vérifier le code réel) : lecture des 4 services cités par HMAYDA (Virement, Chèque, Virement
  international, MiseADisposition) — confirmé que dans les 4 cas, les deux méthodes publiques
  (`processXxxDecaissementTransaction`/`processXxxEncaissementTransaction`) étaient **déjà
  strictement identiques** : même corps, déléguant toutes deux à la même méthode privée
  `processTransaction`, mêmes annotations `@Transactional`/`@AuditableAction`. Le sens réel
  (débit/crédit, ou banque de prélèvement pour Virement) est décidé par le mapper à partir du champ
  `type` de l'entité elle-même — jamais par la méthode appelée. Vérifié qu'aucun autre appelant
  n'existe que `ValidationController` et le `BatchValidationHandler` de chaque source (tous les
  écrans dédiés historiques sont déjà supprimés, voir suite 7).
- **LCN découverte pendant l'investigation** : même lecture appliquée à `LcnValidationService`,
  qui présentait exactement le même problème (non cité initialement par HMAYDA). Signalé
  explicitement via `AskUserQuestion` plutôt que supposé hors périmètre ou inclus d'office — HMAYDA
  confirme l'inclusion (5 sources au total). Différence pour LCN : le registre
  `BatchValidationHandler` garde ses deux entrées (`LCN_DECAISSEMENT`/`LCN_ENCAISSEMENT` restent
  deux clés de `TransactionSource` distinctes, structurellement différent de `requestDto.sens()`) —
  seule la couche service et le REST fusionnent.
- **Décision de design validée via `AskUserQuestion`** : une seule méthode, **sans paramètre sens**
  (recommandé, retenu) plutôt qu'une méthode avec sens explicite et branchement interne — un if/else
  aurait été de la pure cérémonie puisque les deux branches auraient été rigoureusement identiques
  aujourd'hui.
- **Backend** (5 sources : Virement, Chèque, Virement international, MiseADisposition, LCN) :
  - Chaque `IXxxValidationService`/`XxxValidationService` : les deux méthodes fusionnées en une
    seule (`processChequeTransaction`, `processVirementTransaction`,
    `processVirementInternationalTransaction`, `processMiseAdispositionTransaction`,
    `processLcnTransaction`) — l'ancienne méthode privée `processTransaction` devient directement
    la méthode publique unique, aucune logique modifiée.
  - Chaque `XxxBatchValidationHandler` : suppression du branchement sur `requestDto.sens()`, un
    seul appel de service désormais (LCN garde ses 2 handlers distincts par `TransactionSource`,
    mais chacun appelle la même méthode unique).
  - `ValidationController` : 10 endpoints `/process/{decaissement,encaissement}/{source}` fusionnés
    en 5 endpoints uniques `/process/{source}` (`virement`, `cheque`, `virement-international`,
    `mise-a-disposition`, `lcn`).
  - `BatchValidationService.requireSensIfNeeded` **volontairement inchangé** : `sens` reste exigé
    pour ces 5 sources en lot, car le frontend en a toujours besoin pour grouper la sélection par
    (source, sens) et refuser une sélection hétérogène (règle actée le 19/09/2026, voir plus haut) —
    seul le handler qui consommait `sens` pour choisir une méthode ne le fait plus.
  - Javadoc stale corrigée en passant : `SensOperation.java` affirmait encore que les endpoints
    unitaires « encodent déjà le sens dans l'URL » et que l'enum servait à choisir la méthode
    unitaire — les deux affirmations sont désormais fausses, corrigées.
- **Frontend** : `ValidationService` (10 méthodes → 5 : `chequeToRapprochement`,
  `virementToRapprochement`, `virementInternationalToRapprochement`, `miseAdispoToRapprochement`,
  `lcnToRapprochement`) ; dans `recapitulatif.component.ts`, les 5 ternaires
  `d.type === SensType.DECAISSEMENT ? ... : ...` dans `buildAndSubmit` remplacés par un seul appel
  (le `d.type` utilisé ailleurs pour l'affichage, ex. `recapLabel` de MiseADisposition, reste
  inchangé — c'est un choix d'affichage, pas un choix d'endpoint).
- **Documentation** : `docs/rbac/README.md` mis à jour dans le même changement (contrairement aux
  docs fonctionnelles `docs/modules`/`docs/direction`/`docs/guide-utilisateur`, dont la mise à jour
  reste différée par décision HMAYDA — le RBAC, lui, doit suivre le code dans le même changement
  par règle CLAUDE.md) : 22 endpoints au lieu de 27, table mise à jour, note explicative ajoutée.
- **Balayage final** : recherche exhaustive de tous les anciens noms de méthode/endpoint sur tout
  le dépôt (backend + frontend + specs + tests) — aucune référence résiduelle hors mentions
  historiques volontaires dans les nouveaux Javadocs et ce journal.
- **Prochaine étape** : HMAYDA build/teste manuellement, commit/push.

**19/09/2026 (suite 7) — 🏁 Les 20 sources sont migrées, le chantier applicatif est terminé.**
Pilotes MiseADisposition, Agios, Commission, Frais bancaire, Prélèvement, Impôt/Taxe et Opération
erronée menés backend + frontend + nettoyage dans la même session, à la demande explicite de
HMAYDA (« attaquer les 7 sources à la fois ») — dernier lot du chantier, couvrant les deux derniers
patrons restants :
- **MiseADisposition** : seule des 7 à sens explicite choisi par l'appelant (même patron que
  Virement/Chèque/Virement international — deux méthodes de service, une par sens, un seul
  `MiseADispositionBatchValidationHandler` branchant sur `requestDto.sens()`). Le récap distingue
  l'identifiant métier affiché selon le sens, comme les 2 écrans dédiés d'origine : `N° Opération`
  pour un décaissement, `Émetteur` pour un encaissement — seul le libellé du champ change, aucune
  logique métier nouvelle.
- **Agios, Commission, Frais bancaire, Prélèvement, Impôt/Taxe, Opération erronée** : les 6
  restantes, toutes décaissement fixe, un seul appel de service chacune, même patron que
  Restitution/Provision/Impayée — aucune ne nécessite de `banqueId` explicite. Aucune n'a
  d'identifiant métier naturel distinct de son id dans son écran dédié d'origine (la boîte de
  confirmation y affichait déjà l'id) : `recapLabel`/`recapValue` retombent sur l'id pour les 6,
  cohérent avec le choix fait pour Restitution/Provision.
- **Recherche préalable** : 7 agents de recherche lancés en parallèle pour rassembler tous les
  faits nécessaires par source (endpoint détail, structure du DTO de réponse, méthode de service
  frontend, DTO de requête de validation, chemin de route) avant d'écrire le moindre code —
  volume trop important pour être lu séquentiellement dans la session sans saturer le contexte.
- **Backend** : 7 nouveaux handlers (`MiseADispositionBatchValidationHandler`,
  `AgiosBatchValidationHandler`, `CommissionBatchValidationHandler`,
  `FraisBancaireBatchValidationHandler`, `PrelevementBatchValidationHandler`,
  `ImpotEtTaxeBatchValidationHandler`, `OperationErroneBatchValidationHandler`). Les 7 `case`
  correspondants retirés de `BatchValidationService.processOne` — **le switch historique est
  entièrement supprimé** (plus aucune source n'y retombe), `processOne` se réduit à une simple
  consultation du registre de handlers. Les champs/imports/constructeur devenus inutiles retirés en
  bloc ; `copyChampsCommuns` (n'avait plus qu'un seul appelant, MiseADisposition) supprimé avec eux.
  Javadoc de classe entièrement réécrite pour refléter l'état final (registre unique, plus de
  switch, 3 familles de patron documentées).
- **Frontend** : 7 nouvelles entrées dans `unitValidationConfigs`. TVA libre/optionnelle par défaut
  0 % pour les 7, comme leurs écrans dédiés respectifs.
- **Nettoyage** : 8 écrans dédiés supprimés (MiseADisposition en a 2, les 6 autres 1 chacun) —
  **les dossiers `decaissement/` et `encaissements/` de `etatsDesEncours/views/lesEncours/` sont
  désormais complètement vides et ont été supprimés**, plus aucun écran de validation dédié
  n'existe nulle part dans le projet. Routes retirées de `etats-des-encours.routes.ts`. Dans
  `les-encours.component.ts` : les 7 dernières entrées `breadcrumbMap` retirées (ne reste que
  `rapprochement`/`recapitulatif`) ; la catégorie de menu « Décaissements » supprimée entièrement
  (n'avait plus aucun enfant après retrait de ses 6 derniers items — même raisonnement que la
  suppression de « Encaissements » lors du lot précédent) ; le menu latéral se réduit maintenant à
  Rapprochement + Récapitulatifs, ce qui est la conséquence naturelle et attendue de la migration
  complète. Commentaire obsolète sur `SEGMENTS_REALISES`/`breadcrumbMap` (mentionnait encore « 34
  entrées ») simplifié en conséquence.
  Code orphelin retiré des deux côtés pour chacune des 7 sources — `getOutstandingXxx`/
  `getEncoursXxxForYyyByBank`, leurs exports PDF encours, et les helpers `buildEncoursFilterSummary`/
  `finDuMoisCourant`/`endOfMonth` (champ) que ces méthodes étaient les seules à utiliser. Particularité
  Commission : `endOfMonth` était un **champ** calculé une fois à la construction du bean (même bug
  que celui déjà corrigé ailleurs) — devenu orphelin et supprimé avec son unique paire
  d'appelants. `sumMontant`/`generateXxxPdf`/`buildFilterSummary` **conservés** partout où
  l'export PDF non-encours (`exporterPdf`/`exportPdf`) les utilise encore. `IOperationErroneService`
  perd aussi `EncoursOperationErroneDto` de sa surface (contrairement à Commission/FraisBancaire/
  ImpotEtTaxe, `searchOperationErrone` renvoie un `Page` simple, pas de DTO à total agrégé — donc
  aucun autre usage ne retenait ce DTO après le retrait de l'encours). `Specification` builders
  laissés en place partout, même décision que pour toutes les sources précédentes.
- **Fichiers** : 7 nouveaux handlers ; modifiés : `BatchValidationService.java`,
  `BatchValidationHandler.java`, `recapitulatif.component.ts`, `recapitulatif.component.html`,
  `etats-des-encours.routes.ts`, `les-encours.component.ts`, et les service/interface/controller
  de chacun des 7 modules (backend) + leur service Angular (frontend) ; supprimés : 8 dossiers
  d'écrans dédiés complets (+ les dossiers `decaissement/`/`encaissements/` désormais vides), les
  dossiers `decaissement/`/`encaissements/` eux-mêmes, et l'ensemble des DTOs `Encours*`
  spécifiques à l'encours (listés en détail dans les fichiers modifiés/supprimés de cette session).
- **Prochaine étape** : HMAYDA build/teste manuellement l'intégralité de l'application (backend +
  frontend), commit/push. Le chantier applicatif de migration vers le récap unifié est terminé —
  reste en attente la décision d'architecture mentionnée par HMAYDA le 19/09/2026 (voir Décisions
  arbitrées ci-dessous) : unifier, pour chaque source à deux sens qui a encore deux méthodes de
  service séparées (Virement, Chèque, Virement international, MiseADisposition), en une seule
  méthode de validation par source avec branchement interne sur le sens — à traiter dans une session
  dédiée, avec les options/recommandation habituelles avant tout changement de code, puisque cela
  touche des `XxxValidationService` déjà en production.

**19/09/2026 (suite 6).** Pilote Virement international mené backend + frontend + nettoyage, à la
demande explicite de HMAYDA (« on attque virement international ») — retour à la première famille
de patron (sens explicite choisi par l'appelant), comme Cheque/Virement, après les deux familles
« sens porté par l'entité » (marché, puis Restitution/Provision/Impayée) :
- **Constat, avant codage** : le module avait déjà son propre sous-package `validation/` (comme
  Cheque/LCN/Virement, contrairement à la famille marché/Restitution/Provision/Impayée) — signe
  avant-coureur confirmé par lecture du `case VIREMENT_INTERNATIONAL` historique dans
  `BatchValidationService` : branchement sur `requestDto.sens()` entre
  `processVirementInternationalDecaissementTransaction`/`...EncaissementTransaction`, exactement le
  patron Virement. `GET /virements-internationaux/{id}` existait déjà — rien à créer pour l'étape 1.
  Curiosité relevée en lisant `VirementInternationalValidationService` : les deux méthodes
  publiques ont un corps strictement identique (délèguent toutes deux à la même méthode privée
  `processTransaction`) — c'est le mapper qui lit `virementInternational.getType()` pour décider
  débit/crédit, pas la méthode appelée ; sans conséquence pour la migration (le handler reproduit
  fidèlement le `case` existant, qui appelait déjà l'une ou l'autre sans changement de comportement
  observable).
- **`banqueId` non exigé** : contrairement à la famille marché, VIREMENT_INTERNATIONAL n'apparaît
  pas dans `requireBanqueIdIfNeeded` — confirmé par lecture du switch, aucun changement nécessaire
  là.
- **Backend** : nouveau `VirementInternationalBatchValidationHandler`
  (`virementInternational/validation/services/`), copie exacte du patron
  `VirementBatchValidationHandler` (branchement sur `requestDto.sens() == SensOperation.DECAISSEMENT`).
  `case VIREMENT_INTERNATIONAL` retiré de `BatchValidationService.processOne`, avec le champ/import
  devenu inutile (`virementInternationalValidationService`, DTOs et interface associées) — vérifié
  par grep, aucune référence résiduelle. **13 sources migrées au total à ce jour** — 7 restantes
  dans le switch historique (MiseADisposition, Agios, Commission, Frais bancaire, Prélèvement,
  Impôt/Taxe, Opération erronée).
- **Frontend** : nouvelle entrée `TransactionSource.VIREMENT_INTERNATIONAL` dans
  `unitValidationConfigs`, réutilisant l'import déjà partagé `Type as SensType`
  (`../../../../../../Virements/Type`, déjà importé pour Virement/Cheque) pour le branchement
  `d.type === SensType.DECAISSEMENT`. `recapLabel` = « Bénéficiaire » (`d.beneficiaire`), seul champ
  identifiant naturel affiché par les 2 écrans dédiés actuels dans leur boîte de confirmation. TVA
  libre/optionnelle par défaut 0 % comme les 2 écrans dédiés actuels.
- **Nettoyage** : 2 écrans dédiés supprimés (décaissement + encaissement), route/breadcrumb/menu
  retirés (`etats-des-encours.routes.ts`/`les-encours.component.ts`) — la catégorie de menu
  « Encaissements » n'avait plus que Virement international comme unique enfant : supprimée
  entièrement plutôt que laissée avec `items: []` (menu expansible vide = dead UI, cohérent avec la
  règle CLAUDE.md « supprimer le dead code »). Code orphelin retiré des deux côtés —
  `getEncoursDecaissement`/`getEncoursEncaissement`, leurs exports PDF, et les helpers
  `buildEncoursFilterSummary`/`finDuMoisCourant`/`buildEncoursParams` que ces méthodes étaient les
  seules à utiliser — `sumMontant` **conservé** côté backend (encore utilisé par `exportPdf`, qui
  reste). `VirementInternationalSpec` (builders `encoursDecaissement`/`encoursEncaissement`/
  `hasDateEmission`/etc.) **laissé en place**, même décision que pour toutes les sources
  précédentes.
- **Fichiers** : 1 nouveau (`VirementInternationalBatchValidationHandler.java`) ; modifiés :
  `BatchValidationService.java`, `BatchValidationHandler.java`, `recapitulatif.component.ts`,
  `recapitulatif.component.html`, `etats-des-encours.routes.ts`, `les-encours.component.ts`,
  `VirementInternationalService.java`, `VirementInternationalController.java`,
  `virement-international.service.ts` ; supprimés : 2 dossiers d'écrans dédiés complets,
  `EncoursVirementInternationalResponseDto.java`/`.ts`, `EncoursVirementsInternationalSearchDto.java`/`.ts`.
- **Prochaine étape** : HMAYDA build/teste manuellement backend et frontend, commit/push, puis
  choix de la source suivante parmi les 7 restantes (MiseADisposition, Agios, Commission, Frais
  bancaire, Prélèvement, Impôt/Taxe, Opération erronée).

**19/09/2026 (suite 5).** Pilotes Restitution, Provision et Impayée menés backend + frontend +
nettoyage dans la même session, à la demande explicite de HMAYDA (« oki let's atatque restitution ,
impayee et provision ») — troisième famille de patron après « sens explicite » (Cheque/Virement/LCN)
et « sens porté par l'entité, marché » (Préfinancement/Factoring/Droit constaté) :
- **Constat, avant codage** : les 3 endpoints de détail `GET .../{id}` existaient déjà — rien à
  créer pour l'étape 1. Confirmé par lecture complète de `RestitutionController`/
  `RestitutionValidationService`, `ProvisionController`/`ProvisionValidationService`,
  `ImpayeeController`/`ImpayeeValidationService` et de leurs mappers/DTOs avant tout code.
- **Troisième variante de sens porté par l'entité, sans `banqueId`** : comme la famille « marché »,
  ces 3 sources ont un seul appel de service, aucun branchement sur `sens` dans le handler — mais
  chacune matérialise le sens différemment sur l'entité : Restitution est **toujours** un
  encaissement (aucun champ de sens du tout) ; Provision porte un champ `SensOperation sens` direct
  (commentaire trouvé dans `provision-encours.component.ts` : « un seul endpoint de validation,
  le sens de la Transaction générée est déterminé côté backend par `provision.getSens()`, pas par
  l'écran appelant ») ; Impayée porte un `Type` (`moyensDePaiements.virements.enums.Type`), décidé
  par le mapper comme pour la famille marché. **Différence clé avec la famille marché : aucune de
  ces 3 sources n'exige de `banqueId`** — confirmé par lecture des DTOs (aucun champ `banqueId`) et
  des services de validation (`banqueService.findOrThrow(entity.getBanque().getId())` n'est qu'une
  vérification d'existence, jamais une comparaison avec une valeur transmise par l'appelant) —
  c'est le patron le plus simple rencontré jusqu'ici.
- **Ambiguïté du champ « Date d'émission »** : même situation que toutes les sources précédentes —
  appliqué directement la politique générale actée le 19/09/2026 (verrouiller), sans re-demander à
  HMAYDA (voir Décisions arbitrées).
- **Backend** : 3 nouveaux handlers, chacun un simple appel direct construit à partir des mêmes 9
  arguments positionnels que l'ancien `case` (`id, tauxTva, null, null, dateValeur, dateOperation,
  null, null, rapprochementId` — reproduisant à l'identique le comportement historique) —
  `RestitutionBatchValidationHandler`, `ProvisionBatchValidationHandler`,
  `ImpayeeBatchValidationHandler` (chacun dans le sous-package `validation/services/` de son
  module). Les 3 `case` correspondants retirés de `BatchValidationService.processOne`, avec les
  champs/imports devenus inutiles (`restitutionValidationService`, `provisionValidationService`,
  `impayeeValidationService` et leurs DTOs/interfaces) — vérifié par grep, aucune référence
  résiduelle. **12 sources migrées au total à ce jour** (Leasing, CMT, Chèque, LCN×2, Virement,
  Préfinancement, Factoring, Droit constaté, Décomptes, Restitution, Provision, Impayée — **8
  restantes** dans le switch historique : MiseADisposition, VirementInternational, Agios,
  Commission, Frais bancaire, Prélèvement, Impôt/Taxe, Opération erronée).
- **Frontend** : 3 nouvelles entrées dans `unitValidationConfigs` (`RESTITUTION`, `PROVISION`,
  `IMPAYEE`), `recapLabel`/`recapValue` choisis par cohérence avec ce que chaque écran dédié
  affichait déjà (« ID Restitution »/« ID Provision » + `d.id`, « N° » + `d.numero ?? d.id` pour
  Impayée qui a un numéro optionnel), TVA libre/optionnelle par défaut 0 % comme leurs écrans
  dédiés respectifs.
- **Nettoyage** : 5 écrans dédiés supprimés (Restitution n'en a qu'un — toujours encaissement —,
  Provision et Impayée en ont 2 chacun — décaissement/encaissement, malgré le sens porté par
  l'entité et non par l'écran, ces écrans filtraient simplement par sens à l'affichage), routes/
  menu/breadcrumbs retirés (5 routes, 5 entrées `breadcrumbMap`, 5 items de menu dans
  `etats-des-encours.routes.ts`/`les-encours.component.ts`), code orphelin retiré des deux côtés —
  `getOutstandingRestitutions`/`getOutstandingProvision`/`getOutstandingImpayees`, leurs exports
  PDF encours, et les helpers `buildEncoursFilterSummary`/`TemporalAdjusters`/`PageRequest` que ces
  méthodes étaient les seules à utiliser dans chacun des 3 services — retirés avec elles. Javadocs
  de classe des 3 `ServiceImpl` (mentions désormais fausses des méthodes supprimées) nettoyés.
  `sumMontant`/`libelleType` (Impayée) et `totalSigne` (Provision) **conservés** : encore utilisés
  par `exporterPdf`, qui reste. Les `Specification` builders (`RestitutionSpec`/
  `ProvisionSpecification`/`ImpayeeSpecifications` — `outstandingUntil`, `hasBanque`, etc.)
  **laissés en place**, même décision que pour toutes les sources précédentes.
- **Fichiers** : 3 nouveaux (`RestitutionBatchValidationHandler.java`,
  `ProvisionBatchValidationHandler.java`, `ImpayeeBatchValidationHandler.java`) ; modifiés :
  `BatchValidationService.java`, `BatchValidationHandler.java`, `recapitulatif.component.ts`,
  `recapitulatif.component.html`, `etats-des-encours.routes.ts`, `les-encours.component.ts`,
  `RestitutionServiceImpl.java`/`IRestitutionService.java`/`RestitutionController.java`,
  `ProvisionServiceImpl.java`/`IProvisionService.java`/`ProvisionController.java`,
  `ImpayeeServiceImpl.java`/`IImpayeeService.java`/`ImpayeeController.java`,
  `restitution.service.ts`, `provision.service.ts`, `ImpayeeService.ts` ; supprimés : 5 dossiers
  d'écrans dédiés complets, `EncoursRestitution.java`/`.ts`, `EncoursRestitutionSearchDto.java`,
  `EncoursProvisionDto.java`/`.ts`, `EncoursProvisionSearchDto.java`, `EncoursImpayeeDto.java`/`.ts`,
  `EncoursImpayeeSearchDto.java`.
- **Prochaine étape** : HMAYDA build/teste manuellement backend et frontend, commit/push, puis
  choix de la source suivante parmi les 8 restantes (MiseADisposition, VirementInternational,
  Agios, Commission, Frais bancaire, Prélèvement, Impôt/Taxe, Opération erronée).

**19/09/2026 (suite 4).** Pilotes Préfinancement, Droit constaté, Factoring et Décomptes menés
backend + frontend + nettoyage dans la même session, même patron que les sources précédentes —
ces 4 sources partagent une structure commune (« marché »), distincte de Cheque/Virement/LCN :
- **Constat, avant codage** : les 4 endpoints de détail `GET .../{id}` existaient déjà (comme pour
  toutes les sources précédentes) — rien à créer pour l'étape 1.
- **Sens porté par l'entité, jamais par l'appelant** : contrairement à Cheque/Virement (`sens`
  explicite) et à LCN (`TransactionSource` éclaté), Préfinancement/Factoring/Droit constaté ont un
  champ `Type` (REMBOURSEMENT/DEBLOCAGE) directement sur l'entité — le mapper de validation décide
  seul du débit/crédit à partir de ce champ, donc **un seul appel de service par source**, aucun
  branchement sur `sens` ni sur le détail au runtime (contrairement à Cheque/Virement qui lisent
  `d.type`). Décompte est toujours un encaissement (pas de `Type` du tout). Confirmé par lecture de
  `PrefinancementValidationService`/`FactoringValidationService`/`DroitConstateValidationService`/
  `DecompteValidationService` et de leurs mappers — aucune ambiguïté business à trancher avec
  HMAYDA sur ce point (résout la question laissée en suspens dans le tableau des 24 sources).
- **`banqueId` obligatoire, contrairement aux sources précédentes** : ces 4 sources exigent déjà un
  `banqueId` explicite pour la validation en lot (`requireBanqueIdIfNeeded`, inchangé) — pour
  vérifier la cohérence avec la banque du marché (Préfinancement/Factoring), la banque propre à
  l'entité (Droit constaté) ou la banque d'encaissement (Décomptes). Côté récap, ce `banqueId` est
  lu directement sur le DTO de détail (`d.banqueId` / `d.banqueEncaissementId` pour Décomptes) —
  déjà résolu côté backend, pas besoin de le faire porter par `banqueIdFromRoute` du composant.
- **Ambiguïté du champ « Date d'émission »** : même situation que Cheque/LCN/Virement (écrans
  dédiés éditables, dialogue générique verrouillé) — appliqué directement la politique générale
  actée le 19/09/2026 (verrouiller), sans re-demander à HMAYDA (voir Décisions arbitrées).
- **Backend** : 4 nouveaux handlers, un par source, chacun un simple appel direct (aucune
  complexité de branchement comme pour Cheque/Virement/LCN) — `PrefinancementBatchValidationHandler`,
  `FactoringBatchValidationHandler`, `DroitConstateBatchValidationHandler`,
  `DecompteBatchValidationHandler` (chacun dans le sous-package `validation/services/` de son
  module, sibling de son `XxxValidationService`). Les 4 `case` correspondants retirés de
  `BatchValidationService.processOne`, avec les champs/imports devenus inutiles. 9 sources migrées
  au total à ce jour (Leasing, CMT, Chèque, LCN×2, Virement, Préfinancement, Factoring, Droit
  constaté, Décomptes — 11 restantes dans le switch historique).
- **Frontend** : 4 nouvelles entrées dans `unitValidationConfigs` (`PREFINANCEMENT`, `FACTORING`,
  `DROIT_CONSTATE`, `DECOMPTES`), `recapLabel` = « N° de Marché » (`d.marcheNumero`) pour les
  quatre, TVA libre/optionnelle par défaut 0 % comme leurs écrans dédiés respectifs.
- **Nettoyage** : 7 écrans dédiés supprimés (Préfinancement, Droit constaté et Factoring en ont 2
  chacun — remboursement/déblocage — Décomptes n'en a qu'un, toujours encaissement), routes/menu/
  breadcrumbs retirés, code orphelin retiré des deux côtés — `getRemboursementEncoursForBanque`/
  `getDeblocageEncoursForBanque`/`getDecomptesEncoursForBanque`, leurs exports PDF, et surtout le
  calcul de somme par `EntityManager`/`CriteriaBuilder` (`sumMontant`) que ces méthodes étaient les
  seules à utiliser dans chacun des 4 services — retiré avec elles (imports `EntityManager`/
  `PersistenceContext`/`CriteriaBuilder`/`CriteriaQuery`/`Root`/`TemporalAdjusters` nettoyés).
  **Oubli rattrapé en cours de route** : `PrefinancementService.ts` avait été laissé de côté au
  premier passage (seuls les 3 autres services frontend avaient été nettoyés) — repéré par un grep
  de vérification final, corrigé avant de continuer. Les `Specification` builders
  (`PrefinancementSpecification`/`FactoringSpec`/`DroitConstateSpec`/`DecompteSpec` — `byBanqueId`,
  `outstandingRemboursementUntil`, etc.) **laissés en place**, même décision que pour toutes les
  sources précédentes.
- **Prochaine étape** : HMAYDA build/teste manuellement backend et frontend, commit/push, puis
  choix de la source suivante parmi les 11 restantes (MiseADisposition, VirementInternational,
  Provision, Impayé, Agios, Commission, Frais bancaire, Prélèvement, Impôt/Taxe, Restitution,
  Opération erronée).

**19/09/2026 (suite 3).** Pilotes LCN et Virement menés backend + frontend + nettoyage dans la
même session, même patron que Chèque (voir entrée précédente) :
- **Ambiguïté tranchée avec HMAYDA avant tout code** : champ « Date d'émission » du dialogue
  unitaire — même situation que Chèque (écrans dédiés éditables, dialogue générique verrouillé) —
  HMAYDA confirme verrouiller pour les deux, cohérence avec Chèque (voir Décisions arbitrées).
- **Constat LCN, avant codage** : LCN a une structure différente de Chèque/Virement — le sens
  n'est pas porté par `sens` mais directement par trois valeurs `TransactionSource` distinctes
  (`LCN_DECAISSEMENT`, `LCN_ENCAISSEMENT`, `LCN_ESCOMPTE` — vues dans
  `sql/v_recapitulatif_encours.sql`, `CASE WHEN l.type = 'DECAISSEMENT' THEN 'LCN_DECAISSEMENT'
  WHEN l.nature_lcn = 'ENCAISSEMENT' THEN 'LCN_ENCAISSEMENT' ELSE 'LCN_ESCOMPTE'`). Le service
  unitaire ne distingue que 2 méthodes (`processLcnDecaissementTransaction` /
  `processLcnEncaissementTransaction` — cette dernière gère indifféremment ENCAISSEMENT et
  ESCOMPTE, le mapper décidant du tag `TransactionSource` de la transaction créée après coup).
  `LCN_ESCOMPTE` n'a jamais eu de `case` dans `BatchValidationService` (retombe sur le `default` et
  lève une exception) — gap préexistant, non comblé par cette migration (zéro changement de
  comportement).
- **Backend** :
  - `GET /lcns/{id}` et `GET /virements/{id}` existaient déjà (comme pour Chèque) — rien à créer
    pour l'étape 1.
  - Deux nouveaux handlers `LcnDecaissementBatchValidationHandler` / 
    `LcnEncaissementBatchValidationHandler` (`lcn/validation/services/`, sibling de
    `LcnValidationService`) — un handler par `TransactionSource` (contrairement à Chèque/Virement
    qui n'ont qu'un handler avec branchement sur `sens`), chacun reprenant à l'identique le corps
    de son ancien `case`. `LCN_ESCOMPTE` volontairement sans handler.
  - Nouveau `VirementBatchValidationHandler` (`virements/validation/services/`), copie exacte du
    patron `ChequeBatchValidationHandler` (un seul handler, branchement sur `requestDto.sens()`).
  - `case Virement`, `case LCN_DECAISSEMENT`, `case LCN_ENCAISSEMENT` retirés de
    `BatchValidationService.processOne`, avec les champs/imports devenus inutiles
    (`virementValidationService`, `lcnValidationService`, DTOs et interfaces associées) — vérifié
    par grep, aucune référence résiduelle. Javadoc de classe et de `BatchValidationHandler` mis à
    jour (5 sources migrées au total : Leasing, CMT, Chèque, LCN×2, Virement — 15 restantes dans le
    switch historique).
  - `ValidationController` (4 endpoints unitaires `process/{decaissement,encaissement}/{lcn,virement}`)
    et `LcnValidationService`/`VirementValidationService` non touchés — comportement inchangé.
- **Frontend** :
  - 4 nouvelles entrées dans `unitValidationConfigs` : `LCN_DECAISSEMENT`, `LCN_ENCAISSEMENT`,
    `LCN_ESCOMPTE` (ces deux dernières partagent la même config — même endpoint
    `lcnEncaissementToRapprochement`, reproduisant le seul écran dédié encaissement actuel qui
    traite les deux natures indifféremment) et `Virement` (branchement runtime sur `d.type`, comme
    Chèque — son seul `TransactionSource` ne suffit pas à distinguer le sens).
  - Import `Type` renommé `ChequeType` → `SensType` (partagé maintenant par Chèque et Virement).
  - TVA libre/optionnelle par défaut 0 % pour les deux, comme leurs écrans dédiés respectifs.
- **Nettoyage** (mené dans la même session, comme pour Chèque) :
  - **4 écrans dédiés supprimés** : `lcn-encours/` (décaissement), `encaissement-lcn-encours/`,
    `virement-encours/` (décaissement ET encaissement — même nom de dossier/composant dans les
    deux arborescences `decaissement/` et `encaissements/`, comme Cheque).
  - Retrait des 4 entrées de route, 4 entrées `breadcrumbMap`, 4 items de menu correspondants dans
    `etats-des-encours.routes.ts` / `les-encours.component.ts`.
  - **Code orphelin retiré** (vérifié par grep avant suppression, aucun autre appelant — les
    modules `LCN`/`Virements` eux-mêmes, avec leurs propres écrans de liste/CRUD sous
    `portail/LCN`/`portail/Virements`, restent intacts et n'appelaient aucune de ces méthodes) :
    - Backend `LcnService.java` : `getEncoursDecaissementsForLcn`, `getEncoursEncaissementsForLcn`,
      `exportEncoursEncaissementPdf`, `exportEncoursDecaissementPdf` + leur seul helper dédié
      `buildEncoursFilterSummary` et `finDuMoisCourant` (sans appelant restant), import
      `TemporalAdjusters` retiré. 2 endpoints `LcnController` retirés. DTOs
      `EncoursLcnResponse.java`/`EncoursLcnSearchDto.java` supprimés.
    - Backend `VirementService.java` : mêmes 4 méthodes + `buildEncoursFilterSummary` +
      `finDuMoisCourant` retirées, import `TemporalAdjusters` retiré. 2 endpoints
      `VirementController` retirés. DTOs `EncoursVirementResponseDto.java`/
      `EncoursVirementsSearchDto.java` supprimés.
    - Frontend `LcnService.ts` : `encoursDecaissementLcnsByBank`, `encoursEncaissementLcnsByBank`,
      `exportEncoursEncaissementPdf`, `exportEncoursDecaissementPdf` retirées, import
      `toLocalDateString` retiré (devenu inutile) ; DTOs `EncoursLcnResponse.ts` et
      `EncoursLcnSearchDto.ts` (celui-ci découvert orphelin après coup, jamais utilisé côté
      backend — supprimé aussi) supprimés.
    - Frontend `VirementService.ts` : `getEncoursDecaissementVirementsByBank`,
      `getEncoursEncaissementVirementsByBank`, `exportEncoursEncaissementPdf`,
      `exportEncoursDecaissementPdf` retirées, DTOs `EncoursVirementResponseDto.ts`/
      `EncoursVirementsSearchDto.ts` supprimés.
    - `LcnSpecification`/`VirementSpec` (builders `encoursDecaissement`/`encoursEncaissement`/
      `hasDateEcheance`/`hasDateEmission`) **laissés en place**, même décision que
      `ChequeSpec`/`EcheanceLeasingSpecification`/`EcheanceCMTSpecification`.
- **Prochaine étape** : HMAYDA build/teste manuellement backend et frontend, commit/push, puis
  choix de la source suivante parmi les 19 restantes.

**19/09/2026 (suite 2).** Pilote Chèque mené backend + frontend + nettoyage, même patron que
Leasing/CMT, en une seule session (les 4 étapes demandées par HMAYDA — endpoint détail, extraction
`BatchValidationHandler`, câblage frontend, retrait des écrans dédiés) :
- **Ambiguïtés tranchées avec HMAYDA avant tout code** (règle non négociable), via 2 questions à
  choix + recommandation — voir Décisions arbitrées pour le détail complet :
  1. Champ « Date d'émission » du dialogue unitaire générique → HMAYDA choisit de le verrouiller
     comme Leasing/CMT (contre ma recommandation d'ajouter un flag par source pour le garder
     éditable comme sur les 2 écrans dédiés Chèque actuels). Changement de comportement assumé.
  2. Documentation (`docs/modules`/`docs/direction`/`docs/guide-utilisateur`/`docs/rbac`) → HMAYDA
     choisit de rester cohérent avec le précédent (non mise à jour pour Leasing/CMT non plus) —
     écart connu, à rattraper plus tard pour toutes les sources migrées en une fois.
- **Constat avant codage** : contrairement à Leasing/CMT, l'endpoint de détail `GET /cheques/{id}`
  existait déjà (`ChequeController.getById` → `ChequeService.getChequeById`), tout comme son
  pendant frontend `ChequeService.getChequeById(id)` — rien à créer côté backend pour l'étape 1.
- **Backend** :
  - Nouveau `ChequeBatchValidationHandler` (`moyensDePaiements/cheques/validation/services/`,
    sibling de `ChequeValidationService` — module Chèque a son propre sous-package `validation`,
    contrairement à Leasing/CMT), reprenant à l'identique (copier-coller, aucun changement de
    logique) le corps de l'ancien `case CHEQUE ->` de `BatchValidationService`. Seule différence
    structurelle avec les handlers Leasing/CMT : le chèque a un `sens` propre (`requestDto.sens()`
    choisit entre `processChequeDecaissementTransaction`/`processChequeEncaissementTransaction`),
    déjà exigé en amont par `requireSensIfNeeded` (aucun changement nécessaire à cette méthode).
    `copyChampsCommuns` étant une méthode privée de `BatchValidationService`, le handler inline les
    4 mêmes affectations de champs plutôt que d'y faire appel — comportement strictement identique.
  - `case CHEQUE` retiré de `BatchValidationService.processOne`, avec les champs/imports devenus
    inutiles (`chequeValidationService`, `ChequeTransactionRequest`, `ChequeTransactionResponseDto`,
    `IChequeValidationService`) — vérifié par grep sur tout le fichier après édition, aucune
    référence résiduelle. Javadoc de classe et de `BatchValidationHandler` mis à jour (Leasing/CMT
    → Leasing/CMT/Chèque, 20 → 19 sources restantes).
  - `ValidationController` (`process/decaissement/cheque`, `process/encaissement/cheque`) et
    `ChequeValidationService`/`IChequeValidationService` eux-mêmes non touchés — même principe que
    Leasing/CMT, seul le dispatch de la validation **en lot** change de mécanisme.
- **Frontend** :
  - Entrée `TransactionSource.CHEQUE` ajoutée à `unitValidationConfigs` dans
    `recapitulatif.component.ts` : `fetchDetail` réutilise `ChequeService.getChequeById` (déjà
    existant), TVA libre/optionnelle par défaut 0 % (comme CMT et comme les 2 écrans dédiés Chèque
    actuels), `recapLabel` = « N° Chèque ». Particularité Chèque (contrairement à Leasing/CMT qui
    ont un sens fixe) : `buildAndSubmit` lit `d.type` (fixé à la création du chèque, jamais un
    choix fait à la validation) pour choisir entre
    `chequeDecaissementToRapprochement`/`chequeEncaissementToRapprochement` — les deux méthodes
    existaient déjà sur `ValidationService`, aucune modification nécessaire là non plus.
- **Nettoyage (mené dans la même session, sans attendre de validation intermédiaire de HMAYDA —
  contrairement à Leasing/CMT où le nettoyage a eu lieu dans une session séparée après validation
  réelle ; ici les 4 étapes ont été explicitement demandées ensemble)** :
  - **2 écrans dédiés retirés** (Chèque en a deux, décaissement + encaissement, contrairement à
    Leasing/CMT qui n'en avaient qu'un) : dossiers complets
    `Frontend/.../decaissement/cheques-encours/` et
    `Frontend/.../encaissements/cheque-encaissements-encours/` supprimés (`.ts/.html/.scss/.spec.ts`
    de chaque) — suppression physique réelle cette fois (contrairement aux pilotes précédents,
    `device_bash` indisponible n'est plus une contrainte de cette session).
  - Retrait des 2 entrées de route (`cheques-decaissements`, `cheques-encaissements`) dans
    `etats-des-encours.routes.ts`, des 2 entrées `breadcrumbMap` et des 2 items de menu
    (Encaissements/Décaissements → Chèques) dans `les-encours.component.ts`.
  - **Code orphelin retiré** (vérifié par grep sur tout `Frontend/src` et `Backend/src/main/java`
    avant suppression — aucun autre appelant) :
    - Frontend `ChequeService.ts` : `getEncoursDecaissementChequesByBank`,
      `getEncoursEncaissementChequesByBank`, `exportEncoursEncaissementPdf`,
      `exportEncoursDecaissementPdf` retirées, imports `EncoursChequeResponseDto`/
      `EncoursChequeSearchDto`/`toLocalDateString` (devenu inutile) retirés ; les 2 fichiers DTO
      `EncoursChequeResponseDto.ts`/`EncoursChequeSearchDto.ts` supprimés.
    - Backend `ChequeService.java` : `getEncoursDecaissementForCheques`,
      `getEncoursEncaissementForCheques`, `exportEncoursEncaissementPdf`,
      `exportEncoursDecaissementPdf` retirées, avec leur seul helper privé dédié
      `buildEncoursFilterSummary` et `finDuMoisCourant` (devenus sans appelant), import
      `TemporalAdjusters` retiré. Le champ `banqueService` (`IBanqueService`) reste utilisé par
      `createEncaissementCheque`/`editEncaissementCheque` — pas retiré, contrairement à Leasing/CMT
      où il était devenu totalement orphelin.
    - Backend `ChequeController.java` : 4 endpoints retirés (`GET .../decaissements/encours/{id}`,
      `GET .../encaissements/encours/{id}`, et leurs 2 exports PDF) ; les 2 DTOs
      `EncoursChequeResponseDto.java`/`EncoursChequeSearchDto.java` supprimés.
    - `ChequeSpec` (`encoursDecaissements`/`encoursEncaissements`/`hasDateEmission`) **laissé en
      place** malgré la perte de leur seul appelant — même décision que pour
      `EcheanceLeasingSpecification`/`EcheanceCMTSpecification` lors des pilotes précédents
      (builders de `Specification` partagés, retrait à faire seulement si HMAYDA le demande).
- **Fichiers** : 1 nouveau (`ChequeBatchValidationHandler.java`) ; modifiés :
  `BatchValidationService.java`, `BatchValidationHandler.java`, `ChequeController.java`,
  `ChequeService.java`, `recapitulatif.component.ts`, `recapitulatif.component.html`,
  `etats-des-encours.routes.ts`, `les-encours.component.ts`, `ChequeService.ts` ; supprimés :
  `EncoursChequeResponseDto.java`/`.ts`, `EncoursChequeSearchDto.java`/`.ts`, et les dossiers complets
  des 2 écrans dédiés Chèque.
- **Prochaine étape** : HMAYDA build/teste manuellement backend et frontend (aucune capacité de
  compilation depuis cette session), commit/push lui-même, puis choix de la source suivante parmi
  les 21 restantes.

**19/09/2026 (suite).** Bug remonté par HMAYDA en test manuel réel : après avoir sélectionné des
opérations CMT + Leasing mélangées (blocage « Sélection hétérogène » bien déclenché — voir entrée
19/09/2026 ci-dessous), puis désélectionné les lignes CMT pour ne garder que 4 lignes Leasing, le
compteur « X opération(s) sélectionnée(s) » et le libellé du bouton « Valider en lot (X) » restaient
bloqués sur l'ancienne valeur (8) au lieu de refléter la désélection.
- **Cause identifiée** : `<p-table>` n'avait jamais de `dataKey` explicite. Sans lui, PrimeNG
  détermine par lui-même si une ligne est "sélectionnée" via une comparaison interne (égalité
  profonde des objets ou équivalent) plutôt qu'une clé stable — fragile dès qu'une page est
  rechargée (tri, pagination, changement de filtre : `loadRecapitulatif()` remplace `recapItems`
  par des instances d'objets fraîchement désérialisées à chaque appel). Notre propre
  `TableSelectionState` utilisait déjà une clé composite fiable (`${source}:${id}` — `id` seul
  n'est pas unique tous sources confondues, chaque source ayant sa propre séquence en base), mais
  PrimeNG, lui, n'avait aucun moyen de reconnaître la même ligne de façon cohérente entre deux
  rendus : les deux mécanismes de suivi (le nôtre et celui de PrimeNG) pouvaient diverger,
  d'où un décalage entre coches visuelles/désélections et le compteur réel.
- **Fix** : ajout d'un champ `compositeKey: string` sur `RecaputilatifItem` (calculé côté client
  dans `loadRecapitulatif()` juste après réception de la réponse — `${item.source}:${item.id}`,
  jamais renvoyé par le backend), `dataKey="compositeKey"` posé explicitement sur `<p-table>`, et
  `TableSelectionState` construit avec `item => item.compositeKey` (même valeur exacte que le
  `dataKey` de PrimeNG — élimine toute divergence entre les deux). Fichiers modifiés :
  `RecaputilatifItem.ts` (542 → 1505 octets), `recapitulatif.component.ts` (33812 → 34110 octets),
  `recapitulatif.component.html` (24560 → 24589 octets) — vérifiés byte-for-byte après écriture.
- **Prochaine étape** : HMAYDA retteste en usage réel (sélection multi-source, désélection,
  changement de filtre/tri/page en cours de sélection) pour confirmer que le compteur suit
  maintenant correctement en toutes circonstances ; aucun moyen de compiler/exécuter l'app depuis
  cette session pour le vérifier autrement.

**19/09/2026.** Revirement sur le comportement de « Valider en lot » quand la sélection mélange
plusieurs sources et/ou plusieurs sens — demande explicite de HMAYDA : *« la validation en lot
doit être limitée aux opérations d'une même source qui peuvent être groupées »*, puis correction
plus stricte : *« déjà le fait de permettre de valider en lot des sources différentes ou des sens
opposés ne doit pas être permis, oublier ce que fait submitLot actuellement, et côté backend
également une exception métier doit se lever avec un message clair que l'UI peut lire »*. Décision
arbitrée via `AskUserQuestion` (2 questions, options recommandées retenues les deux fois) :
- **Frontend : bloquer l'action, message clair (recommandé).** L'ancien comportement (regroupement
  automatique de la sélection en plusieurs `LotGroup` par `source__sens`, puis un appel séquentiel
  `POST /rapprochement/process/lot` par groupe via `concatMap`/`toArray`) est entièrement
  abandonné — plus aucun appel backend silencieux multiple pour une seule action utilisateur.
  - Nouveau getter `selectionEstHomogene` (`lotGroups.length <= 1`) vérifié dans `openLotModal()` :
    si la sélection couvre plus d'une combinaison (source, sens), l'ouverture du dialogue est
    bloquée et un toast d'erreur explicite s'affiche (« Sélection hétérogène » — invite à vider la
    sélection et recommencer avec une seule combinaison type/sens à la fois). Aucun appel réseau
    n'est déclenché dans ce cas.
  - `confirmLotValidation()`/`submitLot()` simplifiés pour n'opérer que sur un seul `LotGroup`
    garanti homogène (`this.lotGroups[0]`) — plus de boucle sur plusieurs groupes, requête
    `BatchValidationRequestDto` unique (`transactionSource`/`sens` uniques, cohérent avec la
    structure du DTO backend qui ne pouvait de toute façon jamais porter plusieurs sources/sens à
    la fois).
  - Gestion d'erreur du `subscribe` améliorée : le toast affiche désormais le message réel renvoyé
    par le backend (`err?.error?.message`) au lieu d'un texte générique fixe — sert directement la
    demande d'un « message clair que l'UI peut lire ».
  - **Fichiers** : `recapitulatif.component.ts` (33192 → 33812 octets) et
    `recapitulatif.component.html` (24801 → 24560 octets), vérifiés byte-for-byte après écriture
    (`device_commit_files` + `device_list_dir`).
- **Backend : vérifier que chaque id appartient vraiment à la source/sens déclarée (recommandé)**
  — option retenue par HMAYDA, mais **investigation avant codage** (relecture de
  `BatchValidationRequestDto.java`, `BatchValidationService.java`,
  `ValidationExceptionHandler.java`, `ErrorResponse.java`) a montré qu'**aucun changement de code
  n'est nécessaire aujourd'hui pour Leasing et CMT spécifiquement** :
  - `BatchValidationRequestDto` ne porte qu'un seul `transactionSource` et un seul `sens` pour tout
    l'appel — la structure même du DTO empêche déjà qu'un appel unique porte un mélange ; le risque
    réel était uniquement côté frontend (une action utilisateur silencieusement éclatée en
    plusieurs appels), pas quelque chose que le backend pouvait recevoir comme « mélangé ».
  - Pour Leasing et CMT, chaque `BatchValidationHandler` (`LeasingBatchValidationHandler`/
    `CmtBatchValidationHandler`) interroge exclusivement son propre repository JPA spécifique à la
    source — un id qui n'appartient pas réellement à cette source ne peut donc pas être mal
    attribué en silence : il lève déjà une `ResourceNotFoundException` (message explicite du type
    « Échéance CMT introuvable avec l'ID: X »), déjà mappée par `ValidationExceptionHandler` en
    HTTP 404 avec un `ErrorResponse.message` lisible par l'UI. Ni Leasing ni CMT n'ont de champ
    `sens` variable à recouper (tous deux ignorés par `requireSensIfNeeded`).
  - **Risque réel différé** : un `sens` déclaré par erreur qui ne correspond pas au sens réel d'une
    entité ne deviendra pertinent que pour les sources qui ont un `sens` propre et pas encore
    migrées vers `BatchValidationHandler` (Virement, Chèque, MiseADisposition,
    VirementInternational, LCN) — à traiter au moment de leur migration, pas avant (éviter le code
    mort/prématuré).
  - Ce constat a été communiqué explicitement à HMAYDA avant d'écrire le code frontend ; pas encore
    de confirmation explicite reçue en retour sur ce point précis au moment de cette entrée.
- **Prochaine étape** : HMAYDA build/teste manuellement le changement frontend (aucune capacité de
  compilation depuis cette session) — voir aussi l'entrée 19/09/2026 (suite) ci-dessus pour un bug
  de compteur de sélection remonté et corrigé pendant ce test. Confirmation attendue sur le constat
  backend ci-dessus avant de le considérer clos. Choix de la source suivante parmi les 22 restantes
  toujours en attente (voir entrées 15/09/2026 ci-dessous).

**15/09/2026.** Extraction d'un `BatchValidationHandler` pour la validation en lot Leasing + CMT
dans `BatchValidationService` — voir Décisions arbitrées pour la mesure de risque et le choix de
périmètre :
- **`BatchValidationHandler`** (nouvelle interface,
  `validation/services/handlers/BatchValidationHandler.java`) : `source()` (clé
  d'enregistrement) + `processOne(Long id, BatchValidationRequestDto requestDto)`. Chaque
  implémentation est un `@Component` Spring, découvert automatiquement (injection de
  `List<BatchValidationHandler>` dans `BatchValidationService`, converti en
  `Map<TransactionSource, BatchValidationHandler>` dans le constructeur). Ajouter une source migrée
  ne demande plus de toucher `BatchValidationService` — juste une nouvelle implémentation dans le
  module de cette source.
- **`CmtBatchValidationHandler`** (`cmt/echeances/services/`) et **`LeasingBatchValidationHandler`**
  (`leasing/echeancesLeasing/services/`) : reprennent **à l'identique** (copier-coller, aucune
  logique changée) le corps des anciens `case CMT ->` / `case LEASING ->` du switch — même
  résolution de l'entité parente via repository, même construction du DTO, même appel à
  `ICmtValidationService`/`ILeasingValidationService`.
- **`BatchValidationService.processOne`** : vérifie d'abord le registre
  (`handlerRegistry.get(requestDto.transactionSource())`) et délègue si une entrée existe, sinon
  retombe sur le switch historique. Les `case CMT`/`case LEASING` et les champs/imports qui ne
  servaient qu'à eux (`echeanceCMTRepository`, `cmtValidationService`, `echeanceLeasingRepository`,
  `leasingValidationService`, DTOs et entités CMT/Leasing) sont retirés — plus aucune référence
  résiduelle (vérifié par relecture complète du fichier après édition). Les 20 autres `case`
  restent inchangés dans le switch, à migrer un par un plus tard.
- **Portée volontairement limitée** : `ValidationController` (endpoints unitaires
  `process/echeance-cmt`/`process/echeance-leasing`) et les services `CmtValidationService`/
  `LeasingValidationService` eux-mêmes ne sont pas touchés — seul le dispatch de la validation
  **en lot** change de mécanisme. Comportement fonctionnel strictement identique pour l'appelant
  (même requête, même réponse) — seule la façon dont `BatchValidationService` route en interne
  a changé.
- **Non fait, sciemment** : le champ `tvaAutoriserSaisieLibre`-style qui permettrait au futur
  dialogue unitaire générique de savoir qu'une source n'accepte pas la TVA en montant libre
  (seuls Leasing/CMT le font aujourd'hui, voir Décisions arbitrées) — pas ajouté avant qu'une
  source concernée soit réellement migrée, pour ne pas introduire de code mort.
- **Fichiers** : 3 nouveaux (`BatchValidationHandler.java`, `CmtBatchValidationHandler.java`,
  `LeasingBatchValidationHandler.java`) + `BatchValidationService.java` modifié — écrits via
  `device_commit_files`, taille en octets revérifiée par `device_stage_files` après écriture
  (`device_bash` toujours indisponible).
- **Prochaine étape** : validation par HMAYDA en usage réel de « Valider en lot » sur des
  opérations Leasing et CMT (même checkpoint que pour tout changement backend cette session — pas
  de moyen de compiler depuis cette session). Une fois confirmé, migrer les 20 sources restantes
  vers `BatchValidationHandler` une par une (chacune une simple extraction du corps de son `case`
  existant dans une nouvelle classe, sans changement de comportement).

**15/09/2026 (suite).** Nettoyage CMT post-pilote (même déroulé que le nettoyage Leasing du
14/09, périmètre confirmé avec HMAYDA : CMT uniquement, pas de balayage large) :
- **Ancien écran de validation CMT dédié retiré** (`echeances-cmt.component.ts/.html`, décision
  identique à Leasing : seul le récap validera CMT désormais) :
  - Retrait de l'entrée de route `echeances-cmt` dans `etats-des-encours.routes.ts`.
  - Retrait du lien de menu (« CMT » sous Décaissements) et de l'entrée `breadcrumbMap`
    correspondante dans `les-encours.component.ts`.
  - Endpoint backend de validation (`ValidationService.echeanceCmtToRapprochement` →
    `POST /rapprochement/process/echeance-cmt`) conservé intact : le récap l'appelle à
    l'identique.
- **Nettoyage complémentaire** — retrait de `getEncoursCMT()` / `exportEncoursCMTPdf()`, devenus
  orphelins après la suppression de l'écran ci-dessus (vérifié : aucun autre écran du module CMT
  ne les appelait — ni `cmt-list-actifs`, ni `cmt-form`, ni `cmt-list-archived`, ni
  `cmt-list-en-attente-de-ml`, ni `tableau-amortissement-cmt`, ni `BankAccueilAggregatorService`
  qui utilise une agrégation différente) :
  - **Frontend** (`EcheanceCmtService.ts`) : méthodes `getEncoursCMT()` et `exportEncoursCMTPdf()`
    retirées, imports `EncoursCMTRequestDTO`/`EncoursCMTResponseDTO` retirés.
  - **Backend** : endpoints `GET /encours` et `GET /encours/export/pdf` retirés du
    `EcheanceCMTController` ; `getEncoursCMTForBanque()`/`exportEncoursCMTPdf()` retirés de
    `IEcheanceCMTService` et de `EcheanceCMTService` ; le champ `banqueService` (`IBanqueService`,
    injecté par constructeur) devenu inutilisé une fois ces méthodes parties, retiré avec son
    import et celui de l'entité `Banque` (vérifié par relecture complète du fichier : aucun autre
    usage). Contrairement à Leasing, `EcheanceCMTService` n'avait pas de helper privé dédié type
    `sumMontantTTC()` — les deux méthodes retirées utilisaient directement les builders de
    `EcheanceCMTSpecification` (`byCreditBanqueId`/`outstandingUntil`/`byCreditDesignationLike`/
    `byMontant`), **laissés en place** comme pour Leasing (builders de `Specification` partagés,
    décision séparée à faire seulement si HMAYDA le demande explicitement).
  - Les 6 fichiers modifiés (3 backend, 3 frontend) vérifiés byte-for-byte (stage + taille en
    octets) après écriture.
- **Contrainte d'environnement aggravée** : `device_bash` confirmé cassé par une mise à jour
  Windows du 08/09 (message d'erreur explicite du bridge cette fois, plus une simple
  déconnexion passagère) — donc aucun `mv`/`rm` réel possible depuis cette session, sur toute la
  durée du projet tant que ce n'est pas résolu côté plateforme.
- **Suppression physique restante** (8 fichiers, copie seule possible — pas de move réel) — à la
  demande explicite de HMAYDA (« grouper les fichiers à supprimer dans un dossier "deleted" »),
  une **copie** de chacun a été déposée dans `G-Banque/deleted/` (mêmes chemins relatifs que
  l'original, préfixés `Frontend/`/`Backend/`) pour revue ; les fichiers originaux listés
  ci-dessous restent en place et doivent être supprimés manuellement par HMAYDA (le dossier
  `deleted/` peut ensuite être supprimé aussi, une fois les originaux confirmés partis) :
    1. `Frontend/src/app/portail/etatsDesEncours/views/lesEncours/decaissement/echeances-cmt/`
       (dossier complet : `.component.ts/.html/.scss/.spec.ts`)
    2. `Frontend/src/app/portail/cmt/dtos/EncoursCMTRequestDTO.ts`
    3. `Frontend/src/app/portail/cmt/dtos/EncoursCMTResponseDTO.ts`
    4. `Backend/src/main/java/staport/finance/gBanque/cmt/echeances/dtos/EncoursCMTRequestDTO.java`
    5. `Backend/src/main/java/staport/finance/gBanque/cmt/echeances/dtos/EncoursCMTResponseDTO.java`
- **Prochaine étape** : HMAYDA supprime manuellement les originaux listés ci-dessus (et le dossier
  `deleted/` une fois vérifié), confirme le build/test CMT en usage réel, puis choix de la source
  suivante parmi les 22 restantes pour la migration `BatchValidationHandler` (voir entrée
  15/09/2026 ci-dessus).

**14/09/2026 (suite 4).** Fusion des dialogues de validation unitaire Leasing + CMT en un seul,
suite à une question de HMAYDA (« pourquoi deux dialogues, ça ne peut pas être un seul pour
toutes ? ») — voir Décisions arbitrées pour les options proposées et le choix retenu :
- **`UnitValidationConfig<TDetail>`** : interface décrivant tout ce que le dialogue générique doit
  savoir sur une source — `fetchDetail` (appel du `GET .../{id}`), `toCommonDetail` (extraction des
  champs communs `dateEcheance`/`observation`/`libelle`/`montantTTC`/`recapLabel`/`recapValue`
  depuis le DTO spécifique à la source), `tvaInitialValue`/`tvaLocked` (politique TVA),
  `buildAndSubmit` (construction de la requête spécifique + appel du bon endpoint de validation),
  et les libellés de dialogue (`confirmHeader`, `successDetail`, `fetchErrorDetail`).
- **`unitValidationConfigs`** : `Map<TransactionSource, UnitValidationConfig>` avec une entrée
  Leasing et une entrée CMT (reprenant exactement le comportement des deux dialogues distincts
  qu'elles remplacent — aucun changement fonctionnel, juste une seule implémentation partagée).
  Ajouter une source ne demande plus qu'une entrée dans cette Map, jamais un nouveau `<p-dialog>`
  ni un nouveau jeu de méthodes `open*/close*/confirm*/submit*`.
- Un seul jeu d'état/méthodes générique remplace les deux : `displayUnitValidationModal`,
  `unitValidationForm`, `unitValidationDetail`, `canValidateUnitairement()`,
  `openUnitValidation()`, `closeUnitValidationModal()`, `isUnitValidationInvalid()`,
  `confirmUnitValidation()`, `submitUnitValidation()`. Un seul `<p-dialog>` dans le template,
  avec des ids `recapUnit*` (au lieu de `recapLeasing*`/`recapCmt*`).
- Décision backend non remise en cause : les `*ValidationService` par source restent séparés
  (raison différente — éviter les cycles d'injection Spring —, sans rapport avec ce refactor
  frontend).
- Fichiers modifiés : `recapitulatif.component.ts` et `recapitulatif.component.html` (mêmes
  fichiers que le pilote CMT ci-dessous) — écrits via `device_commit_files`, taille en octets
  revérifiée par `device_stage_files` après écriture (`device_bash` toujours indisponible).

**14/09/2026 (suite 3).** Pilote CMT mené backend + frontend, même patron que Leasing (voir note
de session précédente) :
- **Ambiguïtés tranchées avec HMAYDA avant tout code** (règle non négociable), via 3 questions à
  choix + recommandation :
  1. Politique TVA CMT → saisie libre/optionnelle retenue (option recommandée, confirmée par
     lecture du code existant — voir Décisions arbitrées).
  2. Interface `IEcheanceCMTService` → HMAYDA choisit de l'ajouter (contrairement à ma
     recommandation de suivre le patron existant sans interface) — décision honorée intégralement.
  3. Portée → plan complet confirmé : backend (`GET by-id`) → extension du récap pour CMT → retrait
     de l'ancien écran CMT une fois le récap validé en usage réel.
  - Extension non demandée mais explicitement décidée par HMAYDA au passage : rouverture de la TVA
    Leasing (verrouillée à 20 % depuis le pilote initial) — devient éditable, 20 % par défaut,
    « aucun » ou montant libre possibles. Documentée dans Décisions arbitrées comme réversant la
    décision du 14/09/2026 (pilote initial).
- **Backend** :
  - Nouvelle interface `IEcheanceCMTService` (`cmt/echeances/services/interfaces/`), même forme que
    `IEcheanceLeasingService` — `EcheanceCMTService` l'implémente (`@Override` ajouté aux 10
    méthodes existantes), `EcheanceCMTController` et `CreditMoyenTermeService` basculés pour
    injecter l'interface plutôt que la classe concrète.
  - Nouvelle méthode `getEcheanceCMTById(Long id)` sur `EcheanceCMTService`/`IEcheanceCMTService`
    (patron `findById(...).orElseThrow(...)` → `echeanceCmtMapper.toResponseDto(...)`, identique à
    `getEcheanceLeasingById`) + nouvel endpoint `GET /echeances-cmt/{id}` sur
    `EcheanceCMTController`. Zéro changement aux 22 endpoints unitaires existants ni au module
    `recapitulatif` en lecture.
- **Frontend** :
  - `EcheanceCmtService.getEcheanceCMTById(id)` ajouté (patron `getEcheanceLeasingById`).
  - Écran Récapitulatif étendu : bouton « Valider » unitaire désormais actif aussi sur les lignes
    CMT (`isCmt()`, `openCmtValidation()`), nouveau `<p-dialog>` de validation CMT reprenant
    exactement les champs et le comportement de l'ancien écran dédié (TVA libre via
    `app-tva-input`, dates, libellé, observation), `submitCmtValidation()` appelle
    `validationService.echeanceCmtToRapprochement(...)` (méthode déjà existante, non modifiée).
  - TVA Leasing du récap : `tva: [{ value: {...}, disabled: true }]` → `tva: [{ tauxTva: 20,
    montantTva: null }]` (éditable, 20 % par défaut) suite à la décision ci-dessus.
- **Contrainte d'environnement inchangée** : `device_bash` toujours indisponible (panne du 08/09).
  Chaque fichier modifié écrit via `device_commit_files` puis revérifié par un nouveau
  `device_stage_files` (taille en octets comparée à l'original) — pas de diff cmd.exe cette
  session, les dossiers profonds (`Backend/.../gBanque`, `Frontend/src/app`) étant maintenant
  connectés directement, ce qui a aussi permis de lire les fichiers sans passer par `_review/`.
- **Ancien écran CMT dédié** (`echeances-cmt.component.ts/.html`) **pas encore retiré** — même
  checkpoint que pour Leasing : en attente que HMAYDA valide le récap CMT en usage réel (build +
  test manuel) avant la phase de nettoyage (retrait route/menu/breadcrumb, puis recherche de code
  mort orphelin — `getEncoursCMT`/`exportEncoursCMTPdf` notamment, à vérifier une fois l'écran
  retiré, comme cela a été fait pour Leasing).
- **Prochaine étape** : validation du pilote CMT par HMAYDA en usage réel, puis nettoyage de
  l'ancien écran CMT (même déroulé que le nettoyage Leasing ci-dessous), puis choix de la source
  suivante parmi les 22 restantes.

**14/09/2026 (suite 2).** Nettoyage Leasing post-pilote + résolution de la contrainte d'environnement :
- **Dossiers profonds connectés** : à la demande de HMAYDA (le contournement `_review\` + copy
  cmd.exe n'était plus acceptable), deux dossiers connectés directement depuis l'app desktop en
  plus de `G-Banque` : `Frontend/src/app` et `Backend/src/main/java/staport/finance/gBanque`. Le
  plafond de profondeur de `device_stage_files` (7 dossiers sous la racine connectée) ne bloque
  plus la lecture des fichiers de sources individuelles (`cmt/`, `leasing/`, etc.), qui étaient à
  9-11 niveaux sous `G-Banque`. `device_bash` reste indisponible (panne du 08/09, toujours non
  résolue côté plateforme) — donc toujours pas d'exécution de commande directe sur la machine de
  HMAYDA, mais la lecture/écriture de fichiers individuels via `device_stage_files`/
  `device_commit_files` n'a plus besoin du contournement manuel.
- **Ancien écran de validation Leasing supprimé** (décision explicite de HMAYDA : seul le récap
  validera Leasing désormais) :
  - Retrait de l'entrée de route `echeances-leasing` dans `etats-des-encours.routes.ts`.
  - Retrait du lien de menu (« Leasing » sous Décaissements) et de l'entrée `breadcrumbMap`
    correspondante dans `les-encours.component.ts`. Les deux fichiers vérifiés byte-for-byte
    (stage + diff) après écriture.
  - Le endpoint backend de validation (`ValidationService.echeanceLeasingToRapprochemnt` →
    `POST /rapprochement/process/echeance-leasing`) est conservé intact : le récap l'appelle à
    l'identique, aucune suppression backend nécessaire pour la validation elle-même.
- **Nettoyage complémentaire (même session, suite à la consigne explicite de HMAYDA : « tout ce
  qui n'est plus appelé est du code mort à supprimer »)** — retrait de `getEncoursLeasing()` /
  `exportEncoursLeasingPdf()`, devenus orphelins après la suppression de l'écran ci-dessus (aucun
  autre écran du module Leasing ne les appelait — vérifié : ni les vues `contratsLeasing`, ni
  `contratCadre`, ni `organismesLeasing`, ni le tableau de bord `BanqueController`/
  `BankAccueilAggregatorService` qui utilise une agrégation différente) :
  - **Frontend** (`EcheanceLeasingService.ts`) : méthodes `getEncoursLeasing()` et
    `exportEncoursLeasingPdf()` retirées, imports `EncoursLeasingRequestDTO`/
    `EncoursLeasingResponseDTO` retirés.
  - **Backend** : endpoints `GET /encours` et `GET /encours/export/pdf` retirés du
    `EcheanceLeasingController` ; `getEncoursLeasingForBanque()` / `exportEncoursLeasingPdf()`
    retirés de `IEcheanceLeasingService` et de `EcheanceLeasingService` ; le helper privé
    `sumMontantTTC()` (JPA Criteria, plus appelé que par ces deux méthodes) retiré avec eux, ainsi
    que le champ `entityManager` et `banqueService` (`IBanqueService`) du service — devenus inutilisés
    une fois ces méthodes parties (vérifié par grep sur tout le fichier avant suppression : aucun
    autre appelant). Constructeur du service mis à jour en conséquence.
  - Les 4 fichiers modifiés (2 backend, 2 frontend) vérifiés byte-for-byte (stage + diff) après
    écriture.
  - `EcheanceLeasingSpecification.byContratBanqueId/outstandingUntil/
    byContratOrganismeLeasingId/byContratNumeroContrat/byMontant` **laissés en place** : ce sont
    des builders de prédicats sur une classe `Specification` partagée — les retirer est une
    décision séparée, à faire seulement si HMAYDA le demande explicitement (risque/bénéfice moins
    clair que pour un endpoint REST ou une méthode de service).
  - **Suppression physique restante** (5 éléments, impossible depuis cette session —
    `device_bash`, seul moyen de `rm` à distance, reste indisponible ; tous déjà totalement
    déréférencés donc sans risque fonctionnel à rester temporairement sur disque) — à supprimer
    manuellement par HMAYDA :
    1. `Frontend/src/app/portail/etatsDesEncours/views/lesEncours/decaissement/echeances-leasing/`
       (dossier complet : `.component.ts/.html/.scss/.spec.ts`)
    2. `Frontend/src/app/portail/leasing/contratsLeasing/dtos/encours-leasing-request.dto.ts`
    3. `Frontend/src/app/portail/leasing/contratsLeasing/dtos/encours-leasing-response.dto.ts`
    4. `Backend/src/main/java/staport/finance/gBanque/leasing/echeancesLeasing/dto/EncoursLeasingRequestDTO.java`
    5. `Backend/src/main/java/staport/finance/gBanque/leasing/echeancesLeasing/dto/EncoursLeasingResponseDTO.java`
- **Suppression physique confirmée par HMAYDA** (les 5 éléments listés ci-dessus) — vérifiée par
  `device_list_dir` : dossier `echeances-leasing/` absent, les deux DTOs `Encours*` absents côté
  Frontend et Backend.
- **Prochaine étape** : pilote CMT, dans une nouvelle session (prompt de démarrage donné à HMAYDA
  ci-dessous), même patron que Leasing.

### Note pour la prochaine session (CMT)

- **Commit/push du pilote Leasing** : à faire par HMAYDA lui-même (`git add -A && git commit &&
  git push` depuis `C:\Users\Hp\Desktop\G-Banque`) — `device_bash` reste indisponible, donc
  aucune commande git ne peut être exécutée depuis une session cloud sur sa machine.
- **Repérage CMT déjà fait** (pas besoin de re-explorer) :
  - Backend : `Backend/src/main/java/staport/finance/gBanque/cmt/echeances/` — `controllers/
    EcheanceCMTController.java`, `services/EcheanceCMTService.java` (⚠️ **pas d'interface**
    `IEcheanceCMTService`, contrairement à Leasing — à trancher avec HMAYDA : en ajouter une pour
    la cohérence, ou suivre le patron existant tel quel), `services/CmtValidationService.java` +
    `services/interfaces/ICmtValidationService.java` (validation, probablement déjà réutilisable
    par le récap comme pour Leasing), DTOs `EcheanceCMTResponseDto`, `CmtTransactionRequestDto`/
    `CmtTransactionResponseDto` (déjà référencés par `validation.service.ts` côté frontend via
    `echeanceCmtToRapprochement`).
  - Frontend : `Frontend/src/app/portail/cmt/services/EcheanceCmtService.ts` (service à
    compléter, patron `getEcheanceLeasingById`), écran de validation actuel probablement
    `Frontend/src/app/portail/etatsDesEncours/views/lesEncours/decaissement/echeances-cmt/
    echeances-cmt.component.ts/.html` (à confirmer — nommage parallèle à `echeances-leasing`).
  - Point d'ambiguïté connu à trancher avec HMAYDA avant d'écrire du code : la politique de TVA
    CMT (verrouillée à un taux fixe comme Leasing, ou différente ?) — ne pas supposer, lire
    `CmtValidationService.java` d'abord puis demander confirmation si le comportement observé
    n'est pas évident.

**14/09/2026 (suite).** Pilote Leasing du refactoring applicatif mené de bout en bout, backend
puis frontend (Option A — voir Décisions arbitrées ci-dessus) :
- **Backend** : un seul ajout, `GET /echeances-leasing/{id}` → `EcheanceLeasingResponseDTO`
  (`EcheanceLeasingController` → `EcheanceLeasingService` → `IEcheanceLeasingService`, patron
  `findById(...).orElseThrow(...)` identique à `toggleStatus`/`updateEcheance`). Zéro changement
  aux 22 endpoints unitaires existants, à `v_recapitulatif_encours` ou au module `recapitulatif`
  en lecture.
- **Frontend** : `TableSelectionState<T, K>` généralisé avec une clé composite (`K` par défaut
  `number`, rétrocompatible avec les 24 écrans existants) pour supporter la sélection multi-lignes
  du récap, où `entityId` seul n'est pas unique entre sources. Écran Récapitulatif étendu avec
  case à cocher par ligne, bandeau de sélection, bouton « Valider » (Leasing uniquement pour ce
  pilote) et « Valider en lot » (généralisé à toutes les sources — découpe automatique par
  (source, sens), un appel `/process/lot` par groupe, `concatMap` séquentiel). La validation
  unitaire Leasing réutilise le patron exact de l'écran dédié (TVA verrouillée 20 %, mêmes effets
  de bord post-succès).
- **Contrainte d'environnement** : `device_bash` toujours indisponible (panne du 08/09) — chaque
  fichier modifié a été vérifié par un `copy` cmd.exe vers `_review/` puis diff, avant et après
  écriture, pour garantir l'absence de régression malgré l'impossibilité de compiler/tester
  directement sur la machine de HMAYDA depuis cette session.
- **Prochaine étape** : valider le pilote en usage réel (build + test manuel côté HMAYDA), puis
  répliquer le patron sur les 23 sources restantes, dans l'ordre qui reste à définir avec HMAYDA.

**14/09/2026.** Chantier repo unique + GitHub mené de bout en bout : backup, fusion `git subtree`
de Backend (444 commits) et Frontend (291 commits) dans le dépôt racine, vérification absence de
secrets dans l'historique, correction du bug `graphify-out` (848 fichiers suivis par erreur,
gitignore corrigé), push vers `github.com/HmaydaAbdo/g-banque` (privé), build Backend + Frontend
vérifiés depuis le dépôt fusionné. `device_bash` indisponible tout du long (panne connue depuis le
08/09) — chaque commande donnée puis exécutée manuellement par HMAYDA, résultat revérifié avant la
suivante. Prochaine étape : choisir la source pilote pour le refactoring applicatif.

**12/09/2026.** Première discussion de l'idée : écran de récap unique pour la validation, sens
porté par la ligne plutôt que par un choix utilisateur générique. Risques identifiés : RBAC
(la route n'est plus la frontière de permission), `docs/guide-utilisateur/finance.md` à réécrire
sur la partie validation, distinction entre "unifier la validation" et "unifier le CRUD des 24
entités sources" (hors-scope ici).
