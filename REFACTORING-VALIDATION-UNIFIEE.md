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

### Source pilote

**Leasing**, choisi le 14/09/2026 — sens fixe (toujours décaissement), un seul écran existant
(`echeances-leasing.component.ts`), bon candidat pour poser le patron avant les sources à sens
porté par la ligne (Préfinancement/Droit constaté/Factoring).

### Tableau des 24 sources — statut

| Source | Sens | Politique de sens | Endpoint(s) actuel(s) | Écran(s) actuel(s) | Statut |
|---|---|---|---|---|---|
| Virement | D/E | choix utilisateur | `process/{decaissement,encaissement}/virement` | 2 | Non commencé |
| Chèque | D/E | choix utilisateur | `process/{decaissement,encaissement}/cheque` | 2 | Non commencé |
| LCN | D/E | choix utilisateur | `process/{decaissement,encaissement}/lcn` | 2 | Non commencé |
| Mise à disposition | D/E | choix utilisateur | `process/{decaissement,encaissement}/mise-a-disposition` | 2 | Non commencé |
| Virement international | D/E | choix utilisateur | `process/{decaissement,encaissement}/virement-international` | 2 | Non commencé |
| Préfinancement | D/E | porté par la ligne | `process/prefinancement` (D) / `process/prefinancement` (E, à vérifier) | 2 | Non commencé |
| Droit constaté | D/E | porté par la ligne | `process/droit-constate` | 2 | Non commencé |
| Factoring | D/E | porté par la ligne | `process/factoring` | 2 | Non commencé |
| Provision | D/E | porté par l'enregistrement | `process/provision` | 2 | Non commencé |
| Impayé | D/E | porté par l'enregistrement | `process/impayee` | 2 | Non commencé |
| CMT (échéance) | D | fixe | `process/echeance-cmt` + `GET /echeances-cmt/{id}` | 1 (ancien écran encore présent) | **Récap opérationnel (backend + frontend, 14/09/2026) — en attente de validation HMAYDA (build + test manuel) avant retrait de l'ancien écran** |
| Leasing (échéance) | D | fixe | `process/echeance-leasing` | 0 (récap uniquement) | **Pilote fait + ancien écran supprimé (14/09/2026)** |
| Agios | D | fixe | `process/agios` | 1 | Non commencé |
| Commission | D | fixe | `process/commission` | 1 | Non commencé |
| Frais bancaire | D | fixe | `process/frais-bancaire` | 1 | Non commencé |
| Impôt / Taxe | D | fixe | `process/impot-taxe` | 1 | Non commencé |
| Prélèvement | D | fixe | `process/prelevement` | 1 | Non commencé |
| Opération erronée | D | fixe | `process/operation-errone` | 1 | Non commencé |
| Décomptes | E | fixe | `process/decompte` | 1 | Non commencé |
| Restitution | E | fixe | `process/restitution` | 1 | Non commencé |

*Tableau à vérifier/compléter avec HMAYDA (ex. sens réel de Préfinancement/DroitConstaté/
Factoring côté backend, colonnes/filtres par source) avant de s'en servir comme base du
descripteur générique.*

---

## Journal de session

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
