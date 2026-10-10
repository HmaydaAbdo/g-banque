# TODO — G-Banque

## En attente

### Chantier — Finalisation de la CI et mises à jour de dépendances (relevé le 10/10/2026)

**État.** Pipeline `ci.yml` fusionnée sur `main`. Dernier run sur `main` : backend OK, frontend OK, **sécurité en échec**
(Docker Hub refuse le téléchargement des images gitleaks/Trivy : `toomanyrequests`, pull anonyme limité), donc les jobs
d'images n'ont pas tourné. Aucune image publiée sur Docker Hub pour l'instant. Seule la branche `main` est conservée.

**À faire.**
1. **Rate limit Docker Hub (correctif poussé le 10/10/2026, à vérifier sur le run de `main`)** : `docker/login-action` placé `docker/login-action` (identifiants Docker Hub déjà configurés) avant
   les `docker run` gitleaks et Trivy du job `security`, et avant le build du job `images`. Relancer la CI sur `main`
   et vérifier le push de `gbanque-backend` / `gbanque-frontend` (`latest` + sha). Confirmer que le secret
   `DOCKERHUB_TOKEN` existe sous ce nom exact.
2. **Fonctions GitHub indisponibles sur l'offre gratuite d'un dépôt privé (constaté le 10/10/2026)** : protection de
   branche et CodeQL (code scanning). L'analyse statique de sécurité n'est donc pas en place ; à reprendre avec une
   offre payante, ou avec un outil ne dépendant pas de GitHub (ex. Semgrep CLI dans la CI).
   **Protection de `main`** : impossible aujourd'hui (le 10/10/2026, l'API renvoie 403 : dépôt privé sur offre gratuite,
   la protection de branche exige GitHub Pro/Team ou un dépôt public). Fait à la place : suppression automatique des
   branches fusionnées activée. Règle d'équipe en attendant : jamais de push direct sur `main` hors correctifs de CI, PR
   obligatoire avec la CI verte. À reprendre si le dépôt passe en offre payante (checks exigés : `Backend`, `Frontend`,
   `Sécurité`, `Image Docker (backend|frontend)`).
3. **Mises à jour de dépendances écartées le 10/10/2026** (anciennes PR Dependabot fermées). À reprendre une par
   une comme chantiers décidés, jamais en fusion automatique :
   - Angular 17 → 20/22 (`router`, `platform-browser-dynamic` ; toute la famille `@angular/*`, CLI, TypeScript, Node) ;
   - PrimeFlex 3 → 4 (et PrimeNG en cohérence) ;
   - Spring Security 7 (impose Spring Boot 4, lié au chantier « Sécurité des images et dépendances backend ») ;
   - Java 17 → 25 / image Maven Temurin 26 (PR 5 et 6) ;
   - actions GitHub en version majeure : `checkout` 7, `setup-node` 6, `build-push-action` 7, `login-action` 4,
     `setup-buildx-action` 4 (faible risque, à valider sur la CI) ;
   - mises à jour mineures/patch groupées Maven et npm (faible risque).
4. **Dependabot supprimé (10/10/2026)** : il ouvrait une PR (et 2 workflows CI/CodeQL) par écosystème, trop de bruit pour
   suivre une seule CI. Mises à jour de dépendances faites à la main, quand on le décide. Si on le réactive : un seul
   écosystème, rythme mensuel. La suppression automatique des branches fusionnées reste activée sur le dépôt.

### Chantier — Stratégie de tests, tous niveaux, tous modules (demandé le 09/10/2026)

**Constat (09/10/2026).** Backend : 884 classes, **1 seul test** (`GBanqueApplicationTests`, chargement du contexte sur
MySQL Testcontainers) → couverture de lignes 9,7 % (hors tout comportement métier). Frontend : 61 specs, dont **58 en
échec** (squelettes « should create » générés par le CLI, sans providers) → couverture 9 %. Le job `frontend` de la CI
les exécute en `continue-on-error` (temporaire).

