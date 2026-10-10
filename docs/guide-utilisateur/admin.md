# Guide utilisateur — Administrateur système

> Ce guide couvre les fonctionnalités accessibles au profil **Administrateur système** (`SYSTEM_ADMIN`).
> Un administrateur système a accès à l'intégralité de G-Banque, mais son rôle principal est la gestion des accès.

---

## Se connecter

1. Ouvrir G-Banque dans le navigateur.
2. Saisir votre **identifiant** et votre **mot de passe**.
3. Cliquer sur **Se connecter**.

Après connexion, vous êtes redirigé vers le tableau de bord Finance (en tant qu'administrateur, vous avez accès à tous les modules).

> **Compte verrouillé ?** Après 5 tentatives échouées consécutives, l'accès est bloqué 15 minutes. Aucune action n'est nécessaire — le décompte est automatique.

> **Durée de la session.** Une fois connecté, vous restez connecté jusqu'à **8 heures** (une journée de travail), même si vous rechargez la page ou ouvrez G-Banque dans un autre onglet. Au-delà, le message **« Session expirée — Votre session a expiré, veuillez vous reconnecter. »** s'affiche et vous êtes renvoyé vers la page de connexion : reconnectez-vous pour continuer. Le même message apparaît si votre compte a été désactivé entre-temps.

---

## Consulter mes notifications

*Accès : icône cloche dans la barre de navigation, en haut de l'écran — disponible pour tout utilisateur connecté*

Une pastille rouge sur la cloche indique le nombre de notifications non lues. Cliquer sur la cloche
ouvre la liste de vos notifications (échéances à venir sur les lignes fonctionnelles, contrats de
leasing ou crédits moyen terme, et autres informations diffusées par G-Banque).

### Marquer une notification comme lue

Cliquer sur une notification dans la liste, puis confirmer. Le compteur de notifications non lues se
met à jour.

### Déclencher une notification publique (réservé administrateur système)

Fonctionnalité exposée uniquement via l'API (aucun écran dédié à ce jour) : permet de diffuser une
notification à tous les utilisateurs, en dehors des déclenchements automatiques (échéances).

---

## Gérer les départements

*Accès : sidebar → Administration → Départements*

### Créer un département

1. Cliquer sur **Nouveau**.
2. Saisir le **nom** du département (convention : majuscules sans espace, ex. `FINANCE`, `TRESORERIE`).
3. Saisir une **description** (optionnelle).
4. Valider.

> **Important** : le nom du département pilote les droits d'accès dans toute l'application. Une fois créé et utilisé, renommer un département sans mettre à jour les appartenances peut bloquer des collaborateurs.

### Modifier un département

1. Cliquer sur l'icône **Modifier** sur la ligne du département.
2. Changer le nom ou la description.
3. Valider.

### Supprimer un département

1. Cliquer sur l'icône **Supprimer**.
2. Confirmer la suppression.

> **Suppression impossible** si des collaborateurs appartiennent encore à ce département. Retirez d'abord leurs appartenances depuis la gestion des utilisateurs.

### Rechercher un département

Utiliser le champ de recherche en haut de la liste — filtre instantané sur le nom et la description.

---

## Gérer les utilisateurs

*Accès : sidebar → Administration → Utilisateurs*

### Créer un compte collaborateur

1. Cliquer sur **Nouveau**.
2. Saisir l'**identifiant** (unique) et le **mot de passe** (minimum 8 caractères).
3. Valider — le compte est créé actif, sans droits de département par défaut.
4. Aller sur la fiche du collaborateur pour lui attribuer ses droits.

### Attribuer des droits à un collaborateur

1. Cliquer sur **Gérer** sur la ligne du collaborateur (ou naviguer vers sa fiche).
2. Dans la section **Appartenance aux départements**, sélectionner un département et un rôle (`MEMBRE` ou `DIRECTEUR`).
3. Cliquer sur **Assigner**.

> Un collaborateur peut appartenir à plusieurs départements avec des rôles différents. Répéter l'opération autant de fois que nécessaire.

### Modifier le rôle d'un collaborateur dans un département

Réassigner le collaborateur au même département avec le nouveau rôle — l'ancien rôle est automatiquement remplacé (pas de doublon).

### Retirer un collaborateur d'un département

Sur la fiche du collaborateur, dans le tableau des appartenances, cliquer sur **Supprimer** sur la ligne du département concerné.

### Activer / désactiver un compte

Sur la liste des utilisateurs, utiliser le **toggle actif/inactif** directement sur la ligne. Effet immédiat : un compte désactivé ne peut plus se connecter.

### Accorder / retirer le statut Administrateur système

Sur la liste des utilisateurs, utiliser le **toggle Admin système** directement sur la ligne.

> Un administrateur système (`SYSTEM_ADMIN`) accède à tous les modules sans appartenir à aucun département.

### Changer le mot de passe d'un collaborateur

1. Ouvrir la fiche du collaborateur (bouton **Gérer**).
2. Dans la section **Changer le mot de passe**, saisir le nouveau mot de passe (minimum 8 caractères).
3. Valider.

### Supprimer un compte

Sur la liste, utiliser le bouton de suppression. Toutes les appartenances aux départements sont supprimées automatiquement.

> Préférer la **désactivation** à la suppression pour conserver l'historique d'audit du collaborateur.

### Rechercher un utilisateur

Utiliser le champ de recherche par **identifiant** (debounce 300 ms — la liste se met à jour automatiquement pendant la saisie).

---

## Gérer les règles de commission

*Accès : sidebar → Administration → Règles de commission*

Cette page permet de configurer les montants de commission appliqués automatiquement lors de la génération des opérations bancaires (leasing, virements, etc.). Les règles sont stockées en base de données et s'appliquent sans redémarrage de l'application.

### Comprendre les conditions

Chaque règle est caractérisée par une **condition** qui détermine quand elle s'applique :

| Condition | Libellé | Comportement |
|---|---|---|
| `DEFAULT` | Par défaut | S'applique si aucune règle `SI_MATCH` ou `SI_NON_MATCH` n'existe pour cette banque |
| `SI_MATCH` | Si organisme correspond | S'applique quand l'organisme de l'opération correspond exactement au champ **Organisme** de la règle |
| `SI_NON_MATCH` | Si organisme ne correspond pas | S'applique quand la banque a une règle `SI_MATCH` mais que l'organisme de l'opération ne correspond à aucune |

**Algorithme de résolution (par ordre de priorité) :**
1. Chercher une règle `SI_MATCH` pour la banque et l'organisme exact → montant si trouvé.
2. Si une règle `SI_MATCH` existe pour la banque mais l'organisme ne correspond pas → appliquer `SI_NON_MATCH`.
3. Sinon → appliquer `DEFAULT`.
4. Si aucune règle n'existe pour la banque → aucune commission générée.

### Créer une règle

1. Cliquer sur **Nouvelle règle**.
2. Renseigner les champs :
   - **Banque** : nom exact de la banque (ex. `CIH`, `BMCI`, `SAHAM BANK`). La comparaison est insensible à la casse.
   - **Organisme** : à renseigner uniquement pour les conditions `SI_MATCH` / `SI_NON_MATCH`. Laisser vide pour `DEFAULT`.
   - **Type de transaction** : sélectionner dans la liste (`LEASING`, `Virement`, etc.).
   - **Condition** : `Par défaut`, `Si organisme correspond` ou `Si organisme ne correspond pas`.
   - **Montant (MAD)** : montant fixe en dirhams, jusqu'à 4 décimales.
3. Cliquer sur **Créer**.

### Modifier une règle

Cliquer sur l'icône **crayon** sur la ligne concernée, modifier les valeurs, puis **Enregistrer**.

### Supprimer une règle

Cliquer sur l'icône **corbeille** et confirmer. La suppression est immédiate et irréversible — si une commission est en cours de génération, la règle supprimée ne s'applique plus aux nouvelles opérations.

### Filtrer, trier et parcourir la liste

- **Recherche libre** : filtre sur la banque ou l'organisme (recherche instantanée pendant la saisie).
- **Filtres** : banque, type de transaction, condition — combinables entre eux et avec la recherche libre. Le bouton **Réinitialiser** (icône croix) efface tous les filtres.
- **Tri** : cliquer sur l'en-tête d'une colonne triable (Banque, Référence, Type transaction, Condition, Montant) pour trier par cette colonne ; recliquer pour inverser l'ordre.
- **Pagination** : 15 lignes par page par défaut, réglable à 25 ou 50 via le sélecteur en bas de la liste. Le nombre total de règles correspondant aux filtres appliqués est affiché en bas de la liste.

### Règles configurées (état au 14/07/2026)

| Banque | Organisme | Type | Condition | Montant |
|---|---|---|---|---|
| CIH | — | Leasing | Par défaut | 11,00 MAD |
| SAHAM BANK | — | Leasing | Par défaut | 11,00 MAD |
| BMCI | BMCI LEASING | Leasing | Si organisme correspond | 16,50 MAD |
| BMCI | — | Leasing | Si organisme ne correspond pas | 55,00 MAD |
| BMCI | BMCI | Virement | Si organisme correspond | 27,50 MAD |
| BMCI | — | Virement | Si organisme ne correspond pas | 33,00 MAD |
| CDM | — | Virement | Par défaut | 22,00 MAD |
| BP | — | LCN de décaissement | Par défaut | 11,00 MAD |
| BMCI | — | Mise à disposition | Par défaut | 33,00 MAD |

> Ces valeurs peuvent être modifiées à tout moment par l'administrateur sans redémarrer l'application — consulter `docs/modules/README.md#chap24` (Module Commission) pour la version la plus à jour.

---

## Configuration des commissions sur cautions

*Accès : sidebar → Administration → Commissions cautions*

Cette page permet de configurer, pour chaque banque, les deux paramètres nécessaires au calcul
automatique des commissions sur cautions (voir `docs/modules/README.md#chap9`, Module Caution,
section "Calcul des commissions par banque") :

- **Montant minimum (MAD)** : si le calcul au prorata journalier donne un montant inférieur à ce
  seuil, c'est ce seuil qui est facturé.
- **Diviseur (prorata journalier)** : base conventionnelle utilisée pour calculer la commission au
  prorata du nombre de jours (valeur bancaire habituelle : 36 000).

> **Sans configuration enregistrée pour une banque, aucune commission ne peut être calculée pour
> ses cautions** — la génération automatique mensuelle échoue avec une erreur explicite pour cette
> banque tant qu'aucune configuration n'est créée.

### Créer une configuration

1. Cliquer sur **Nouvelle configuration**.
2. Sélectionner une **banque** — seules les banques ne disposant pas encore d'une configuration sont
   proposées (une seule configuration par banque).
3. Saisir le **montant minimum** et le **diviseur**.
4. Cliquer sur **Créer**.

Si toutes les banques disposent déjà d'une configuration, le bouton **Nouvelle configuration** est
désactivé.

### Modifier une configuration

Cliquer sur l'icône **crayon** de la ligne concernée, modifier les valeurs, puis **Enregistrer**.

### Supprimer une configuration

Cliquer sur l'icône **corbeille** et confirmer. Un message d'avertissement rappelle que la
génération de commission pour cette banque échouera tant qu'aucune nouvelle configuration n'est
créée.

### Filtrer, trier et parcourir la liste

- **Recherche libre** : filtre sur le nom de la banque.
- **Filtre** : par banque.
- **Tri** : cliquer sur l'en-tête d'une colonne triable (Banque, Montant minimum, Diviseur).
- **Pagination** : 15 lignes par page par défaut, réglable à 25 ou 50 via le sélecteur en bas de la
  liste.

---

## Configuration leasing

*Accès : sidebar → Administration → Configuration leasing*

Cette page permet de modifier les paramètres du module Leasing :

- **Valeur résiduelle (%)** : taux appliqué au montant d'achat TTC pour calculer la valeur résiduelle d'un contrat de leasing (valeur initiale : 1%). S'applique à tous les nouveaux contrats et à toute régénération d'échéances.
- **Taux de TVA (%)** : taux utilisé pour ventiler chaque échéance entre montant HT et TVA (valeur initiale : 20%). S'applique à tous les nouveaux contrats et à toute régénération d'échéances.
- **Seuil d'alerte court terme (jours)** : fenêtre utilisée pour la métrique de suivi "échéances leasing à venir à court terme" (valeur initiale : 7 jours).
- **Seuil d'alerte long terme (jours)** : fenêtre utilisée pour la métrique de suivi "échéances leasing à venir à long terme" (valeur initiale : 30 jours).

### Modifier la configuration

1. Saisir les nouvelles valeurs (pourcentage pour la valeur résiduelle et le taux de TVA, nombre de jours pour les seuils d'alerte).
2. Cliquer sur **Enregistrer**.

La date de dernière modification est affichée en bas du formulaire. Le changement de valeur résiduelle et de taux de TVA s'applique immédiatement aux contrats créés ou modifiés après l'enregistrement — les contrats déjà signés ne sont pas recalculés. Le changement des seuils d'alerte s'applique immédiatement aux métriques de suivi.

---

## Configuration CMT

*Accès : sidebar → Administration → Configuration CMT*

Cette page permet de modifier les seuils utilisés par les métriques de suivi des crédits à moyen terme :

- **Seuil d'alerte court terme (jours)** : fenêtre utilisée pour la métrique de suivi "échéances CMT à venir à court terme" (valeur initiale : 7 jours).
- **Seuil d'alerte long terme (jours)** : fenêtre utilisée pour la métrique de suivi "échéances CMT à venir à long terme" (valeur initiale : 30 jours).

### Modifier la configuration

1. Saisir les nouvelles valeurs en nombre de jours.
2. Cliquer sur **Enregistrer**.

La date de dernière modification est affichée en bas du formulaire. Le changement s'applique immédiatement aux métriques de suivi.

---

## Configuration Lignes Fonctionnelles

*Accès : sidebar → Administration → Configuration Lignes Fonctionnelles*

Cette page permet de modifier les seuils utilisés par le batch hebdomadaire de suivi des expirations
de lignes fonctionnelles (générales et spécifiques) :

- **Seuil d'alerte (mois)** : nombre de mois restants avant expiration en-deçà duquel une notification
  d'ALERTE (action immédiate requise) est générée (valeur initiale : 1 mois).
- **Seuil d'avertissement (mois)** : nombre de mois restants avant expiration en-deçà duquel une
  notification d'AVERTISSEMENT (planifier le renouvellement) est générée (valeur initiale : 3 mois).

### Modifier la configuration

1. Saisir les nouvelles valeurs en nombre de mois.
2. Cliquer sur **Enregistrer**.

La date de dernière modification est affichée en bas du formulaire. Le changement s'applique
immédiatement à la prochaine exécution du batch (chaque dimanche à 12h).

---

## Configuration Charges fixes bancaires

*Accès : sidebar → Administration → Charges fixes bancaires*

Cette page permet de gérer la liste des charges fixes bancaires récurrentes — frais bancaires,
impôts/taxes, agios et prélèvements connus à l'avance — générées automatiquement chaque mois pour
chaque banque (voir `docs/modules/README.md#chap21`, Module Frais bancaires, section "Génération
automatique mensuelle").
La banque de chaque charge est affichée dans une colonne triable, au même titre que les autres
colonnes de la liste.

À la première mise en service de cette page (16/07/2026), la table a été pré-remplie avec les
charges historiques auparavant codées en dur dans l'application — aucune saisie initiale n'est
nécessaire, seules les évolutions futures (nouveau frais, changement de montant, etc.) doivent être
saisies ici.

Pour chaque charge, les informations suivantes sont modifiables :

- **Banque** : banque à laquelle la charge est rattachée.
- **Libellé** : nom de la charge tel qu'il apparaîtra (avant ajout éventuel de la période).
- **Type** : Frais bancaire, Impôt / Taxe, Agios ou Prélèvement — détermine dans quel module la
  charge sera créée chaque mois.
- **Motif** : uniquement pour le type Impôt / Taxe (ex. TVA, IR) — obligatoire dans ce cas.
- **Montant**.
- **Émission** : jour fixe du mois, dernier jour du mois, ou dernier jour du trimestre.
- **Ajouter automatiquement la période au libellé généré** : si coché, la période (mois ou
  trimestre) est ajoutée automatiquement au libellé au moment de la génération (ex. « IAM 06/2026 »).
- **Afficher le mois précédent** : uniquement si la période est ajoutée au libellé — utilisé pour les
  charges "M-1" (ex. TVA, IR déclarés le mois suivant).

### Créer une charge fixe

1. Cliquer sur **Nouvelle charge fixe**.
2. Renseigner la banque, le libellé, le type, le montant et le mode d'émission.
3. Cliquer sur **Créer**.

### Modifier une charge fixe

1. Cliquer sur l'icône crayon de la ligne concernée.
2. Modifier les champs souhaités.
3. Cliquer sur **Enregistrer**.

### Supprimer une charge fixe

1. Cliquer sur l'icône corbeille de la ligne concernée.
2. Confirmer la suppression.

La suppression n'arrête que la génération automatique future de cette charge — elle n'affecte aucun
frais bancaire, impôt/taxe, agios ou prélèvement déjà généré les mois précédents.

Le changement (création, modification, suppression) s'applique à la prochaine génération automatique
mensuelle (dernier jour du mois, à 07h00) — il n'affecte pas le mois en cours si la génération a déjà
eu lieu.

### Filtrer, trier et parcourir la liste

- **Recherche libre** : filtre sur le libellé ou la banque.
- **Filtres** : banque, type de charge — combinables entre eux et avec la recherche libre. Le bouton
  **Réinitialiser** (icône croix) efface tous les filtres.
- **Tri** : cliquer sur l'en-tête d'une colonne triable (Banque, Libellé, Type, Émission, Montant)
  pour trier par cette colonne ; recliquer pour inverser l'ordre.
- **Pagination** : 15 lignes par page par défaut, réglable à 25 ou 50 via le sélecteur en bas de la
  liste. Le nombre total de charges correspondant aux filtres appliqués est affiché en bas de la
  liste.

---

## Consulter le journal d'audit

*Accès : sidebar → Paramètres → Les Journaux du portail — réservé à l'administrateur système*

### Consulter les entrées du journal

La liste affiche chaque opération sensible (création, modification, suppression) réalisée sur les
données de G-Banque : date et heure, utilisateur, type d'opération, notion métier concernée. Elle est
filtrable par utilisateur, type d'opération, notion métier et période.

### Consulter le détail d'une entrée

Cliquez sur la ligne de l'entrée dans la liste : une fenêtre s'ouvre et affiche, champ par champ, la
valeur avant et la valeur après pour l'opération sélectionnée.

Le détail n'est pas chargé avec la liste, il est calculé au moment où vous ouvrez la fenêtre : un
court temps de chargement est donc normal.

Ce que vous y verrez :

- **uniquement les champs qui ont changé** — un champ resté identique n'apparaît pas ;
- pour une **création**, la colonne « Ancienne Valeur » est vide (tiret) sur tous les champs ;
- pour une **suppression**, c'est la colonne « Nouvelle Valeur » qui est vide ;
- lorsque le champ modifié renvoie à une autre notion (banque, marché, caution…), la valeur
  s'affiche sous la forme **« identifiant — libellé »**, par exemple « 7 — BMCE ». L'identifiant
  est toujours présent : il reste exact même si la notion a été renommée ou supprimée depuis.
  Certaines notions n'ont pas de libellé (le rapprochement, par exemple) et n'affichent que leur
  identifiant ;
- les valeurs longues sont tronquées à l'écran : survolez la valeur pour l'afficher en entier.

Si la fenêtre indique « Aucun détail de log trouvé », c'est que l'opération n'a modifié aucun champ
suivi. Si elle indique « Impossible de charger le détail de ce log », le détail n'a pas pu être
reconstruit — signalez-le à l'équipe technique.

### Supprimer une entrée du journal

Bouton corbeille depuis la liste. Cette suppression n'est, à ce jour, pas elle-même tracée dans le
journal.

---

## Se déconnecter

Cliquez sur l'icône de déconnexion de la barre supérieure puis confirmez **Oui, Déconnexion** : la session est fermée immédiatement (elle ne peut plus être réutilisée, même depuis un autre onglet) et vous revenez à la page de connexion.

Sans déconnexion, la session se ferme d'elle-même au bout de **8 heures**, avec le message « Session expirée ».

> **Effet de vos modifications de droits.** Quand vous modifiez les départements, les rôles ou le statut administrateur d'un collaborateur, ou que vous désactivez son compte, le changement s'applique à sa session en cours **dans les 15 minutes**, sans qu'il ait à se reconnecter.
