# Briefing interne — avance leasing (mis à jour le 20/10/2026)

*Pour HMAYDA — ce qui a été fait, où c'est documenté, ce qui reste.*

## 1. Le besoin

Des contrats de leasing démarrent par une **avance**, réglée par chèque, LCN ou virement national. Le mois suivant, les mensualités commencent. Les montants doivent rester cohérents et l'avance ne doit **jamais être comptée deux fois**.

## 2. Décisions prises ensemble

| Sujet | Décision |
|---|---|
| Montant | Avance **comprise dans le prix d'achat** et dans la **durée** (phases = période − 1 mois) |
| Représentation | **Échéance n°1**, à la date de début ; mensualités dès M+1 |
| Définition de l'avance | Elle **est** le chèque / LCN / virement choisi dans le contrat : son **montant devient l'avance** (réglé ou en cours) |
| Commission / TVA | Pas de commission sur l'avance ; TVA 20 % comme les mensualités |
| Valeur résiduelle | Toujours 1 % du **prix d'achat complet** |
| Endettement | **Aucune contrainte** imposée : il résulte du calcul (avance + mensualités + VR) |
| Rattachement | Dans le **wizard du contrat**, côté leasing uniquement (plus d'écran de rattachement séparé) |
| Statut | L'avance **suit le statut de son moyen de paiement** (option C : aucune copie, aucun risque de désynchronisation) |
| Transaction | **Une seule** : celle du moyen de paiement ; son libellé et son observation mentionnent l'avance et le contrat |
| Annulation du moyen de paiement | L'avance redevient PRÉVUE et réapparaît dans les encours de leasing |
| Encours | L'avance rattachée est **exclue** du leasing : le moyen de paiement la représente |
| Contrats existants | **Non touchés** |
| Verrou | Règle conservée : une échéance payée (moyen réglé) verrouille modification et suppression du contrat |

## 3. Évolutions du 20/10/2026 (retours de HMAYDA)

- **Assistant de droite supprimé** (panneau d'aide + plan en direct) : le wizard revient à une seule colonne.
- **Champ d'avance affiché par défaut** à l'étape *Détails généraux* : type → **liste déroulante à chargement progressif** recherchable par numéro (pages de 20, pour la performance) des chèques / LCN / virements existants, réglés ou en cours.
- **Mention dans la transaction** : libellé et observation de la transaction du moyen de paiement (et du moyen lui-même) indiquent « Avance … échéance n°1 du contrat N° x ». Si le moyen est réglé, la transaction existante est mise à jour ; s'il est en cours, la mention est posée sur lui et se retrouve à la validation.
- **Changement de moyen** : par modification du contrat (tant que non réglé) ; l'ancien retrouve ses textes d'origine. La suppression du contrat retire aussi la mention.

## 4. Ce qui a été construit

**Base de données** — migration **V16** : `avance_ttc` sur le contrat, lien vers le moyen de paiement sur l'échéance (un moyen ne règle qu'une seule avance), vue `v_echeance_leasing_moyen`. Vue des encours adaptée.

**Backend**
- Génération du plan avec avance, contrôles de saisie, pas de commission sur l'avance.
- Le contrat reçoit `avanceMoyenPaiement` (type + identifiant) ; le service d'avance contrôle le moyen (décaissement, non annulé, non déjà utilisé), pose le montant et la mention.
- Endpoints : `GET /echeances-leasing/avance/{contratId}` et `GET /echeances-leasing/avance/moyens-disponibles` (paginé, recherche par numéro, par type).
- Lecture des chèques / LCN / virements par des façades dédiées ; mise à jour des textes de la transaction via `ITransactionService.modifierTextes` (qui propage au moyen de paiement).
- Garde-fous : l'avance ne se valide pas comme un loyer et ne se bascule pas à la main.

**Frontend** — section « Avance (facultative) » avec liste déroulante paginée, récapitulatif enrichi, carte « Avance » en lecture seule dans le tableau d'amortissement.

**Tests** — 14 tests d'intégration sur PostgreSQL réel (plan, statut qui suit le moyen, mention sur le moyen **et** sur la transaction unique, absence de doublon dans les encours, refus, modification, suppression, liste paginée). Front : build OK, 183 tests OK.

## 5. Documentation mise à jour (4 niveaux)

| Niveau | Fichier | Contenu |
|---|---|---|
| Fonctionnel | `docs/modules/README.md` (chapitre 7) | Définition, règles 20 à 29, cycle de vie, droits, wizard |
| Direction | `docs/direction/README.md` (chapitre 7) | Valeur apportée |
| Utilisateur | `docs/guide-utilisateur/finance.md` | Création avec avance, « Régler l'avance », messages |
| Accès | `docs/rbac/README.md` | Endpoints, conditions d'affichage |

Guide à présenter : `presentation/guide-avance-leasing.md`.

## 6. État et points de vigilance

- **Non commité** à la rédaction de ce briefing (les changements du 20/10 sont dans l'arbre de travail).
- **Non vérifié visuellement** : la liste déroulante à chargement progressif n'a pas été essayée dans le navigateur.
- Le montant de l'avance est **copié depuis le moyen de paiement à la création** : si le montant d'un chèque en cours est modifié ensuite dans son module, l'avance du contrat ne suit pas (à revoir si ce cas existe).
- Le statut de l'avance est **calculé en base** : toute nouvelle requête sur le statut d'une échéance passe par le champ `statutPaiement` (lecture).
- Supprimer un chèque / LCN / virement rattaché fait réapparaître l'avance dans les encours de leasing.
