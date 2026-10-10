# TODO — G-Banque

## En attente

### Déploiement en production — PostgreSQL 18, Flyway, environnement unique, sessions par cookies (préparé le 10/10/2026)

Tout est dans `main`. Bascule **en une seule fois** : la production passe de MySQL 8 à PostgreSQL 18 (les sessions en
cours seront fermées, les utilisateurs se reconnectent). G-Banque reste servi en **HTTP** : `AUTH_COOKIE_SECURE=false`.
Procédure détaillée et résultats de la répétition : `docs/infra/migration-postgres.md`.

1. **Geler les saisies** puis exporter la base MySQL de production (phpMyAdmin, comme l'export du 10/10/2026).
2. **`.env` du serveur** = le `.env` local mis à jour le 10/10/2026 : variables `POSTGRES_*` et `PGADMIN_*` (mots de
   passe générés), `DB_HOST=postgres`, `DB_PORT=5432`, `MYSQL_*` et `GRAFANA_*` retirées, nouvelle `JWT_SECRET`.
   Comparer avec `.env.example` : aucune variable manquante.
3. Sur le serveur, base PostgreSQL vierge : `docker compose up -d postgres`, puis
   `python sql/migration-postgres/migrer_vers_postgres.py <export.sql>` (schéma V1 + copie, contrôle des volumes
   intégré, tout ou rien), puis `docker compose up -d --build` (le backend applique V2 sur les données migrées).
   Ne pas copier `docker-data/postgres` depuis un poste Windows (droits refusés par PostgreSQL) : migrer depuis l'export,
   ou restaurer une sauvegarde (`docs/infra/backup-postgres.md`).
4. Le nginx frontal doit servir le frontend et `/gbanque/api` sur la **même origine** et transmettre `Set-Cookie` /
   `Cookie` sans les modifier (comportement par défaut de `proxy_pass`).
5. Vérifier : connexion, rechargement de page (session conservée), Récapitulatif, une recherche par libellé, déconnexion ;
   `docker logs gbanque-backup` → « SQL backup created successfully ».
6. Conserver l'ancienne base MySQL (arrêtée, non supprimée) jusqu'à validation complète par l'équipe Finance.

**Répétition faite le 10/10/2026** sur l'export de production du jour : 102 tables / 55 403 lignes migrées, volumes
identiques, schéma validé, 14/14 contrôles API (dont recherche insensible aux accents), sauvegarde restaurée à l'identique.

**À trancher (données) :**
- 8 lignes `commission` (et leurs révisions d'audit) ont `operation = 'Importation'`, valeur qui n'existe plus dans le
  code : **ouvrir l'une d'elles renvoie une erreur 500** (vérifié, commission 17276) ; la liste s'affiche. Décision
  métier, puis migration Flyway dédiée (accord explicite avant toute écriture).

**Petits défauts relevés :**
- Une URL d'API inexistante renvoie 500 (« No static resource ») au lieu de 404 — `GlobalExceptionHandler`.
- `rapprochement.date_rapprochement` est stockée en binaire (`YearMonth` Java sérialisé, `bytea`) faute de
  convertisseur : illisible en SQL. Ajouter un `AttributeConverter` vers `date` (1er du mois) + migration de conversion.

### Monitoring (Prometheus, Grafana) — retiré du compose le 10/10/2026

Retiré à la demande de HMAYDA (configuration absente du dépôt). Le backend expose toujours `/actuator/prometheus`.
Pour le réintroduire : versionner `monitoring/prometheus.yml` et le provisioning Grafana (sans secret), images épinglées.

### Chantier — Passage en HTTPS (demandé le 10/10/2026, après le déploiement ci-dessus)

G-Banque est servi en HTTP. Passer en HTTPS sur le nginx frontal (certificat : Let's Encrypt si le nom
`staport.dyndns.org` est joignable depuis Internet, sinon certificat interne), redirection HTTP → HTTPS, puis
`AUTH_COOKIE_SECURE=true` dans `.env` (cookies de session et XSRF marqués `Secure`) et `CORS_ALLOWED_ORIGINS` en `https://`.
Ensuite seulement : en-tête `Strict-Transport-Security`, et cadrer une CSP nginx compatible PrimeNG.

### Chantier — Montées de version (à décider une par une, jamais automatiques)

- **Angular 17 → 19+** (toute la famille `@angular/*`, CLI, TypeScript, Node) : 16 CVE HIGH dans `package-lock.json`,
  corrigées seulement à partir d'Angular 19 (scan non bloquant dans le job `security` d'ici là). PrimeNG/PrimeFlex en cohérence.
- **Spring Boot 4** (Spring Framework 7, Jackson 3, Security 7) : décision d'architecture ; retirera les 3 exceptions de
  `.trivyignore`. Retirer alors les propriétés `tomcat.version`, `netty.version`, `jackson-bom.version` du `pom.xml`.
- **Java 17 → 21/25** et image Maven correspondante.
- Mises à jour mineures/patch Maven et npm, par lot validé par la CI.
- Évaluer un JRE réduit (`jlink`) pour l'image backend (~290 Mo) — gain ~100 Mo, à tester.
- Vérifier sur le serveur les versions de Grafana/Prometheus en service : l'épinglage (13.2.3 / v3.15.0) fera une montée
  de version au prochain `pull` si le serveur tourne plus ancien.

### Avertissements du build de production frontend (relevés le 10/10/2026)

- `recapitulatif.component.scss` : 9,59 ko pour un budget d'avertissement de 6 ko — **à 0,4 ko de l'erreur (10 ko)
  qui bloquerait le build**. L'alléger (styles morts, règles communes vers `styles.scss`/tokens).
- Bundle initial : 1,19 Mo pour 1 Mo. Charger les écrans et modules PrimeNG à la demande (lazy loading des routes).
- (Corrigé le 10/10/2026 : page `landing`, budget composant relevé à 6 ko, cf. `conventions/Frontend.md`.)

### Chantier — Stratégie de tests, tous niveaux, tous modules (demandé le 09/10/2026)

État au 10/10/2026 : backend 9 tests (contexte + 8 tests de session), frontend 72 tests verts (couverture lignes ~37 %,
surtout des tests « should create »). Le schéma de test est construit par Flyway et validé par Hibernate.

1. Écrire les tests **de tous les niveaux pour tous les modules**, backend et frontend, selon `conventions/testing.md`
   (unitaire, intégration, composant, bout en bout), module par module dans l'ordre du chapitrage de la doc :
   socle d'accès → comptes bancaires → produits financiers → marchés → moyens de paiement → transactions →
   charges appliquées → anomalies → rapprochement → états des encours → transverse.
   Priorité : calculs financiers (agios, commissions, encours), `@PreAuthorize` (allowed/denied), handlers du registre
   (validation, annulation, synchronisation d'édition).
2. Remplacer progressivement les specs frontend « should create » par de vrais tests de comportement.
3. Introduire le bout en bout (Playwright) sur les parcours critiques ; décision d'outil à valider (architecture).
4. Relever `DIFF_COVER_MIN` (`ci.yml`) par paliers 0 → 30 → 50 → 70 → 80 %.
5. Mettre à jour `conventions/testing.md` au fil des décisions (exemples de fixtures/builders une fois posés).

### Chantier — Déploiement continu par SSH (en attente de l'accès SSH au serveur)

Cf. `docs/infra/ci-cd.md` § « Passage au déploiement continu ». Préalables : secrets Docker Hub configurés,
`docker-compose` de production basé sur `image:` (Docker Hub). Workflow `cd.yml` à écrire quand l'accès est disponible.

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

### Saisie libre du montant de TVA en validation — recette fonctionnelle (code terminé le 07/09/2026)

Le code (20 sources, ~30 écrans, modal de lot) est terminé et le build de production passe désormais en CI.
**Reste à faire par HMAYDA :** un test fonctionnel bout-en-bout d'au moins une source par famille de comportement
(XOR strict taux/montant, TVA optionnelle CMT/Leasing, saisie en lot interdite). Détail : historique git et mémoire
`project_gbanque_tva_saisie_libre`.

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
  `REFACTORING-VALIDATION-UNIFIEE.md` (journal) pour le détail technique. La vue est désormais
  appliquée automatiquement par Flyway (`R__v_recapitulatif_encours.sql`) au premier démarrage
  du backend après déploiement.

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