**À faire.**
1. Réparer ou remplacer les 58 specs frontend en échec, puis retirer `continue-on-error` du job `frontend` (`ci.yml`).
2. Écrire les tests **de tous les niveaux pour tous les modules**, backend et frontend, selon `conventions/testing.md`
   (unitaire, intégration, composant, bout en bout), module par module dans l'ordre du chapitrage de la doc :
   socle d'accès → comptes bancaires → produits financiers → marchés → moyens de paiement → transactions →
   charges appliquées → anomalies → rapprochement → états des encours → transverse.
   Priorité : calculs financiers (agios, commissions, encours), `@PreAuthorize` (allowed/denied), handlers du registre
   (validation, annulation, synchronisation d'édition).
3. Introduire le bout en bout (Playwright) sur les parcours critiques ; décision d'outil à valider (architecture).
4. Relever `DIFF_COVER_MIN` (`ci.yml`) par paliers 0 → 30 → 50 → 70 → 80 %.
5. Démarrage du contexte de test : ~190 s en local (Docker Desktop, ~100 tables avec Envers). Mesurer sur le runner ;
   si trop long, envisager un contexte partagé entre tests d'intégration et des tranches (`@DataJpaTest`).
6. Mettre à jour `conventions/testing.md` au fil des décisions (exemples de fixtures/builders une fois posés).

### Chantier — Sécurité des images et dépendances backend (relevé le 09/10/2026)

**Constat.** Scan Trivy de l'image backend (HIGH/CRITICAL corrigeables) : **67 vulnérabilités, dont 12 critiques**,
toutes dans les dépendances Java (Tomcat embarqué 21, Netty, Jackson, Spring Framework 6.1.10 avec RCE/XSS/DoS, …).
Cause : Spring Boot **3.3.1** (hors support). L'OS de l'image est propre ; l'image frontend est propre (0).

**À faire.**
- Décision d'architecture à valider : montée de version Spring Boot (3.3.x le plus récent → 3.5.x), impact sur
  `spring-boot-starter-oauth2-authorization-server`, Hibernate/Envers, springdoc, MapStruct. Vérifier aussi pourquoi
  `spring-webflux` et Netty sont dans le classpath.
- Ensuite, passer le scan Trivy backend en bloquant (`trivy-exit-code: "1"` dans `images` et `exit-code: "1"` dans `security`).
- Épingler les images de `docker-compose.yml` encore en `:latest` (prometheus, grafana, phpmyadmin, mysql-cron-backup)
  sur les versions réellement déployées (à relever sur le serveur) ; sinon un `pull` change la version sans prévenir.
- Évaluer un JRE réduit (`jlink`) pour l'image backend (293 Mo aujourd'hui) — gain ~100 Mo, à tester.
- **Clé JWT** : un ancien commit (`f7cb002e`, `SecurityConfig.java`) contient une clé JWT en dur, identique à celle de `env.dev`.
  Vérifier qu'elle n'est pas celle de la production ; sinon la renouveler (invalide les sessions en cours). Le commit est mis en liste d'autorisation dans `.gitleaks.toml`.
- Cadrer une CSP nginx compatible PrimeNG (en-têtes de base déjà posés).

### Chantier — Déploiement continu par SSH (en attente de l'accès SSH au serveur)

Cf. `docs/infra/ci-cd.md` § « Passage au déploiement continu ». Préalables : secrets Docker Hub configurés, protection de
`main`, `docker-compose` de production basé sur `image:` (Docker Hub). Workflow `cd.yml` à écrire quand l'accès est disponible.

### Chantier — Agios unifiés : calculé vs facturé, tout calculé (lancé le 09/10/2026)

> **Plan détaillé : `CHANTIER-AGIOS-UNIFIES.md`** (racine du dépôt) — à lire en premier dans la session dédiée.

