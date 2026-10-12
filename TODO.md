# TODO — G-Banque

## En attente

### Mise en production — PostgreSQL 18, Flyway, environnement unique, sessions par cookies (en cours, 10/10/2026)

Serveur Windows, dossier `C:\apps\G-Banque` (`docker-compose.yml` + `.env`, images Docker Hub `:latest`).
**Fait le 10/10/2026 :** ancienne stack MySQL arrêtée (données MySQL conservées pour retour arrière), base PostgreSQL
restaurée depuis la sauvegarde du 10/10 et passée dans le volume nommé `gbanque-postgres-data` (contrôle conforme : 104 tables,
2 vues, Flyway 2, 55 407 lignes).

**Prochain déploiement (images `:latest` du 11/10/2026 et suivantes).** Le backend applique au démarrage les migrations **V3** (transaction
4956 réaffectée à l'opération erronée 12), **V4** (`rapprochement.date_rapprochement` en vraie date) et **V5** (dates du virement 1814).
Vérifier d'abord la sauvegarde du jour (`./backups/postgres`), puis dans les logs : « Successfully applied … now at version v5 ».

**Reste à faire :**
1. Supprimer sur le serveur l'ancien dossier `docker-data\postgres` et `gbanque-init.sql.gz` (contrôle du volume validé).
2. Le nginx frontal doit servir le frontend et `/gbanque/api` sur la **même origine** et transmettre `Set-Cookie` /
   `Cookie` sans les modifier (comportement par défaut de `proxy_pass`).
3. Vérifier : connexion, rechargement de page (session conservée), Récapitulatif, une recherche par libellé,
   déconnexion ; `docker logs gbanque-backup` → « SQL backup created successfully ».
4. Conserver l'ancienne base MySQL (conteneur `gbanque-mysql` arrêté, dossier `docker-data\mysql`) jusqu'à validation
   complète par l'équipe Finance, puis la supprimer ; supprimer aussi les copies de `gbanque-init.sql.gz`.
5. Si des saisies ont été faites dans l'ancienne application après l'export de 04h12 : refaire la migration avec un
   nouvel export (`docs/infra/migration-postgres.md`).

### Monitoring (Prometheus, Grafana) — retiré du compose le 10/10/2026

Retiré à la demande de HMAYDA (configuration absente du dépôt). Le backend expose toujours `/actuator/prometheus`.
Pour le réintroduire : versionner `monitoring/prometheus.yml` et le provisioning Grafana (sans secret), images épinglées.

### Chantier — Passage en HTTPS (demandé le 10/10/2026, après le déploiement ci-dessus)

G-Banque est servi en HTTP. Passer en HTTPS sur le nginx frontal (certificat : Let's Encrypt si le nom
`staport.dyndns.org` est joignable depuis Internet, sinon certificat interne), redirection HTTP → HTTPS, puis
`AUTH_COOKIE_SECURE=true` dans `.env` (cookies de session et XSRF marqués `Secure`) et `CORS_ALLOWED_ORIGINS` en `https://`.
Ensuite seulement : en-tête `Strict-Transport-Security`, et cadrer une CSP nginx compatible PrimeNG.

### Chantier — Montées de version (à décider une par une, jamais automatiques)

*Décisions HMAYDA du 11/10/2026 : chantiers à ouvrir dans des sessions dédiées (trop longs pour une session de rattrapage).*

- **Backend — Spring Boot 3.5 → 4.1, puis Java.** Le support gratuit de Boot 3.5 s'est arrêté le 30/06/2026 (plus de correctifs de
  sécurité : d'où les versions Tomcat/Netty/Jackson épinglées à la main). Cible **4.1** (support jusqu'au 31/07/2027 ; 4.0 : 31/12/2026).
  Spring Framework 7, Jackson 3, Hibernate 7 (Envers), Security 7, springdoc 3 ; garder Java 17 pour ce lot. Retire les 3 exceptions de
  `.trivyignore` et les propriétés `tomcat.version`, `netty.version`, `jackson-bom.version` du `pom.xml`. Ensuite, lot séparé **Java 17 → 21 ou 25**
  (pom, images Maven/JRE, CI).
- **Frontend — Angular 17 → 22 par étapes** (comme pour erp-rh), un lot vert par marche. Le code livré est exposé : failles dans
  `@angular/core`, `common`, `compiler`, `router` (XSS i18n/SVG, fuite du jeton XSRF, déni de service), corrigées en 20.3 / 21.2 et au-delà.
  Node 20 est en fin de support (CI et `Frontend/Dockerfile`) : passer à Node 22 ou plus avec Angular 21+. PrimeNG 17 → 21+ à traiter
  à part : nouveau système de thème (le `styles.scss` de 35 Ko et les surcharges de tableaux sont le vrai coût), contrôle visuel écran par écran.
- Mises à jour mineures/patch faites le 11/10/2026 (npm : verrou rafraîchi dans les plages, 90 → 71 vulnérabilités ; Maven : openpdf 1.3.43, jacoco 0.8.15, maven-compiler-plugin 3.16.0). Reste : **PostgreSQL JDBC 42.7.14** (publié le 07/10/2026, règle des 2 semaines : à passer à partir du 21/10/2026) ; les autres dépendances sont déjà à la dernière version de leur série. OpenPDF 2.x/3.x (changement de paquets) et le reste des vulnérabilités npm relèvent des montées majeures ci-dessous.
- **JRE réduit (`jlink`) — évalué le 11/10/2026, non adopté (décision d'architecture à valider).** Essai : build jlink (modules détectés par `jdeps` + `jdk.crypto.ec`, `jdk.unsupported`, `jdk.management`…) sur `alpine:3.24` avec `fontconfig` et `font-dejavu` : **image 203 Mo au lieu de 298 Mo (−95 Mo, −32 %)** ; JRE de 65 Mo au lieu de 135 Mo. Démarrage vérifié sur une base vierge (Flyway jusqu'à V4, validation Hibernate, 0 erreur). Non vérifié : génération PDF et envoi de courriel dans le conteneur (nécessite une session). Contreparties : un `Dockerfile` plus long, une liste de modules à maintenir (un module oublié ne se voit qu'à l'exécution), l'image de base n'est plus `eclipse-temurin` (mises à jour de sécurité du JRE à suivre soi-même : reconstruire l'image à chaque correctif Temurin). Essai non conservé dans le dépôt (une vingtaine de lignes : `jdeps --print-module-deps` puis `jlink --strip-debug --no-man-pages --compress=2`) ; à refaire si la taille d'image devient un problème (déploiement sur un lien lent).

### Avertissements du build de production frontend (relevés le 10/10/2026)

- `recapitulatif.component.scss` : 7,89 ko pour un budget d'avertissement de 6 ko (9,59 ko avant le retrait des règles
  mortes le 10/10/2026 ; erreur à 10 ko). Aller sous 6 ko suppose de découper ce composant (cartes de source, modale
  « Ajouter », indicateurs) — à traiter avec la refonte visuelle des États des encours.

### Chantier — Stratégie de tests, tous niveaux, tous modules (demandé le 09/10/2026)

État au 11/10/2026 : backend 134 tests (session, migrations V4 et V5, 404, génération PDF, commission « Importation », et la
bascule de statut sur 19 types d'opération × 6 scénarios, audit compris) ; frontend 72 tests verts (couverture lignes ~37 %, surtout des tests
« should create »). Le schéma de test est construit par Flyway et validé par Hibernate. Reste notamment : tests de la bascule des échéances
de leasing et de CMT (graphe de dépendances lourd à fabriquer), `@PreAuthorize` (allowed/denied) de chaque contrôleur, calculs financiers.

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
