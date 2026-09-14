# TODO — G-Banque

## En attente

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