**Objectif (HMAYDA, 09/10/2026).** Fusionner `agios` (saisie manuelle), `agios_trimestriel` (périodes
calculées) en un seul objet métier portant un **montant calculé** et un **montant facturé**, pour
mesurer en permanence l'exactitude du calcul. On **garde** `agios_journalier` (calcul jour par jour).
Plus aucune création manuelle d'agios « à la main » : tout type d'agios doit être **calculé**, donc
tout type non couvert aujourd'hui doit être couvert, **cas par cas**. Les agios calculés alimentent
les encours **en temps réel**. Séance de cadrage avec l'équipe Finance le 10/10/2026.

**Constat de départ (dump du 09/10/2026, schéma d'analyse `gbanque_analysis`).**
- 77 lignes `agios` (4 992 792,83 MAD) ; 46 sont des « arrêtés de trimestre » dont le type n'est pas
  lisible dans le libellé ; 10 factoring, 10 intérêts débiteurs, 8 escompte LCN, 3 préfinancement.
- Calcul qui colle à la banque (± quelques centimes) : facilité de caisse BMCI T1/T2, CIH T1-T3, SAHAM
  T1-T3, CDM T2 ; préfinancement BMCE T2, CDM T2, SAHAM T3, CIH T3.
- Calcul qui s'écarte : facilité de caisse AWB (T1 122 647 calculé / 118 207 facturé ; T2 74 392 /
  71 696) et CDM T1 (20 174 / 19 941) ; BP (une seule ligne « intérêts débiteurs » agrégée, 79 675 en
  T1 et 106 627 en T2, que le calcul ne reproduit pour aucun type).
- **Non calculés du tout** : escompte LCN (8 lignes), petits frais fixes (AWB 110,00 ×8, 718,98,
  678,27 ; SAHAM 6,41 ; CDM 5,34), frais d'engagement factoring (AWB), factoring BMCI hors mars et
  septembre (10 lignes saisies sur 8 mois), droits constatés (aucun total calculé ne retrouve une ligne
  bancaire). Périodes non couvertes : T4/2025 ; préfinancement et droits constatés du T1/2026.
- Les 13 périodes clôturées pointent vers un `agios_definitif_id` qui n'existe plus : les agios
  générés à la clôture ont été supprimés et remplacés par les lignes réelles de la banque.
- Factoring BMCI mars : période restée `OUVERT` (211 825,80 calculé / 208 449,22 facturé).
- **Anomalies de données à corriger avant migration** : la transaction 63 (AWB, 85 784,02, rapprochement 2)
  référence l'agios 4 (SAHAM) au lieu de l'agios 16 (AWB, « INTERET DEBITEURS », PAYE sans transaction) ;
  l'agios 4 est donc référencé par deux transactions.
- Dépendances à traiter : 75 transactions de source `AGIOS` (référent = id de `agios`, à conserver ou
  remapper), vue `v_recapitulatif_encours` (branche 14), 69 fichiers backend et 34 fichiers frontend
  mentionnent « agios », validation unifiée (`AgiosValidationService`, handlers), PDF/Excel, docs
  (4 niveaux + rbac), audit, métriques.

**Plan.**

0. **Séance Finance (10/10)** — décisions à obtenir (voir la liste de questions ci-dessous). Livrable :
   le catalogue des types d'agios par banque et la règle de calcul de chacun.
1. **Préparation des données** — typer les 77 lignes (banque, type, période) avec Finance ; produire la
   matrice banque × type × période (✔ couvert, ◐ écart, ✘ non calculé) : elle sert de jeu de recette.
   Corriger les deux anomalies ci-dessus (accord explicite avant toute écriture en base).
