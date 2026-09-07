# TODO — G-Banque

## En attente

### Saisie libre du montant de TVA en validation (demandé le 07/09/2026)

Dans le formulaire de validation (rapprochement), pour toutes les sources, permettre à
l'utilisateur de ne choisir aucun des taux de TVA prédéfinis de la liste déroulante et de
saisir directement le montant de TVA à la place. Prévu pour une nouvelle session (pas
commencé) — points de départ déjà repérés pour gagner du temps au démarrage :

- **Modal de validation groupée partagé** :
  `Frontend/src/app/portail/etatsDesEncours/views/lesEncours/shared/validation-lot-modal/`
  — dropdown `tvaOptions` (liste de `{label, value}`, valeurs en pourcentage) sur le contrôle
  `tvaDeductible`, actuellement `Validators.required` (pas d'option "aucun").
- **Modal de validation unitaire** : chaque écran d'encours a le sien avec vraisemblablement
  le même pattern — 24 fichiers `.html` identifiés référençant `tvaDeductible`/`tvaCollectee`
  (un par source × sens : chèques, virements, LCN, provisions, opérations erronées, impayées,
  restitutions, commissions, agios, prélèvements, frais bancaires, impôts et taxes, leasing,
  CMT, préfinancement, mise à disposition).
- **Backend** : `tauxTva` référencé dans au moins 20 classes `*TransactionMapper.java` (une
  par source) — aucun enum partagé `TauxTva` trouvé, donc vraisemblablement un champ
  numérique par mapper. Calcul actuel présumé : montant × tauxTva (%) → montant de TVA.

**À trancher avec HMAYDA avant toute implémentation** (architecture du contrat API) : comment
distinguer "taux choisi" de "montant saisi directement" — nouveau champ optionnel dédié au
montant manuel, tauxTva rendu nullable, ou autre — sachant que ça touche potentiellement les
20 mappers backend et les deux familles de formulaires frontend (modal groupé + modaux
unitaires par écran).

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