2. **Couvrir les types manquants, un par un**, par ordre d'enjeu financier, chacun comme un nouveau
   calculateur (interface `AgiosCalculateur` existante + `TypeAgios`), avec sa règle validée par Finance :
   factoring (BMCI mensuel rétroactif + AWB frais d'engagement), intérêts agrégés BP, écart AWB facilité
   de caisse (taux, date de valeur ?), droits constatés (facturés ou non ?), escompte LCN (nouveau
   calculateur, formule par remise), frais fixes (paramétrage par banque), rétroactif T4/2025 et T1/2026
   là où les données le permettent.
3. **Modèle unifié** — **décision d'architecture attendue avant tout code** (voir options). Script de
   migration versionné dans `sql/`, conservation des liens avec les transactions, vue des encours,
   entité/API/frontend, validation unifiée, docs (modules, direction, guide-utilisateur, rbac).
4. **Temps réel dans les encours** — l'encours d'agios d'une période en cours = le calculé cumulé jusqu'à
   aujourd'hui, remplacé par le facturé dès qu'il est saisi (à confirmer avec Finance).
5. **Recette** — comparer calculé/facturé sur tout l'historique migré ; seuil d'écart accepté fixé avec
   Finance ; retrait de la saisie manuelle une fois tous les types couverts.

**Options du modèle unifié (à trancher).**
- **A (recommandée)** : une ligne par (banque, type, période[, marché]) avec `montant_calcule`,
  `montant_facture`, `ecart` ; les débits réellement passés par la banque restent des lignes de
  facturation rattachées (plusieurs par période, p. ex. CIH T3 en a 4) et c'est chacune qui est validée
  dans le rapprochement. Deux tables, mais fidèle à la réalité bancaire.
- **B** : une seule table, une ligne par débit bancaire (type + période renseignés), le calculé étant
  sommé à la volée depuis `agios_journalier` — un seul objet, mais pas de colonne « calculé » stockée.

**Décisions de HMAYDA (09/10/2026, réponses aux questions de cadrage).**
- Un montant facturé par type et par période ; certains types sont mensuels (factoring), d'autres
  trimestriels. Types calculés aujourd'hui : facilité de caisse, factoring, préfinancement, droits
  constatés ; à ajouter : escompte LCN (règle de calcul à découvrir le 10/10).
- Écart : on enregistre le **calculé** et le **facturé** ; **à la validation**, on précise le montant
  exact facturé (le calculé est affiché à l'utilisateur) ; l'**écart** (en MAD et en %) n'est **jamais
  enregistré**, il est recalculé à l'affichage.
- Encours : affichent le **cumul calculé à ce jour** ; le **facturé remplace le calculé** dès qu'il est
  saisi (c'est ce que la banque débite réellement).
- TVA : on garde l'existant. Petits frais fixes : aucune règle connue à ce jour.
- Les deux lignes `PREVU` (BMCI 300 000, AWB 23 251,30) : à remplacer par le calcul détaillé, **dernière
  étape du chantier**.
- Rétroactif : historique disponible seulement depuis 2026 ; les trimestres antérieurs ne sont pas
  calculables.
- Modèle : **option A** retenue, et **le calcul journalier est conservé** partout où il s'applique.

**Points d'alimentation de `agios` repérés dans le code (09/10/2026).** Quatre chemins créent des
agios, pas deux : (1) la saisie manuelle (`POST` `AgiosController.createAgios`) ; (2) la clôture d'une
période (`AgiosOrchestrateurService.cloturerTrimestre`) ; (3) **`ChargesFixesScheduler`** (dernier jour du
mois, 07h00) qui crée une ligne `PREVU` par charge fixe de type `AGIOS` — aujourd'hui une seule
configuration : BMCI « AGIOS UTILISATION LIGNE ASMN FACTORING », 300 000,00, émission le 1er du mois,
période ajoutée au libellé (c'est l'origine de la ligne `PREVU` factoring de BMCI) ; (4) les
services de validation/synchronisation qui modifient le statut et les dates d'une ligne existante.
Aucune création par import ou script SQL. Le chantier doit donc aussi retirer le type `AGIOS` de la
configuration des charges fixes (écran « Charges fixes bancaires — Configuration »).

**Questions pour Finance (10/10).**
1. La banque débite-t-elle un montant par type et par période, ou agrège-t-elle (BP) ? Comment ventiler ?
2. Quels types existent par banque (facilité de caisse, préfinancement, droits constatés, factoring,
   escompte LCN, commissions d'engagement, frais fixes, autres) ? Fréquence mensuelle ou trimestrielle ?
3. Droits constatés : facturés ? dans quelle ligne ? Règle de calcul exacte (base, taux, jours).
4. Escompte LCN : formule (jours remise → échéance, taux, commission fixe, TVA) et où vit le taux.
5. Petits frais fixes (110,00 AWB…) : montant, périodicité, par compte ou par banque ?
6. Écart facturé/calculé : seuil acceptable (MAD ou %) ? Qui l'analyse ? Que fait-on d'un écart durable
   (corriger le taux, enregistrer l'écart) ?
7. Encours en temps réel : cumul à ce jour ou projection à la fin de période ? Calculé tant que non
   facturé, facturé ensuite ?
8. TVA : le facturé est-il saisi en TTC ? Les 1 502,81 (CIH T2/T3) sont-ils une commission fixe ?
9. Les deux lignes `PREVU` (300 000 BMCI estimation factoring 09/2026 ; 23 251,30 AWB) : à remplacer
   par le calculé ?
10. Rétroactif : jusqu'où recalculer (T4/2025 ? T1/2026) ?

### Saisie libre du montant de TVA en validation (demandé le 07/09/2026)

Dans le formulaire de validation (rapprochement), pour toutes les sources, permettre à
l'utilisateur de ne choisir aucun des taux de TVA prédéfinis de la liste déroulante et de saisir
directement le montant de TVA à la place.

**Architecture arbitrée avec HMAYDA le 07/09/2026.** Le contrat de validation porte désormais deux
champs mutuellement exclusifs, et la conversion taux → montant reste intégralement serveur.

1. **Contrat API** : sur chaque DTO unitaire, le champ de taux (`tva` / `tvaDeductible`, `Double`,
   en pourcentage) est renommé **`tauxTva`** et devient nullable ; un champ **`montantTva`**
   (`BigDecimal`, nullable) est ajouté. Exactement l'un des deux est renseigné — XOR vérifié dès la
   couche Jakarta par un `@AssertTrue`, puis par `TvaResolver`. Le renommage est fait dans la
   foulée, source par source : le nom `tvaDeductible` pour un pourcentage disparaît.
2. **Validation en lot** : la saisie libre y est **interdite** — un montant fixe appliqué à N
   opérations de montants différents n'a pas de sens comptable. Le lot ne transporte qu'un taux, que
   le service de chaque module convertit sur le TTC de chaque opération. Conséquence heureuse :
   **`BatchValidationRequestDto` ne change pas de forme** (`ids: number[]` conservé), et
   `processOne` se contente d'alimenter `tauxTva` en laissant `montantTva` à `null`.
3. **Persistance** : inchangée. `Transaction` ne porte que `tvaDeductible` / `tvaCollectee`
   (`BigDecimal`, des montants) ; le taux n'est persisté nulle part et ne le sera pas.
   **Aucune migration BDD.** Si un besoin d'audit du taux apparaît, la colonne s'ajoutera plus tard
   sans rien casser.
4. **Répartition des responsabilités** : `TvaResolver` (`validation/utils`) porte les règles — XOR,
   bornes du taux, `0 <= montantTva <= TTC` — et délègue à `TVAUtils` pour l'extraction depuis le
   TTC. Il est appelé par le **service** de validation de chaque module, qui passe le montant résolu
   au mapper. Les mappers ne calculent plus rien : ils posent le montant sur `tvaDeductible` ou
   `tvaCollectee` selon le sens, redevenant conformes à `conventions/architecture.md`
   (« Mapper : conversion uniquement, jamais de logique métier »).
5. **Aucun calcul côté client.** Le frontend transmet le taux choisi, jamais un montant qu'il aurait
   dérivé d'un taux. Ce point n'est pas cosmétique : une transposition TypeScript du calcul
   `TVA = TTC − TTC/(1+taux/100)` en `number` diverge d'un centime sur **10,6 % des montants au
   taux 20 %** (mesuré sur 400 000 montants ; 0 écart à 10 % et 14 %), la TVA y valant TTC/6 et
   tombant sans arrêt sur des demi-centimes exacts. Le serveur garde le calcul, le problème
   n'existe pas.

**Périmètre.** Backend : 20 mappers + 20 DTO de requête (dont 11 `record` à constructeur positionnel
— Commission, Agios, FraisBancaire, ImpotEtTaxe, Restitution, OperationErrone, Provision, Decompte,
Impayee, CMT, Leasing) + les 20 services de validation + `BatchValidationService.processOne`.
Frontend : 30 écrans unitaires dupliquant le même bloc TVA + le modal de lot partagé. Le backend ne
compte **qu'un seul fichier de test** : chaque source doit être vérifiée manuellement.

**Terminé le 07/09/2026.** Les 20 sources backend, les ~30 écrans Angular et le modal de lot sont
tous migrés vers `tauxTva`/`montantTva`. Vérifié par compilation ciblée (`javac`, tous les fichiers
touchés) et `tsc --noEmit` (frontend entier) — voir mémoire `project_gbanque_tva_saisie_libre` pour
le détail source par source. **Reste à faire manuellement par HMAYDA**, non automatisable depuis
cet environnement : un `ng build` réel (pour attraper d'éventuelles erreurs `strictTemplates` que
`tsc --noEmit` ne détecte pas) et un test fonctionnel bout-en-bout d'au moins une source par
famille de comportement (XOR strict, TVA optionnelle CMT/Leasing, saisie en lot).

**Migration source par source.** Le renommage `tva`/`tvaDeductible` → `tauxTva` change le JSON de
l'endpoint unitaire : pour une source donnée, le backend et ses écrans se déploient donc ensemble.
Les sources sont en revanche indépendantes les unes des autres — une source migrée et une source
non migrée coexistent sans problème, y compris dans `processOne`.

Pour chaque source : DTO (`tauxTva` + `montantTva` + `@AssertTrue`) → mapper (signature
`toEntity(..., BigDecimal montantTva)`, plus aucun calcul ; pour les mappers MapStruct — CMT,
Leasing — passer le montant en `@Context`) → service (appel `TvaResolver.resoudre`) → branche
`processOne` (`dto::setTauxTva`) → écrans Angular (`<app-tva-input>`, payload, récapitulatif).

**Avancement.**

- [x] `TvaResolver` (`Backend/.../validation/utils/TvaResolver.java`)
- [x] Composant Angular partagé `<app-tva-input>` + `saisie-tva.model.ts`
      (`Frontend/.../views/lesEncours/shared/tva-input/`)
- [x] **Chèque** (décaissement + encaissement) — backend et les 2 écrans
- [x] Virement, LCN, MiseADisposition, VirementInternational
- [x] Prélèvement, Commission, Agios, FraisBancaire, ImpotEtTaxe
- [x] Impayée, Restitution, OpérationErronée, Provision
- [x] Décompte, DroitConstaté, Factoring, Préfinancement
- [x] CMT, Leasing (mappers MapStruct — `@Context`)
- [x] Modal de lot : `autoriserSaisieLibre=false` + renommage `BatchValidationRequestDto.tvaDeductible` → `tauxTva`
- [x] Suppression des `tvaOptions` résiduels et de la mention `%` dans les récapitulatifs
- [x] Swagger et `docs/modules` : rien à modifier — Swagger est généré depuis les DTO (aucune
      annotation `@Schema` ne décrivait l'ancien champ) ; `docs/modules/README.md` décrit un
      résultat fonctionnel (« calcule la TVA associée ») toujours exact, jamais le mécanisme de
      saisie (liste déroulante vs montant libre), donc rien n'y est devenu faux.

### Documentation à mettre à jour — validation unifiée (reporté depuis le 19/09/2026)

Décision HMAYDA du 19/09/2026 : ne pas mettre à jour `docs/modules`, `docs/direction`,
`docs/guide-utilisateur` au fil des sources migrées (validation unifiée + annulation + édition
d'une transaction déjà validée, tous les trois par le même mécanisme de registre de handlers),
pour éviter un rattrapage partiel/incohérent en cours de refactoring — à faire en une fois, pour
toutes les sources, une fois le chantier applicatif stabilisé. `docs/rbac/README.md` est déjà à
jour (mis à jour dans les mêmes changements que le code, comme l'exige CLAUDE.md).

Périmètre du rattrapage :
- `docs/modules/README.md` — chapitre Validation : décrire le mécanisme actuel (point d'entrée
  unique `POST /rapprochement/process`, un handler par source) au lieu de l'ancien (endpoints par
  source)
- `docs/direction/README.md` — chapitre Validation : mentionner la consolidation comme bénéfice
  (moins de code, plus simple à faire évoluer)
- `docs/guide-utilisateur/*.md` — écrans de validation/annulation/édition de transaction déjà
  validée : décrire le dialogue générique partagé (mêmes champs pour valider et pour modifier)

Voir `REFACTORING-VALIDATION-UNIFIEE.md` (journal complet, suites 8 à 11) et mémoire
`project-validation-unifiee-recap` pour le détail technique.

**Ajouts au périmètre du rattrapage (21/09/2026)**, à intégrer dans le même passage documentaire :
- `docs/modules/README.md` — chapitre Leasing et chapitre CMT : décrire la nouvelle possibilité
  d'éditer une échéance `PREVU` via une page dédiée (`/portail/leasing/echeances/edit/:id` et
  `/portail/cmt/echeances/edit/:id`, réservée aux administrateurs système), à la place du dialogue
  historique sur le tableau d'amortissement ; mentionner qu'elle est aussi atteignable depuis le
  récapitulatif unifié.
- `docs/guide-utilisateur/admin.md` — même écran : capture/description du nouveau parcours
  d'édition (avant : dialogue sur place : après : navigation vers une page dédiée, retour en arrière
  au clic sur Annuler/Enregistrer).
- `docs/modules/README.md` — chapitre Mise à disposition : le bug (`type` toujours ENCAISSEMENT
  quel que soit l'écran) est corrigé côté frontend le 21/09/2026, mais **les enregistrements créés
  avant cette date restent en base avec `type=ENCAISSEMENT`** alors qu'ils sont tous en réalité des
  décaissements — décision à prendre avec HMAYDA sur une éventuelle correction de données
  existantes (hors périmètre frontend, nécessite un script/migration côté backend).
- `docs/modules/README.md` et `docs/guide-utilisateur/*.md` — chapitre Récapitulatif : les filtres
  « Libellé » et « Observation » du récapitulatif unifié ont été fusionnés en un seul champ
  « Libellé / Observation » le 21/09/2026 (un seul terme, cherché sur les deux colonnes à la fois,
  OR insensible à la casse) — à refléter dans la description des filtres de cet écran.
- `docs/modules/README.md` et `docs/guide-utilisateur/*.md` — chapitre Récapitulatif : critères de
  recherche restaurés le 21/09/2026 pour retrouver la couverture des anciens écrans séparés :
  émetteur (Virement, Virement international, Chèque, LCN, Mise à disposition) et organisme
  (Leasing) sont désormais aussi cherchés par le champ « Libellé / Observation » (sans nouveau
  champ visible) ; deux nouveaux filtres déroulants « Motif Commission » et « Motif Impôt/Taxe »
  apparaissent quand la source correspondante est sélectionnée. Voir
  `REFACTORING-VALIDATION-UNIFIEE.md` (journal) pour le détail technique. **Rappel déploiement** :
  `sql/v_recapitulatif_encours.sql` doit être ré-exécuté sur la base pour que les 3 nouvelles
  colonnes existent — sans ça le backend échoue au premier appel de l'écran Récapitulatif.

### Bascule du statut d'un virement — garde-fou contre le retour en « en cours » (relevé le 09/10/2026)

**Constat.** Le virement n° 008981 (id 1814, BP, AGNI BTP, 30 000 MAD) est resté « En cours » alors
que la transaction id 250 du rapprochement n° 5 de BP (janvier 2026) pointe sur lui
(`referent = 1814`). Cas unique : les 1 584 autres transactions de type Virement pointent toutes
vers un virement « Réglé ».

**Cause la plus plausible** (non prouvable : l'historique d'audit ne commence que le 21/07/2026).
`VirementService.toggleVirementStatus` bascule `REGLE` ↔ `ENCOURS` sans vérifier si une transaction
de rapprochement pointe sur le virement : quelqu'un a pu le repasser en cours à la main. Le chemin
d'annulation de validation (`VirementReversalHandler`) ne colle pas avec le cas 250 : il remet le
virement en cours mais la transaction est alors supprimée.

À faire (décisions de HMAYDA attendues avant toute modification) :

1. **Corriger la donnée** du virement 1814 : passer à `REGLE` et reprendre
   `date_operation` / `date_valeur` de la transaction 250 (19/01/2026). En attente d'accord
   explicite — consigne antérieure « ne rien toucher en base » pour l'historique.
2. **Garde-fou métier** : refuser le retour `REGLE` → `ENCOURS` par la bascule lorsqu'une
   transaction référence le virement, avec un message clair (le retour en cours passe alors par
   l'annulation de la validation dans le rapprochement). Changement de règle métier : à valider,
   puis documenter dans `docs/modules`, `docs/guide-utilisateur` et `docs/rbac`
   (chapitre Virements).
3. Vérifier si les autres moyens de paiement disposant d'une bascule manuelle
   (virements internationaux, etc.) ont le même défaut.

### Refonte visuelle — écrans États des encours (reporté le 20/09/2026)

HMAYDA juge la mise en page actuelle de « Rapprochement en cours » (et probablement
« Récapitulatifs », même gabarit de tableau dense) pas assez organisée — trop de colonnes, KPI
compressés en une seule ligne de texte plutôt que des cartes.

**Déjà fait (20/09/2026)** : le shell `LesEncoursComponent` — vue par défaut passée à
Récapitulatifs, ancienne sidebar rétractable remplacée par un onglet segmenté compact (dégradé
indigo/violet, ombre colorée, easing « spring »), transition d'entrée `route-enter` sur les deux
vues routées.

**Reste à faire** : refonte du contenu de la page elle-même (carte banque, ligne de KPI, filtres,
tableau). Maquette proposée dans un artifact Design (canvas), à valider avec HMAYDA avant portage
dans le vrai code Angular : https://claude.ai/artifact/7NwaMXVW6Fn8Z6KKdPB6x4 — principe : cartes
KPI au lieu d'une ligne de texte, carte banque réorganisée en blocs, tableau réduit de 11 à 6
colonnes (dates regroupées, libellé+observation fusionnés, débit+crédit fusionnés avec indicateur
de sens, TVA déductible+collectée fusionnées). Non implémenté dans le code — HMAYDA a explicitement
demandé de reporter à une autre session.

Historique complet des sweeps et validations déjà terminés : mémoire `modules_sweep` (et les
mémoires dédiées `project_gbanque_moyens_paiement_sweep`, `project_gbanque_marches_archivage`,
`project_gbanque_funpaiement_sync`, `project_gbanque_champs_non_affiches_audit`), ainsi que
l'historique git.

*Dernier nettoyage : 31/07/2026 — toutes les tâches précédemment listées (sweep de conformité
100%, États des encours, validations locales, scripts SQL du 31/07) ont été confirmées terminées
par HMAYDA.*

*Nettoyage du 07/09/2026 — punch-list de la session close par HMAYDA : normalisation des fins
de ligne (.gitattributes) sur Backend et Frontend ; git init de la racine G-Banque (docs,
conventions, sql, infra, jusque-là non versionnés) ; sql/v_recap_moyens_paiement.sql reconstruit
et versionné pour la première fois (branche CHEQUE vérifiée contre Cheque.java, fix préventif
COLLATE sur source_type) ; javadoc de RecapMoyenPaiementView corrigée (elle affirmait à tort le
script déjà versionné) ; indicateur de tri par défaut sur Récapitulatifs (numero reste un filtre,
jamais une colonne affichée). Détail complet dans l'historique git des 3 dépôts et dans
Claude outputs/email_ecrans_etats_des_encours.md.*
