# Guide utilisateur — Département Finance

> Ce guide couvre les fonctionnalités accessibles au profil **Finance** (Membre ou Directeur du département Finance).

---

## Se connecter

1. Ouvrir G-Banque dans le navigateur.
2. Saisir votre **identifiant** et votre **mot de passe**.
3. Cliquer sur **Se connecter**.

Après connexion, vous êtes redirigé automatiquement vers le **tableau de bord Finance**.

> **Compte verrouillé ?** Après 5 tentatives échouées consécutives, l'accès est bloqué 15 minutes. Aucune action n'est nécessaire — le décompte est automatique.

> **Durée de la session.** Une fois connecté, vous restez connecté jusqu'à **8 heures** (une journée de travail), même si vous rechargez la page ou ouvrez G-Banque dans un autre onglet. Au-delà, le message **« Session expirée — Votre session a expiré, veuillez vous reconnecter. »** s'affiche et vous êtes renvoyé vers la page de connexion : reconnectez-vous pour continuer. Le même message apparaît si votre compte a été désactivé entre-temps.

---

## Tableau de bord Finance

*Accès : sidebar → Accueil (ou automatiquement à la connexion)*

Le tableau de bord affiche en temps réel la situation de chaque compte bancaire :

- **Solde disponible** : calculé à la volée depuis les transactions enregistrées.
- **Plafond FC** (Financement Court terme) : encours utilisé vs plafond autorisé.
- **Disponibilité FC** *(nouveau, 31/08/2026)* : Solde actuel + Plafond FC, calculée pour le jour
  même (sans projection de fin de mois). En vert si positive (marge disponible sous le plafond), en
  rouge si négative (plafond dépassé).
- **Échus LCN** : effets de commerce arrivés à échéance.
- **Échus Virements** : virements dont la date d'échéance est passée.

### Synchronisation FunPaiement

Pour récupérer les données depuis le système de paiement externe :

1. Cliquer sur le bouton **Synchroniser FunPaiement** dans le tableau de bord.
2. Patienter pendant le traitement — le tableau se rafraîchit automatiquement.

> La synchronisation peut être lancée à tout moment. En cas d'écart entre G-Banque et FunPaiement, relancer une synchronisation avant de saisir des corrections manuelles.

### Télécharger les récapitulatifs PDF

- **Récap global** (toutes banques) : bouton **Récap global** en haut du tableau de bord — rapport
  consolidé de trésorerie pour l'ensemble des banques. Un écran demande d'abord de choisir la
  période du rapport, **à ce jour** ou **jusqu'à fin de mois** *(nouveau, 31/08/2026)* — les
  libellés du PDF (soldes, disponibilité FC) s'adaptent au choix fait ; dans les deux cas, les
  opérations en retard non encore validées restent toujours incluses.
- **Récap détaillé** (toutes banques) : bouton **Récap détaillé** en haut du tableau de bord. Une
  catégorie (Leasing, CMT, Agios...) n'apparaît que si au moins une banque y porte un montant non
  nul — une catégorie à 0,00 pour toutes les banques est omise du tableau *(nouveau, 31/08/2026)*.
- **Récap détaillé d'une banque** : bouton **Récap détaillé** sur la carte de la banque concernée.

---

## Gérer les comptes bancaires

*Accès : sidebar → Les Comptes Bancaires*

### Consulter la liste des comptes

La liste affiche tous les comptes bancaires avec le logo, le nom, la devise (badge coloré) et le numéro de compte. Elle est paginée et triable.

**Rechercher** : saisir un nom dans le champ de recherche (résultats mis à jour automatiquement après un court délai).

### Créer un compte bancaire

1. Cliquer sur **Nouveau**.
2. Renseigner les informations :
   - **Nom** de la banque/agence
   - **Numéro de compte** : RIB exact à 24 chiffres
   - **Devise** : sélectionner dans la liste
   - **Adresse** et **Agence**
   - **Solde initial** : montant de départ du compte (obligatoire, positif ou nul)
   - **Logo** : obligatoire — cliquer sur la zone pour téléverser l'image
3. Valider.

> À la création, le premier rapprochement est initialisé automatiquement avec le solde initial saisi comme solde de départ. Ce montant n'est plus modifiable après la création.

### Modifier un compte bancaire

1. Cliquer sur l'icône **Modifier** sur la ligne du compte.
2. Modifier les informations souhaitées. Le logo est conservé si vous n'en téléversez pas un nouveau.
3. Valider.

### Supprimer un compte bancaire

> **Action réservée à l'administrateur système.** Le bouton "Supprimer" n'est visible que pour le profil SYSTEM_ADMIN.

1. Cliquer sur l'icône **Supprimer** sur la ligne du compte.
2. Confirmer la suppression dans la fenêtre d'avertissement.

> **Suppression impossible** dans les cas suivants :
> - Des **lignes fonctionnelles actives** (découvert, ligne de crédit, etc.) sont encore ouvertes sur ce compte — clôturer ou transférer les lignes d'abord.
> - Des **transactions sont en attente** de validation — les traiter avant de supprimer.
> - Le **solde du compte n'est pas nul** — équilibrer le compte avant suppression.
>
> La suppression d'un compte entraîne la suppression en cascade de ses lignes fonctionnelles associées. Cette action est irréversible.

### Consulter l'historique d'un compte

Cliquer sur le nom du compte (ou l'icône de détail) pour accéder à la vue d'historique des transactions.

---

## Module Crédit Moyen Terme (CMT)

*Accès : sidebar → Les Crédits Moyen Terme*

Un crédit à moyen terme (CMT) est un emprunt remboursé par prélèvements périodiques sur une banque. Chaque crédit génère automatiquement son plan de remboursement (les échéances) dès sa création.

Les listes (Crédits encours, en attente de main levées, archivés) affichent pour chaque crédit : ID, désignation, banque, montant nominal, taux, périodicité (ajoutée le 2026-07-27), durée, montant de l'échéance, date de début et statut (Terminé / En cours).

### Créer un crédit CMT

1. Aller dans **Les Crédits Moyen Terme → Les Crédits encours → Nouveau Crédit**.
2. Renseigner : désignation, banque de prélèvement, montant nominal, taux, périodicité (en mois), durée (nombre d'échéances), montant de l'échéance, date de début.
3. Valider → le plan de remboursement complet est généré automatiquement.

### Modifier ou supprimer un crédit CMT

- **Modifier** : depuis la liste des crédits encours, cliquer sur l'icône **Modifier**. Le plan de remboursement est régénéré automatiquement. Bloqué si au moins une échéance est déjà payée.
- **Supprimer** : réservé à l'administrateur système (bouton visible uniquement pour ce profil). Bloqué si au moins une échéance est déjà payée — dans ce cas, clôturer le crédit plutôt que de le supprimer.

### Rechercher par période

Sur les écrans **Les Crédits encours**, **Les Crédits en attente de main levées** et **Les Crédits archivés**, un bouton **Période** ouvre un panneau permettant de filtrer les crédits par plage de date de début (du / au). Le bouton affiche le nombre de plages actives dès qu'un filtre est appliqué.

Sur le **Tableau d'amortissement**, le bouton **Période** ouvre un panneau à trois plages de dates indépendantes : date d'échéance, date d'opération, date de valeur. Chaque plage se filtre par date de début et/ou date de fin, sans qu'il soit nécessaire de renseigner les deux bornes.

### Consulter le tableau d'amortissement

Depuis la liste des crédits → cliquer sur l'icône **Tableau d'Amortissement**. Affiche toutes les échéances ainsi que les totaux (endettement, montant déjà payé, montant restant à payer). Filtrable par numéro d'échéance, statut, et plages de dates (échéance, opération, valeur) via le bouton **Période**.

> **Basculer le statut d'une échéance** (colonne Actions du tableau d'amortissement) : action réservée à l'administrateur système, pour corriger une anomalie sans passer par le rapprochement.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

> **Modifier une échéance** *(réservé à l'administrateur système)* : tant qu'une échéance est au statut **Prévu**, un bouton crayon de la colonne Actions du tableau d'amortissement ouvre une page dédiée de modification (voir « Modifier une échéance de leasing ou de CMT » dans le guide Administrateur). Les mêmes échéances sont aussi modifiables depuis **États des encours → Récapitulatifs** (bouton **Éditer**).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur les écrans **Les Crédits encours**, **Les Crédits en attente de main levées**, **Les Crédits archivés**, ainsi que sur le **Tableau d'amortissement** d'un crédit.

Le PDF généré reprend l'intégralité des lignes correspondant aux filtres actuellement appliqués à l'écran (désignation, banque, plage de date de début, ou pour le tableau d'amortissement : numéro d'échéance, statut, plages de dates échéance/opération/valeur) — pas seulement la page actuellement affichée. Pour le tableau d'amortissement, le PDF inclut aussi les totaux (endettement, montant déjà payé, montant restant à payer). Le fichier est téléchargé directement dans le navigateur.

### Suivre les encours

**Les Crédits Moyen Terme → Les Crédits encours** puis la vue Encours associée : encours total de CMT restant dû pour une banque, à la fin du mois en cours.

### Clôturer un crédit (main levée)

Quand toutes les échéances sont PAYE, le crédit apparaît dans **Les Crédits Moyen Terme → Les Crédits en attente de main levées**. Ouvrir le crédit → **Archiver**. Irréversible.

> Si des échéances restent PREVU, la clôture est bloquée.

### Alertes automatiques

Un traitement automatique (chaque dimanche à 14h30) détecte les crédits éligibles à la main levée et envoie une notification dans l'application.

---

## Gérer les lignes fonctionnelles

*Accès : sidebar → Les Lignes Fonctionnelles*

Une ligne fonctionnelle est une autorisation de financement accordée par une banque (plafond, taux,
période de validité), rattachée soit à toute l'activité d'une banque, soit à un marché précis.

### Consulter et gérer les natures de lignes fonctionnelles

*Accès : sidebar → Les Lignes Fonctionnelles → Les Natures des lignes Fonctionnelles*

Le référentiel des natures (Facilité de Caisse, Caution Administrative, Escompte, etc.) est
consultable et filtrable par code, libellé, description et portée.

- **Créer** : bouton **Ajouter une nature** → renseigner code, libellé, description et portée
  (Générale, Spécifique, ou les deux). Le code ne pourra plus être modifié après création.
- **Modifier** : icône crayon sur une ligne du tableau. Le champ code est verrouillé.
- **Supprimer** : icône corbeille, réservée à l'administrateur système. Bloquée si la nature est
  encore utilisée par une ligne fonctionnelle générale ou spécifique — un message l'indique.

**Exporter en PDF** : un bouton **Exporter PDF** génère un document reprenant l'intégralité des
natures correspondant aux filtres actuellement appliqués (code, libellé, description, portée) —
pas seulement la page affichée. Le fichier est téléchargé directement dans le navigateur.

### Consulter et gérer les lignes fonctionnelles générales

*Accès : sidebar → Les Lignes Fonctionnelles → Les Lignes ponctuelles*

- **Les Lignes encours** : lignes générales actives, avec création, modification et gestion des
  sous-lignes (uniquement pour la nature "Caution Administrative").
- **Les Lignes archivées** : lignes désactivées, conservées pour historique.

**Créer / modifier une ligne générale :**

1. Cliquer sur **Nouveau** (ou l'icône **Modifier** sur une ligne existante).
2. Renseigner :
   - **Banque** (obligatoire)
   - **Nature** de ligne fonctionnelle (obligatoire) — seules les natures à portée Générale (ou
     Générale et Spécifique) sont proposées
   - **Marchés** (facultatif) : sélection multiple ; laisser vide pour une ligne couvrant
     l'ensemble de l'activité de la banque
   - **Plafond** (obligatoire, supérieur à zéro)
   - **Taux** (obligatoire, entre 0 et 100)
   - **Date de départ** et **Date de fin** (obligatoires ; la date de fin ne peut pas précéder la
     date de départ)
3. Valider — un message de confirmation est demandé avant l'enregistrement.

> La création est bloquée si une ligne active existe déjà pour la même banque (ou le même marché)
> et la même nature.

**Gérer les sous-lignes (nature "Caution Administrative" uniquement) :**

Lorsque la nature "Caution Administrative" est sélectionnée, une section **Sous-lignes** apparaît
dans le formulaire :

1. Cliquer sur **Ajouter une sous-ligne** : une fenêtre s'ouvre pour saisir sa nature (parmi les
   natures à portée Générale) et son plafond propre.
2. Une sous-ligne créée peut être modifiée ou retirée depuis cette même fenêtre, tant que la ligne
   mère n'est pas encore enregistrée / lors de sa modification ultérieure.
3. Chaque sous-ligne hérite automatiquement de la banque, du taux, des marchés et des dates de la
   ligne mère — seuls son plafond et sa nature sont propres à la sous-ligne.

> Une sous-ligne déjà enregistrée ne peut pas être modifiée, désactivée ou supprimée
> indépendamment : ces actions se font uniquement depuis sa ligne mère et s'appliquent
> automatiquement à toutes ses sous-lignes (par exemple, désactiver la ligne mère désactive toutes
> ses sous-lignes actives).
>
> Le total des plafonds des sous-lignes n'est pas automatiquement plafonné par le plafond de la
> ligne mère — un contrôle visuel du cumul est affiché dans le formulaire.

**Consulter l'historique (graphique)** : depuis la liste des lignes générales, l'icône dédiée
affiche l'évolution du taux et du plafond d'une ligne dans le temps, par banque et par nature.

**Exporter en PDF** : un bouton **Exporter PDF** est disponible sur les écrans **Les Lignes
encours** et **Les Lignes archivées**. Le PDF reprend l'intégralité des lignes correspondant aux
filtres actuellement appliqués à l'écran (banque, nature, marché, statut) — pas seulement la page
affichée. Le fichier est téléchargé directement dans le navigateur.

### Consulter et gérer les lignes fonctionnelles spécifiques

*Accès : sidebar → Les Lignes Fonctionnelles → Les Lignes spécifiques*

- **Les lignes encours** : lignes spécifiques actives, rattachées à un marché.
- **Les lignes archivées** : lignes désactivées.
- Depuis une ligne, la vue de **suivi** affiche les opérations imputées sur le marché (droits
  constatés, factoring, préfinancement) avec les totaux débit / crédit / reste disponible.

**Créer / modifier une ligne spécifique :**

1. Cliquer sur **Nouveau** (ou l'icône **Modifier** sur une ligne existante).
2. Renseigner :
   - **Marché** (obligatoire) — recherche par numéro
   - **Nature** de ligne fonctionnelle (obligatoire) — seules les natures à portée Spécifique (ou
     Générale et Spécifique) sont proposées
   - **Plafond** (obligatoire, supérieur à zéro)
   - **Taux** (obligatoire, entre 0 et 100)
   - **Date de départ** et **Date de fin** (obligatoires ; la date de fin ne peut pas précéder la
     date de départ)
3. Valider — un message de confirmation est demandé avant l'enregistrement.

> La création est bloquée si une ligne active (générale ou spécifique) existe déjà pour ce marché
> et cette nature.

**Rechercher / filtrer** : les listes de lignes générales et spécifiques (encours et archivées)
sont paginées, triables, et filtrables selon les colonnes affichées (banque, marché, nature,
statut).

**Exporter en PDF** : un bouton **Exporter PDF** est disponible sur les écrans **Les lignes
encours** et **Les lignes archivées**. Le PDF reprend l'intégralité des lignes correspondant aux
filtres actuellement appliqués à l'écran (banque, nature, marché, statut) — pas seulement la page
affichée. Le fichier est téléchargé directement dans le navigateur.

### Alertes d'expiration automatiques

Chaque dimanche, un traitement automatique vérifie la date de fin de chaque ligne active (générale
et spécifique) et envoie une notification dans l'application lorsqu'une échéance approche :

- **Avertissement** : la ligne expire dans un délai proche (3 mois par défaut) — à anticiper.
- **Alerte** : la ligne expire dans un délai très court (1 mois par défaut), ou est déjà expirée —
  action immédiate recommandée.

> Les seuils d'avertissement et d'alerte sont configurables par un administrateur système (voir
> guide Administration, section "Configuration Lignes Fonctionnelles").

---

## Module Leasing

*Accès : sidebar → Leasing*

### Gérer les organismes de leasing

Les organismes (ex. BMCI Leasing, SAHAM) doivent exister dans le référentiel avant de créer un contrat cadre.

- **Créer un organisme** : Leasing → Organismes → Nouveau. Renseigner le nom.
- **Modifier / consulter** : cliquer sur l'organisme dans la liste.
- **Supprimer** : réservé à l'administrateur système. Bloqué si l'organisme est encore lié à un ou plusieurs contrats cadres.

### Gérer les contrats cadres

*Accès : sidebar → Leasing → Contrats cadres*

Un contrat cadre fixe le plafond d'engagement et la période de validité (date de début / date de fin) accordés à un organisme. **Un contrat cadre ACTIF est obligatoire avant de pouvoir signer un contrat de leasing avec cet organisme.**

- **Créer un contrat cadre** : sélectionner l'organisme, renseigner le plafond et les dates. Bloqué si l'organisme possède déjà un contrat cadre ACTIF — archiver l'ancien avant d'en créer un nouveau.
- **Modifier** : le plafond et les dates peuvent être ajustés à tout moment ; l'organisme n'est pas modifiable après création.
- **Archiver** : passe le contrat cadre de ACTIF à ARCHIVÉ. Action irréversible, réservée aux actions habituelles Finance/Admin — aucune suppression n'est possible pour un contrat cadre.
- Un contrat cadre dont la date de fin est dépassée passe automatiquement en **EXPIRÉ** dès qu'un contrat de leasing tente de s'appuyer dessus ; il faut alors en créer un nouveau.

### Créer un contrat de leasing

1. Aller dans **Leasing → Contrats → Nouveau contrat**
2. Renseigner : numéro de contrat, banque de prélèvement, organisme, bien financé, montant TTC, date de début, période en mois, type de contrat, phases.
3. Valider → les échéances et la valeur résiduelle sont calculées automatiquement.

La création se fait sous forme d'assistant en six étapes, affichées en haut de l'écran : **Organisme**, **Détails Généraux**, **Banque de prélèvement**, **Type de Contrat**, **Phases**, **Récapitulatif**. La barre de navigation (**Précédent**, **Suivant**, puis **Soumettre** à la dernière étape) reste ancrée en bas de l'écran et donc toujours visible, quelle que soit la longueur de l'étape affichée ; seul le contenu de l'étape défile. Le bouton **Suivant** est désactivé tant que l'étape en cours comporte une erreur ; le bouton **Soumettre** est désactivé tant que le contrat comporte une erreur de validation, quelle que soit l'étape concernée.

La dernière étape, **Récapitulatif**, présente sur un seul écran, sans section à replier :

- un indicateur **Contrat Valide** / **Contrat Invalide** à côté du titre ;
- la liste des erreurs de validation, le cas échéant ;
- les informations du contrat sur deux colonnes : numéro de contrat, immobilisation, date de début, période, organisme de leasing, banque de prélèvement, type de contrat et valeur résiduelle (affichée « Non applicable » pour un contrat autre que LINEAIRE) ;
- deux montants mis en évidence : **Montant Achat TTC** et **Endettement** ;
- le **Détail des Phases** (numéro de phase, durée en mois, mensualité TTC), affiché en totalité sans pagination.

La soumission ouvre une fenêtre de confirmation ; après confirmation, l'application redirige vers la liste des contrats de leasing.

> La valeur résiduelle (1% du montant TTC par défaut) et le taux de TVA (20% par défaut) sont des paramètres administrables par un administrateur système (voir guide Administration). Leur valeur peut donc différer de ces valeurs par défaut.

Les listes (contrats encours, en attente de main levée, archivés) affichent, pour chaque contrat : ID, numéro, banque, organisme, type, montant TTC, plafond du contrat cadre au moment de la signature (ajouté le 2026-07-27), échéances, date de début, durée, date de fin et statut. L'écran de détail d'un contrat (accessible depuis les listes) affiche en plus la valeur résiduelle (pour les contrats LINEAIRE) et ce même plafond.

| Type | Règle |
|---|---|
| LINEAIRE | 1 seule phase, durée = période totale |
| PROGRESSIF | Plusieurs phases, mensualités croissantes ou constantes |
| DEGRESSIF | Plusieurs phases, mensualités décroissantes ou constantes |

### Modifier ou supprimer un contrat de leasing

- **Modifier** : depuis la liste des contrats encours, ouvrir le contrat et ajuster ses informations. Les échéances sont régénérées automatiquement. Bloqué si au moins une échéance est déjà payée.
- **Supprimer** : réservé à l'administrateur système. Bloqué si au moins une échéance est déjà payée — dans ce cas, clôturer le contrat plutôt que de le supprimer.

### Rechercher par période

Sur les écrans **Les contrats leasing encours**, **Les contrats leasing en attente de main levée** et **Les contrats leasing archivés**, un bouton **Période** ouvre un panneau permettant de filtrer les contrats par plage de date de début (du / au). Le bouton affiche le nombre de plages actives dès qu'un filtre est appliqué.

Sur le **Tableau d'amortissement**, le bouton **Période** ouvre un panneau à trois plages de dates indépendantes : date d'échéance, date d'opération, date de valeur. Chaque plage se filtre par date de début et/ou date de fin, sans qu'il soit nécessaire de renseigner les deux bornes.

### Consulter le tableau d'amortissement

Depuis la liste des contrats → cliquer sur un contrat → **Tableau d'amortissement**. Affiche toutes les échéances, les montants HT/TVA/TTC, et les totaux (payé, restant). Filtrable par numéro d'échéance, statut, et plages de dates (échéance, opération, valeur) via le bouton **Période**.

> **Basculer le statut d'une échéance** (colonne Actions du tableau d'amortissement) : action réservée à l'administrateur système.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée « en cours » » dans le « Module États des encours ».

> **Modifier une échéance** *(réservé à l'administrateur système)* : tant qu'une échéance est au statut **Prévu**, un bouton crayon de la colonne Actions du tableau d'amortissement ouvre une page dédiée de modification (voir « Modifier une échéance de leasing ou de CMT » dans le guide Administrateur). Les mêmes échéances sont aussi modifiables depuis **États des encours → Récapitulatifs** (bouton **Éditer**).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur les écrans **Les organismes de leasing**, **Les contrats cadres**, **Les contrats leasing encours**, **Les contrats leasing en attente de main levée**, **Les contrats leasing archivés**, ainsi que sur le **Tableau d'amortissement** d'un contrat. Le PDF reprend les filtres actuellement appliqués, y compris les plages de dates.

Le PDF généré reprend l'intégralité des lignes correspondant aux filtres actuellement appliqués à l'écran — pas seulement la page actuellement affichée. Pour le tableau d'amortissement, le PDF inclut aussi les totaux (endettement, montant déjà payé, montant restant à payer). Le fichier est téléchargé directement dans le navigateur.

### Suivre les encours

**Leasing → Encours** : encours total de leasing pour une banque à la fin du mois en cours. Filtrable par organisme et numéro de contrat.

### Clôturer un contrat (main levée)

Quand toutes les échéances sont PAYE, le contrat apparaît dans **Leasing → En attente de main levée**. Ouvrir le contrat → **Clôturer**. Irréversible.

> Si des échéances restent PREVU, la clôture est bloquée.

### Alertes automatiques

Un traitement automatique (chaque dimanche) détecte les contrats éligibles à la main levée et envoie une notification dans l'application.

---

## Module Marchés

*Accès : sidebar → Les Marchés → La liste des marchés*

### Consulter les marchés

La liste affiche tous les marchés avec numéro, nom, montant, maître d'ouvrage, banque, dates de
travaux et statut (Actif / Archivé). Elle est filtrable par numéro, nom, maître d'ouvrage, banque
et statut — le filtre statut affiche les marchés **actifs** par défaut.

Un bouton **Période** ouvre un panneau séparé permettant de filtrer par plage de dates (du/au) sur
la date de début des travaux et sur la date d'achèvement des travaux, indépendamment l'une de
l'autre — chaque borne (du, au) est optionnelle et peut être utilisée seule.

### Créer un marché

Bouton **Ajouter Marché**. Champs requis : banque, numéro (unique), nom, montant (supérieur à
zéro), maître d'ouvrage. Les dates de début et d'achèvement des travaux sont optionnelles ; si les
deux sont renseignées, la date d'achèvement ne peut pas être antérieure à la date de début.

### Modifier un marché

Bouton crayon depuis la liste. Tous les champs sont modifiables, y compris la banque et le numéro
(sous réserve d'unicité).

### Archiver / désarchiver un marché

Bouton d'archivage (icône boîte) sur un marché actif : il sort immédiatement et définitivement des
calculs d'agios et d'encours, sans que l'historique déjà généré ne soit modifié. Un marché archivé
affiche un bouton de désarchivage (icône de reprise) pour redevenir pris en compte dans les
calculs futurs. Ces deux actions sont accessibles à toute l'équipe Finance.

### Consulter les lignes rattachées à un marché

Bouton œil sur un marché : ouvre un écran à deux onglets listant les lignes fonctionnelles
spécifiques et générales rattachées à ce marché, avec accès direct au détail de suivi de chacune.

### Supprimer un marché

Bouton corbeille — **réservé à l'administrateur système**. Refusé si le marché a des décomptes,
préfinancements, droits constatés, factorings ou lignes spécifiques rattachés ; utiliser
l'archivage dans ce cas.

### Exporter en PDF

Bouton **Exporter PDF** : génère un état complet des marchés selon les filtres actuellement
appliqués (numéro, nom, maître d'ouvrage, banque, statut, période sur les dates de travaux) — pas
seulement la page affichée à l'écran.

---

## Module Caution

*Accès : sidebar → Les Cautions*

Une caution bancaire est une garantie émise par une banque au bénéfice d'un organisme tiers.
Ce module permet de suivre chaque caution de sa prise jusqu'à sa main levée (clôture), avec
génération automatique des commissions bancaires dues.

### Consulter la liste des cautions

*Accès : sidebar → Les Cautions → Liste des cautions*

La liste affiche toutes les cautions, paginée et triable. Elle est filtrable par numéro de
caution, banque, nature, marché, organisme, montant et statut (active / clôturée).

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour la date de prise (du/au) et la date de main levée (du/au).

### Créer une caution

1. Cliquer sur **Nouveau**.
2. Renseigner :
   - **Banque** (obligatoire)
   - **Nature** de ligne fonctionnelle (obligatoire)
   - **Marché** (facultatif)
   - **Numéro de caution** (obligatoire, unique)
   - **Désignation** et **Organisme** bénéficiaire (obligatoires)
   - **Date de prise** (obligatoire, ne peut pas être dans le futur)
   - **Montant** (obligatoire, strictement positif)
   - **Taux** de commission (obligatoire, entre 0 et 100 %)
3. Valider.

> Si la banque choisie applique un prorata d'entrée (voir guide Administration pour la liste des
> banques concernées), une commission couvrant les premiers jours du mois est générée
> automatiquement dès la création.
>
> **Cas AWB** : aucune commission n'est générée à la création ; la commission du premier mois
> est calculée par la génération de fin de mois. AWB ne facturant pas le jour de délivrance, elle
> couvre les jours allant du **lendemain** de la date de prise jusqu'à la fin du mois (exemple :
> prise le 19/05 → 12 jours facturés, du 20/05 au 31/05). Le libellé de la commission indique le
> nombre de jours retenu (ex. « Prorata entrée : 12 j »).

### Modifier une caution

1. Cliquer sur l'icône **Modifier** sur la ligne de la caution — active ou déjà clôturée.
2. Tous les champs sont modifiables, y compris le numéro, la banque, la nature et le marché.
   - Pour une caution **déjà clôturée**, un champ supplémentaire **Date de main levée**
     apparaît, pour corriger une erreur de saisie — il reste obligatoire.
3. Valider.

> Pour une caution déjà clôturée, modifier un champ qui influence le calcul de la commission de
> clôture (banque, montant, taux, date de prise, date de main levée) **recalcule
> automatiquement** cette commission. La modification est refusée si cette commission est déjà
> réglée (payée), ou si aucune commission de clôture n'est identifiable sur cette caution (cas
> d'une clôture antérieure à cette fonctionnalité) — contacter un administrateur système dans ces
> deux cas.

### Supprimer une caution

> **Action réservée à l'administrateur système.** Le bouton "Supprimer" n'est visible que pour le
> profil SYSTEM_ADMIN.

1. Cliquer sur l'icône **Supprimer** sur la ligne de la caution.
2. Confirmer la suppression.

> **Suppression impossible** si la caution est déjà clôturée — dans ce cas, elle reste conservée
> pour l'historique.

### Clôturer une caution (main levée)

1. Depuis la liste, cliquer sur l'action **Clôturer** sur la ligne de la caution active.
2. Renseigner la **date de main levée**.
3. Confirmer.

> La clôture génère automatiquement la commission finale due (calculée au prorata jusqu'à la date
> de main levée) et, selon la banque, la TVA correspondante. Pour une caution AWB prise et
> clôturée dans le même mois, le jour de prise n'est pas facturé et le jour de main levée l'est
> (exemple : prise le 10/05, main levée le 20/05 → 10 jours facturés). La caution passe alors au statut
> **clôturée** et n'est plus modifiable.
>
> La clôture est bloquée si la caution est déjà clôturée, ou si aucune règle de calcul de
> commission n'est enregistrée pour sa banque — contacter un administrateur système dans ce cas.

### Exporter les cautions en PDF ou en Excel

1. Appliquer les filtres souhaités sur la liste (numéro, banque, nature, marché, organisme, plages
   de dates, montant, statut).
2. Cliquer sur **Exporter** — un panneau s'ouvre juste sous le bouton, proposant d'abord le format
   (**PDF** ou **Excel** — nouveau, 20/09/2026 ; PDF est présélectionné), puis les onze colonnes
   disponibles (N° Caution, Banque, Nature, N° Marché, Organisme, Désignation, Date de prise, Main
   levée, Montant, Taux, Statut), toutes cochées par défaut.
3. Choisir le format souhaité.
4. Décocher les colonnes à exclure du document, ou utiliser **Tout** / **Aucune**. Au moins une
   colonne doit rester cochée.
5. Cliquer sur **Exporter PDF** ou **Exporter Excel** (le libellé du bouton suit le format choisi)
   pour lancer le téléchargement. Cliquer en dehors du panneau le referme sans exporter.

> L'export contient l'intégralité des cautions correspondant aux filtres — pas seulement la page
> actuellement affichée à l'écran — avec uniquement les colonnes sélectionnées, dans le format
> choisi.

### Consulter les commissions sur cautions

*Accès : sidebar → Les Cautions → Commissions sur cautions*

Liste de toutes les commissions générées pour les cautions (prorata d'entrée, commissions
mensuelles, commissions de clôture), avec leur date d'émission et leur montant.

### Fusionner des commissions sur cautions

Depuis cette même liste, sélectionner au moins deux commissions (cases à cocher) affiche le total
HT et TTC de la sélection et un bouton **Fusionner**. La fusion :

- n'est possible que si toutes les commissions sélectionnées sont au statut Prévu, de type Caution,
  rattachées à la même banque et à la même date d'émission ;
- est **désactivée pour la banque BMCI** (sa TVA étant gérée séparément, une infobulle l'indique) ;
- demande un libellé pour la commission fusionnée, puis remplace les commissions sources (et leur
  TVA éventuelle) par une commission unique, montant TTC = somme des montants HT × 1,10.

### Consulter la TVA sur cautions

*Accès : sidebar → Les Cautions → TVA sur cautions*

Liste des montants de TVA générés automatiquement sur les commissions, pour les banques dont la
règle de calcul l'exige.

### Exporter la TVA sur cautions en PDF

1. Appliquer les filtres souhaités sur la liste (banque, libellé, montant).
2. Cliquer sur **Exporter PDF**.

> L'export contient l'intégralité des TVA sur cautions correspondant aux filtres — pas seulement
> la page actuellement affichée à l'écran.

### Génération mensuelle des commissions

Les commissions sur cautions sont générées **automatiquement chaque mois** — aucune action n'est
requise en fonctionnement normal :

- **Fin de mois** (dernier jour à 20h00) : banques facturant en fin de mois.
- **Début de mois** (1er jour à 03h00) : banques facturant en début de mois.

> Une génération manuelle reste possible depuis l'écran approprié en cas de besoin exceptionnel
> (rattrapage), mais n'est pas nécessaire en usage courant.

---

## Module Décomptes

*Accès : sidebar → Les Marchés → Les décomptes*

### Consulter les décomptes

La liste affiche tous les décomptes avec marché, banque d'encaissement, montant, observation,
dates d'émission/opération/valeur et statut (Prévu / Payé). Elle est filtrable par marché, banque
d'encaissement, statut, montant exact, ainsi que par période sur chacune des trois dates (émission,
opération, valeur) indépendamment — chaque plage se saisit avec une date de début et/ou une date de
fin, les deux étant facultatives.

### Créer un décompte

Bouton **Ajouter Décompte**. Champs requis : marché, banque d'encaissement, montant (supérieur à
zéro), date d'émission, libellé.

### Modifier un décompte

Bouton crayon depuis la liste. Tous les champs sont modifiables.

### Changer le statut d'un décompte

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un décompte

Bouton corbeille — **réservé à l'administrateur système**. Refusé si le décompte est déjà Payé.

### Valider les décomptes en attente

Les décomptes « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Décomptes** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Bouton **Exporter PDF**, disponible sur la liste des décomptes : génère un état complet selon les filtres actuellement appliqués sur l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau).

---

## Module Préfinancements

*Accès : sidebar → Les Marchés → Les préfinancements*

### Consulter les préfinancements

La liste affiche tous les préfinancements avec marché, banque, montant, date d'émission, type
(Déblocage / Remboursement) et statut (Prévu / Payé). Elle est filtrable par marché, banque, type,
statut, ainsi que par période sur chacune des dates d'émission, d'opération et de valeur
indépendamment (nouveau, 2026-07-24) — le bouton **Période** ouvre un panneau dédié avec, pour
chaque date, un champ « du » et un champ « au », tous deux facultatifs.

### Créer un préfinancement

Bouton **Ajouter Préfinancement**. Champs requis : marché, montant (supérieur à zéro), date
d'émission, type (Déblocage ou Remboursement). Le libellé est généré automatiquement selon le
type — il n'est pas saisi manuellement.

Le marché sélectionné doit être couvert par une ligne fonctionnelle Préfinancement active
(spécifique ou générale) ; à défaut, la création est refusée avec un message d'erreur explicite.

### Modifier un préfinancement

Bouton crayon depuis la liste. Tous les champs sont modifiables. Un changement de marché déclenche
une nouvelle recherche de la ligne fonctionnelle Préfinancement active du nouveau marché.

### Changer le statut d'un préfinancement

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un préfinancement

Bouton corbeille — **réservé à l'administrateur système**. Refusé si le préfinancement est déjà Payé.

### Valider les mouvements en attente

Les mouvements « Prévu » (remboursements et déblocages) dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Préfinancement** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Consulter le suivi de ligne fonctionnelle

Depuis une ligne fonctionnelle (générale ou spécifique), un écran affiche le récapitulatif agrégé
(banque, marché(s), total débit, total crédit, reste) et le détail de tous les mouvements de
préfinancement rattachés à cette ligne.

### Exporter en PDF

Bouton **Exporter PDF**, disponible sur la liste des préfinancements : génère un état complet selon les filtres actuellement appliqués sur l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau).

---

## Module Factoring

*Accès : sidebar → Les Marchés → Factoring*

### Consulter les factorings

La liste affiche tous les factorings avec marché, banque, montant, date d'émission, type
(Déblocage / Remboursement) et statut (Prévu / Payé). Elle est filtrable par marché, banque, type,
statut, ainsi que par période sur chacune des dates d'émission, d'opération et de valeur
indépendamment (nouveau, 2026-07-24) — le bouton **Période** ouvre un panneau dédié avec, pour
chaque date, un champ « du » et un champ « au », tous deux facultatifs.

### Créer un factoring

Bouton **Ajouter**. Champs requis : marché, montant (supérieur à zéro), date d'émission, type
(Déblocage ou Remboursement). Le libellé est généré automatiquement selon le type — il n'est pas
saisi manuellement.

Le marché sélectionné doit être couvert par une ligne fonctionnelle Factoring active (spécifique ou
générale) ; à défaut, la création est refusée avec un message d'erreur explicite.

### Modifier un factoring

Bouton crayon depuis la liste. Tous les champs sont modifiables. Un changement de marché déclenche
une nouvelle recherche de la ligne fonctionnelle Factoring active du nouveau marché.

### Changer le statut d'un factoring

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un factoring

Bouton corbeille — **réservé à l'administrateur système**. Refusé si le factoring est déjà Payé.

### Valider les mouvements en attente

Les mouvements « Prévu » (remboursements et déblocages) dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Factoring** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Consulter le suivi de ligne fonctionnelle

Depuis une ligne fonctionnelle (générale ou spécifique), un écran affiche le récapitulatif agrégé
(banque, marché(s), total débit, total crédit, reste) et le détail de tous les mouvements de
factoring rattachés à cette ligne.

### Exporter en PDF

Bouton **Exporter PDF**, disponible sur la liste des factorings : génère un état complet selon les filtres actuellement appliqués sur l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau).

---

## Module Droits Constatés

*Accès : sidebar → Les Marchés → Les droits constatés*

### Consulter les droits constatés

La liste affiche tous les droits constatés avec marché, banque, montant, date d'émission, type
(Déblocage / Remboursement) et statut (Prévu / Payé). Elle est filtrable par marché, banque, type,
statut, ainsi que par période sur chacune des dates d'émission, d'opération et de valeur
indépendamment (nouveau, 2026-07-24) — le bouton **Période** ouvre un panneau dédié avec, pour
chaque date, un champ « du » et un champ « au », tous deux facultatifs.

### Créer un droit constaté

Bouton **Ajouter**. Champs requis : marché, banque, montant (supérieur à zéro), date d'émission,
type (Déblocage ou Remboursement). Dès que le marché est sélectionné, le champ banque est
pré-rempli automatiquement avec la banque de ce marché — vous pouvez le remplacer par une autre
banque si les fonds transitent par un compte différent. Le libellé est généré automatiquement selon
le type — il n'est pas saisi manuellement.

Le marché sélectionné doit être couvert par une ligne fonctionnelle Droit Constaté active
(spécifique ou générale) ; à défaut, la création est refusée avec un message d'erreur explicite.

### Modifier un droit constaté

Bouton crayon depuis la liste. Tous les champs sont modifiables, y compris la banque
indépendamment du marché. Un changement de marché déclenche une nouvelle recherche de la ligne
fonctionnelle Droit Constaté active du nouveau marché.

### Changer le statut d'un droit constaté

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un droit constaté

Bouton corbeille — **réservé à l'administrateur système**. Refusé si le droit constaté est déjà Payé.

### Valider les mouvements en attente

Les mouvements « Prévu » (remboursements et déblocages) dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Droit constaté** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Consulter le suivi de ligne fonctionnelle

Depuis une ligne fonctionnelle (générale ou spécifique), un écran affiche le récapitulatif agrégé
(banque, marché(s), total débit, total crédit, reste) et le détail de tous les mouvements de droit
constaté rattachés à cette ligne.

### Exporter en PDF

Bouton **Exporter PDF**, disponible sur la liste des droits constatés : génère un état complet selon les filtres actuellement appliqués sur l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau).

---

## Module Chèques

*Accès : sidebar → Les moyens de paiement → Chèques*

### Consulter les chèques

Deux listes distinctes, paginées et triables :

- **Encaissements** (*sidebar → Chèques → Encaissements*) : chèques reçus par Staport, bénéficiaire
  toujours "STAPORT". Colonnes : ID, date d'émission, date d'échéance, date d'opération, date de
  valeur, montant, émetteur, bénéficiaire, banque d'encaissement, libellé, observation, statut.
  Filtres : numéro, montant, émetteur, banque, statut.
- **Décaissements** (*sidebar → Chèques → Décaissements*) : chèques émis par Staport. Colonnes : ID,
  date d'émission, date d'échéance, date d'opération, date de valeur, bénéficiaire, banque, montant,
  libellé, observation, statut. Filtres : numéro, bénéficiaire, banque, statut.

> Le numéro du chèque n'est pas affiché en colonne (retiré le 2026-07-25) mais reste utilisable
> comme critère de recherche (filtre numéro). L'identifiant technique de synchronisation FunPaiement
> n'est ni affiché ni filtrable — champ interne.

Les colonnes libellé et observation (ajoutées le 2026-07-25) sont des libellés calculés, toujours
renseignés : le libellé reprend le bénéficiaire (encaissement) ou l'émetteur (décaissement) du
chèque, l'observation reprend son numéro ("CHQ N°..."). Les colonnes date d'opération et date de
valeur (également ajoutées le 2026-07-25) ne sont en revanche renseignées qu'une fois le chèque
rapproché — vides tant que le chèque est En cours.

Sur les deux écrans, un bouton **Période** ouvre un panneau séparé permettant de filtrer par plage
de dates (du/au, nouveau 2026-07-25) sur la date d'émission, la date d'échéance, la date
d'opération et la date de valeur (ces deux dernières renseignées uniquement une fois le chèque
rapproché) — chacune des quatre plages est indépendante et chaque borne (du, au) est optionnelle.

Statuts possibles : **En cours**, **Réglé**, **Annulé**.

### Créer un chèque

Bouton **Ajouter Chèque**, accessible uniquement depuis la liste **Encaissements** — la création
manuelle ne concerne que les chèques d'encaissement (bénéficiaire "STAPORT" fixe, non modifiable).
Champs requis : numéro, banque, date d'émission, montant (strictement positif), émetteur. La date
d'échéance est optionnelle.

Il n'existe pas de création manuelle pour les chèques de décaissement — ceux-ci sont intégrés
automatiquement depuis FunPaiement (synchronisation externe).

### Modifier un chèque

Bouton crayon depuis l'une ou l'autre liste. **Désactivé si le chèque est déjà Réglé** — un chèque
réglé n'est plus modifiable.

### Changer le statut d'un chèque

Interrupteur En cours/Réglé dans la liste — **réservé à l'administrateur système**. Désactivé si le
chèque est Annulé.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un chèque

Bouton corbeille — **réservé à l'administrateur système**. Désactivé si le chèque est déjà Réglé.

> **Seuls les chèques saisis manuellement peuvent être supprimés.** Un chèque provenant de la
> synchronisation FunPaiement est refusé, avec un message invitant à l'annuler plutôt qu'à le
> supprimer. La raison : la synchronisation relit tout l'historique des paiements à chaque
> exécution, et une ligne supprimée est recréée à l'identique le lendemain. **L'annulation, elle,
> est stable** — une ligne déjà présente n'est jamais recréée ni modifiée par la synchronisation.

### Valider les chèques en cours

Les chèques « En cours » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Chèque** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur les listes **Encaissements** et **Décaissements**. Le PDF reprend l'intégralité des chèques correspondant aux filtres actuellement appliqués à l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Le fichier est téléchargé directement dans le navigateur.

---

## Module LCN

*Accès : sidebar → Les moyens de paiement → LCN*

### Consulter les LCN

Deux listes distinctes, paginées et triables :

- **Encaissements** (*sidebar → LCN → Encaissements*) : LCN reçus par Staport, bénéficiaire toujours
  "STAPORT". Colonnes : ID, banque, montant, date d'émission, date d'échéance, émetteur, nature
  (Encaissement / Escompte), libellé, observation, statut. Filtres : numéro, banque, nature, statut.
- **Décaissements** (*sidebar → LCN → Décaissements*) : LCN émis par Staport, intégrés
  automatiquement depuis FunPaiement. Colonnes : ID, banque, montant, date d'émission, date
  d'échéance, libellé, observation, statut. Filtres : numéro, banque, statut.

Les colonnes libellé et observation (ajoutées le 2026-07-25) sont des libellés calculés, toujours
renseignés (même logique que pour les Chèques).

> Le numéro du LCN n'est pas affiché en colonne mais reste utilisable comme critère de recherche
> (filtre numéro). L'identifiant technique de synchronisation FunPaiement et la ligne fonctionnelle
> d'escompte associée ne sont ni affichés ni filtrables sur ces deux listes.

Sur les deux écrans, un bouton **Période** ouvre un panneau séparé permettant de filtrer par plage
de dates (du/au, nouveau 2026-07-25) sur la date d'émission, la date d'échéance, la date
d'opération et la date de valeur (ces deux dernières renseignées uniquement une fois le LCN
rapproché) — chacune des quatre plages est indépendante et chaque borne (du, au) est optionnelle.

Statuts possibles : **En cours**, **Réglé**, **Annulé**.

### Créer un LCN

Bouton **Ajouter LCN**, accessible uniquement depuis la liste **Encaissements** — il n'existe pas de création manuelle pour les LCN de décaissement, intégrés automatiquement depuis FunPaiement.

Champs requis : banque, montant (strictement positif), date d'émission, nature (**Encaissement** ou **Escompte**). La date d'échéance est optionnelle.

> Sélectionner la nature **Escompte** associe automatiquement le LCN à la ligne fonctionnelle d'escompte commercial active de la banque choisie. La nature Escompte n'est possible que pour un LCN d'Encaissement.

### Modifier un LCN

Bouton crayon depuis l'une ou l'autre liste. **Désactivé si le LCN est déjà Réglé.** Seul un LCN d'encaissement peut être modifié — un LCN de décaissement (issu de FunPaiement) ne l'est jamais manuellement.

> Changer la nature vers Escompte associe la ligne fonctionnelle d'escompte commercial de la banque ; changer la nature depuis Escompte dissocie cette ligne.

### Changer le statut d'un LCN

Interrupteur En cours/Réglé dans la liste — **réservé à l'administrateur système**. Désactivé si le LCN est Annulé.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un LCN

Bouton corbeille — **réservé à l'administrateur système**. Désactivé si le LCN est déjà Réglé.

> **Seuls les LCN saisis manuellement peuvent être supprimés.** Un LCN provenant de la
> synchronisation FunPaiement est refusé, avec un message invitant à l'annuler plutôt qu'à le
> supprimer. La raison : la synchronisation relit tout l'historique des paiements à chaque
> exécution, et une ligne supprimée est recréée à l'identique le lendemain. **L'annulation, elle,
> est stable** — une ligne déjà présente n'est jamais recréée ni modifiée par la synchronisation.

### Valider les LCN en cours

Les LCN « En cours » arrivés à leur date de référence (échéance, ou émission pour un escompte) ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **LCN Décaissement**, **LCN Encaissement** ou **LCN Escompte** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Suivre les LCN Escompte d'une ligne fonctionnelle

Depuis la fiche d'une ligne fonctionnelle d'escompte commercial, un écran de suivi dédié liste tous les LCN de nature Escompte qui lui sont rattachés, avec le montant total cumulé. Colonnes : ID, date d'émission, date d'échéance, émetteur, date d'opération, date de valeur, montant, nature, type, libellé, observation, statut.

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur les listes **Encaissements** et **Décaissements**. Le PDF reprend l'intégralité des LCN correspondant aux filtres actuellement appliqués à l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Le fichier est téléchargé directement dans le navigateur.

---

## Module Virement

*Accès : sidebar → Les moyens de paiement → Virements*

### Consulter les virements

Deux listes distinctes, paginées et triables :

- **Encaissements** : virements reçus par Staport, bénéficiaire toujours "STAPORT". Colonnes : ID,
  date de réception, date d'opération, date de valeur, bénéficiaire, banque bénéficiaire (banque
  d'encaissement), émetteur, montant, type, libellé, observation, statut. Filtres : montant, banque,
  bénéficiaire, émetteur, statut.
- **Décaissements** : virements émis par Staport, intégrés automatiquement depuis FunPaiement.
  Colonnes : ID, date d'émission, date d'opération, date de valeur, bénéficiaire, banque
  bénéficiaire, banque émettrice, montant, type, libellé, observation, statut. Filtres : montant,
  banque bénéficiaire, bénéficiaire, banque émettrice, statut.

Les colonnes libellé et observation (ajoutées le 2026-07-25) sont des libellés calculés, toujours
renseignés (même logique que pour les Chèques, LCN et Mises à disposition). Les colonnes date
d'opération et date de valeur (également ajoutées le 2026-07-25) ne sont renseignées qu'une fois le
virement rapproché.

Sur les deux écrans, un bouton **Période** ouvre un panneau séparé permettant de filtrer par plage
de dates (du/au, nouveau 2026-07-25) sur la date d'émission, la date d'opération et la date de
valeur (ces deux dernières renseignées uniquement une fois le virement rapproché) — chacune des
trois plages est indépendante et chaque borne (du, au) est optionnelle.

Statuts possibles : **En cours**, **Réglé**, **Annulé**.

### Créer un virement

Bouton **Ajouter virement**, accessible uniquement depuis la liste **Encaissements** — il n'existe pas de création manuelle pour les virements de décaissement, intégrés automatiquement depuis FunPaiement.

Champs requis : banque bénéficiaire, montant (strictement positif), date d'émission. La banque bénéficiaire peut être choisie parmi les banques connues de G-Banque, ou saisie en texte libre si la banque du tiers n'est pas répertoriée.

### Modifier un virement

Bouton crayon depuis la liste Encaissements. **Désactivé si le virement est déjà Réglé.** Seul un virement d'encaissement peut être modifié.

### Changer le statut d'un virement

Interrupteur En cours/Réglé dans la liste — **réservé à l'administrateur système**. Désactivé si le virement est Annulé.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un virement

Bouton corbeille — **réservé à l'administrateur système**. Désactivé si le virement est déjà Réglé.

> **Seuls les virements saisis manuellement peuvent être supprimés.** Un virement provenant de la
> synchronisation FunPaiement est refusé à la suppression, avec le message : *« Suppression
> interdite : le virement provient de la synchronisation FunPaiement et serait recréé à la prochaine
> synchronisation. Annulez-le plutôt que de le supprimer. »*
>
> La raison : la synchronisation relit tout l'historique des paiements à chaque exécution. Une ligne
> supprimée n'est plus reconnue comme déjà connue, et elle est recréée à l'identique — le lendemain,
> le doublon est de retour. **Pour retirer un tel virement des encours, annulez-le** : une ligne déjà
> présente n'est jamais recréée ni modifiée par la synchronisation, l'annulation est donc stable.
>
> La suppression d'un virement saisi manuellement est en revanche définitive.

### Valider les virements en cours

Les virements « En cours » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Virement** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

> Si la banque retenue pour la transaction (émettrice pour un décaissement, bénéficiaire pour un encaissement) n'est pas renseignée sur le virement, la validation reste possible mais le recalcul de solde en cascade n'est pas déclenché pour cette opération.

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur les listes **Encaissements** et **Décaissements**. Le PDF reprend l'intégralité des virements correspondant aux filtres actuellement appliqués à l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Le fichier est téléchargé directement dans le navigateur.

---

## Module Virement International

*Accès : sidebar → Les moyens de paiement → Virements internationaux*

> **Particularité** : contrairement aux Chèques, LCN, Virements et Mises à disposition, ce module n'est alimenté par **aucune synchronisation automatique** avec FunPaiement. Encaissements et décaissements sont **tous deux saisis et modifiés manuellement**, via le même formulaire.

### Consulter les virements internationaux

Liste unique, paginée et triable, des encaissements et décaissements confondus. Colonnes : ID, date
d'émission, date d'opération, date de valeur, bénéficiaire, émetteur, banque, montant, type
(Encaissement / Décaissement), libellé, observation, statut. Filtrable par banque, type et statut.

Les colonnes libellé et observation (ajoutées le 2026-07-25) sont générées automatiquement à la
création (voir ci-dessous), toujours renseignées. Les colonnes date d'opération et date de valeur
(également ajoutées le 2026-07-25) ne sont renseignées qu'une fois le virement international
rapproché.

Un bouton **Période** ouvre un panneau séparé permettant de filtrer par plage de dates (du/au,
nouveau 2026-07-25) sur la date d'émission, la date d'opération et la date de valeur (ces deux
dernières renseignées uniquement une fois le virement international rapproché) — chacune des trois
plages est indépendante et chaque borne (du, au) est optionnelle.

Statuts possibles : **En cours**, **Réglé**, **Annulé**.

### Créer un virement international

Bouton **Ajouter**. Champs requis : banque, émetteur, bénéficiaire, montant (strictement positif), date d'émission, type (**Encaissement** ou **Décaissement**) — le sens est choisi librement à la saisie. Le libellé et l'observation sont générés automatiquement à partir du type, de l'émetteur ou du bénéficiaire, et du montant.

### Modifier un virement international

Bouton crayon depuis la liste. **Désactivé si déjà Réglé.** Contrairement aux autres moyens de paiement, la modification reste possible aussi bien pour un encaissement que pour un décaissement, puisque les deux sont saisis manuellement.

### Changer le statut d'un virement international

Interrupteur En cours/Réglé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un virement international

Bouton corbeille — **réservé à l'administrateur système**. Désactivé si déjà Réglé.

### Valider les virements internationaux en cours

Les virements internationaux « En cours » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Virement international** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur la liste combinée (encaissements et décaissements confondus). Le PDF reprend l'intégralité des virements internationaux correspondant aux filtres actuellement appliqués à l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Le fichier est téléchargé directement dans le navigateur.

---

## Module Mise à disposition

*Accès : sidebar → Les moyens de paiement → Mises à disposition*

### Consulter les mises à disposition

Une seule liste, paginée et triable (écran "Décaissements" de la sidebar). Les mises à disposition sont des décaissements
alimentés automatiquement par la synchronisation FunPaiement : elles ne se saisissent ni ne se modifient à la main. Colonnes : ID, banque, date
d'émission, date d'opération, date de valeur, bénéficiaire, CIN du bénéficiaire, montant, libellé,
observation, statut. Filtres : numéro, bénéficiaire, CIN du bénéficiaire, banque, statut.

Les colonnes libellé et observation (ajoutées le 2026-07-25) sont des libellés calculés, toujours
renseignés (même logique que pour les Chèques et LCN). Les colonnes date d'opération et date de
valeur (également ajoutées le 2026-07-25) ne sont renseignées qu'une fois la mise à disposition
rapprochée.

> Le numéro de la mise à disposition n'est pas affiché en colonne mais reste utilisable comme
> critère de recherche (filtre numéro).

Un bouton **Période** ouvre un panneau séparé permettant de filtrer par plage de dates (du/au,
nouveau 2026-07-25) sur la date d'émission, la date d'opération et la date de valeur — chacune des
trois plages est indépendante et chaque borne (du, au) est optionnelle.

Statuts possibles : **En cours**, **Réglé**, **Annulé**.

### Changer le statut d'une mise à disposition

Interrupteur En cours/Réglé dans la liste — **réservé à l'administrateur système**. Désactivé si Annulée.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer une mise à disposition

Une mise à disposition ne se supprime pas : la synchronisation relit tout l'historique des paiements à chaque exécution et la recréerait à l'identique le lendemain. Pour la retirer des encours, elle est **annulée** côté FunPaiement — une ligne annulée n'est jamais recréée ni modifiée par la synchronisation.

### Valider les mises à disposition en cours

Les mises à disposition « En cours » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Mise à disposition** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur la liste **Décaissements** — Le PDF reprend l'intégralité des mises à disposition correspondant aux filtres actuellement appliqués à l'écran — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Le fichier est
téléchargé directement dans le navigateur.

---

## Module Recap Encaissement / Recap Décaissement

*Accès : sidebar → Les moyens de paiement → Recap Encaissement / Recap Décaissement*

### Consulter le recap

Deux écrans, l'un pour les encaissements, l'autre pour les décaissements, regroupant dans un seul
tableau les opérations des 5 moyens de paiement (Chèques, LCN, Virements nationaux, Virements
internationaux, Mises à disposition), tous statuts confondus, toutes banques confondues.

Colonnes : moyen de paiement, banque, numéro, émetteur (sur l'écran Encaissement) ou bénéficiaire
(sur l'écran Décaissement), montant, date d'opération, date de valeur, date d'échéance (« — » pour
les moyens de paiement qui n'en portent pas), statut, libellé.

Le montant total des lignes actuellement filtrées s'affiche en haut de l'écran, à côté du bouton
d'export.

### Filtrer

Filtres disponibles, tous combinables : banque, moyen de paiement (plusieurs choix possibles),
statut (plusieurs choix possibles), numéro (contient), libellé (contient), montant (de/à) et
période — date d'opération et date de valeur, chacune avec sa propre plage du/au (bouton
**Période**). Le tri par défaut se fait sur la date de valeur, du plus récent au plus ancien ;
cliquer sur l'en-tête d'une colonne permet de changer le tri.

Le bouton **Effacer** réinitialise tous les filtres.

### Ce que l'écran ne permet pas

Ces deux écrans sont strictement consultatifs : aucune création, modification, suppression ou
changement de statut n'est possible ici. Pour agir sur une opération, il faut se rendre sur l'écran
de son moyen de paiement d'origine (par exemple "Chèques → Encaissements" pour un chèque).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur chacun des deux écrans. Le PDF reprend l'intégralité
des lignes correspondant aux filtres actuellement appliqués — pas seulement la page affichée. Le
fichier est téléchargé directement dans le navigateur.

---

## Module Prélèvement

*Accès : sidebar → Les moyens de paiement → Prélèvements*

Un prélèvement est un débit initié par un tiers sur un compte de Staport (ex. prélèvement d'assurance, cotisation CNSS). Contrairement aux autres moyens de paiement, il n'existe **aucune synchronisation FunPaiement** pour ce module ; certains prélèvements récurrents (charges fixes) sont en revanche générés automatiquement chaque mois par un mécanisme interne à G-Banque.

### Consulter les prélèvements

La liste affiche tous les prélèvements avec libellé, banque, montant, date d'émission, date d'opération, date de valeur (ces deux dernières ajoutées le 2026-07-25, renseignées uniquement une fois le prélèvement rapproché) et statut (Prévu / Payé). Elle est paginée, triable et filtrable par banque, statut et libellé.

> L'observation n'est pas affichée en colonne de liste — elle reste consultable et modifiable depuis le formulaire.

Une partie des prélèvements est créée automatiquement le dernier jour de chaque mois à 07h00, pour les charges fixes connues à l'avance (ex. cotisation CNSS) : ces prélèvements apparaissent directement dans la liste, sans action de l'équipe Finance.

### Créer un prélèvement

Bouton **Ajouter prélèvement**. Champs requis : banque, montant, date d'émission, libellé, observation.

### Modifier un prélèvement

Bouton crayon depuis la liste. **Désactivé si le prélèvement est déjà Payé.**

### Changer le statut d'un prélèvement

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un prélèvement

Bouton corbeille — **réservé à l'administrateur système**. Désactivé si le prélèvement est déjà Payé.

### Valider les prélèvements en attente

Les prélèvements « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Prélèvement** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur la liste des prélèvements ainsi que sur l'écran
d'encours (nouveau, 22/07/2026). Le PDF reprend l'intégralité des prélèvements correspondant aux
filtres actuellement appliqués à l'écran — pas seulement la page affichée. Le fichier est
téléchargé directement dans le navigateur.

---

## Historique des transactions

*Accès : sidebar → Historique des transactions*

### Consulter l'historique

Cet écran centralise tous les mouvements bancaires produits par les autres modules (leasing, crédit
moyen terme, chèques, LCN, virements, virements internationaux, mises à disposition, prélèvements,
préfinancements, factoring, droits constatés, décomptes, commissions, agios, cautions, impôts et
taxes, frais bancaires, restitutions, opérations erronées, impayées). Chaque validation d'opération
dans l'un de ces modules crée automatiquement une transaction ici — cet écran ne permet ni création
ni suppression directe.

Colonnes affichées : ID, banque, date d'émission, date d'opération, date de valeur, débit, crédit,
TVA déductible, TVA collectée, source, statut du rapprochement, libellé, observation (nouveau
2026-07-25) et référent (nouveau 2026-07-25 — identifiant technique de l'opération source ayant
généré la transaction, ex. l'ID du chèque ou du LCN ; vide si la transaction n'est pas synchronisée
avec une entité source).

La liste est paginée et filtrable par libellé, banque, source (module d'origine), montant et statut
du rapprochement (en cours, clôture provisoire, clôturé définitivement).

> Le rapprochement auquel appartient chaque transaction est identifiable en base (`rapprochementId`)
> mais n'est pas affiché sur cet écran ni exposé comme lien de navigation — consulter le module
> Rapprochement séparément pour ce niveau de détail.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de la transaction : date d'émission (du/au), date d'opération (du/au)
et date de valeur (du/au). Chaque plage peut être ouverte (une seule borne renseignée), fermée
(les deux bornes) ou laissée vide (pas de filtre sur cette date). Le bouton affiche le nombre de
plages actives une fois au moins un filtre de date appliqué.

### Exporter l'historique en PDF

Bouton **Exporter PDF** dans la barre d'outils de l'écran. Génère un document PDF reprenant toutes
les transactions correspondant aux filtres actuellement appliqués (mêmes critères que la liste
affichée à l'écran, sans limite de pagination) : identifiant, banque, dates d'émission/opération/
valeur, débit, crédit, TVA déductible, TVA collectée, source, statut du rapprochement, libellé et
observation (nouveau 2026-07-25), ainsi que les totaux débit et crédit de l'ensemble exporté. Le
fichier est téléchargé automatiquement. Le référent n'est pas repris dans l'export PDF (uniquement
sur l'écran).

### Modifier une transaction

Depuis l'écran de détail d'un rapprochement (module Rapprochement) ou l'écran **Rapprochement** des États des encours, une transaction peut être corrigée avec les mêmes champs que lors de sa validation : date de valeur, date d'opération, libellé, observation et TVA (un taux ou un montant). La date d'émission, le montant et la banque ne sont pas modifiables. Cette correction est automatiquement répercutée sur l'opération source correspondante — pas d'action supplémentaire requise.

> **Modification impossible** si le rapprochement auquel appartient la transaction est déjà
> **clôturé définitivement** — le message d'erreur l'indique explicitement.

---

## Module Frais bancaires

*Accès : sidebar → Les Frais bancaires*

### Consulter les frais bancaires

La liste affiche tous les frais bancaires avec libellé, banque, montant, date d'émission, date
d'opération, date de valeur, observation (ces trois derniers ajoutés le 2026-07-25) et statut
(Prévu / Payé). Elle est filtrable par banque, libellé et statut.

Une partie des frais bancaires est créée automatiquement chaque mois par un traitement récurrent
(charges fixes connues à l'avance — abonnements, commissions de tenue de compte, etc.) : ces frais
apparaissent directement dans la liste, sans action de l'équipe Finance.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates du frais bancaire : date d'émission (du/au), date d'opération (du/au)
et date de valeur (du/au).

### Exporter les frais bancaires en PDF

Bouton **Exporter PDF** dans la barre d'outils de l'écran. Génère un document PDF reprenant tous
les frais bancaires correspondant aux filtres actuellement appliqués (banque, libellé, statut,
plages de dates), sans limite de pagination : libellé, banque, montant, date d'émission et statut
de chaque frais, ainsi que le montant total de l'ensemble exporté. Le fichier est téléchargé
automatiquement.

### Créer un frais bancaire

Bouton **Ajouter frais bancaire**. Champs requis : banque, montant, date d'émission, libellé,
observation.

### Modifier un frais bancaire

Bouton crayon depuis la liste, accessible quel que soit le statut du frais bancaire.

### Changer le statut d'un frais bancaire

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un frais bancaire

Bouton corbeille — **réservé à l'administrateur système**. Refusé si le frais bancaire est déjà Payé :
un frais bancaire réglé ne peut plus être supprimé.

### Valider les frais bancaires en attente

Les frais bancaires « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Frais bancaire** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

---

## Module Agios

*Accès : sidebar → Agios*

Les agios sont des charges facturées par la banque (pénalités de découvert, commissions sur facilité de caisse, etc.). Chaque agios est rattaché à une banque et passe par deux états : **À payer (PREVU)** et **Payé (PAYE)**.

### Consulter la liste des agios

La liste affiche tous les agios avec leur libellé, leur banque, leur montant, leur date d'émission, leur date d'opération, leur date de valeur, leur observation (ces trois derniers ajoutés le 2026-07-27) et leur statut. Elle est paginée, triable et filtrable.

**Filtrer les agios :**
- Sélectionner une **banque** dans le premier menu déroulant.
- Sélectionner un **statut** (À payer / Payé) dans le second menu déroulant.
- Saisir un **libellé** dans le champ texte pour une recherche partielle.
- Cliquer sur **Appliquer** pour lancer la recherche.
- Cliquer sur **Effacer** pour réinitialiser tous les filtres.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de l'agios : date d'émission (du/au), date d'opération (du/au) et date
de valeur (du/au).

### Exporter les agios en PDF

Bouton **Exporter PDF** dans la barre d'outils de l'écran. Génère un document PDF reprenant tous
les agios correspondant aux filtres actuellement appliqués (banque, statut, libellé, plages de
dates), sans limite de pagination : libellé, banque, montant, date d'émission et statut de chaque
agios, ainsi que le montant total de l'ensemble exporté. Le fichier est téléchargé automatiquement.

### Créer un agios

1. Cliquer sur **Nouveau**.
2. Renseigner les informations :
   - **Banque** : banque à laquelle l'agios est rattaché (obligatoire)
   - **Montant** : montant en MAD (strictement positif, obligatoire)
   - **Date d'émission** : date de facturation (obligatoire)
   - **Libellé** : intitulé de l'agios (obligatoire)
   - **Observation** : commentaire ou contexte (obligatoire)
3. Confirmer la création.

> L'agios est créé avec le statut **À payer (PREVU)**. Les dates de valeur et d'opération ne sont renseignées qu'au moment du rapprochement.

### Modifier un agios

1. Cliquer sur l'icône **Modifier** sur la ligne de l'agios.
2. Modifier les informations souhaitées.
3. Confirmer la modification.

> La modification n'est possible que si l'agios est encore **À payer (PREVU)**. Si l'agios est **Payé**, la modification est bloquée au niveau du rapprochement.

### Basculer le statut d'un agios (toggle)

> **Action réservée à l'administrateur système.** Le bouton de bascule n'est visible que pour le profil SYSTEM_ADMIN.

Il est possible de basculer manuellement un agios entre **À payer** et **Payé** sans passer par le rapprochement :

1. Sur la ligne de l'agios, cliquer sur le bouton de statut (icône en forme de bascule).
2. Confirmer le changement.

> Cette action est réservée aux corrections manuelles. Pour une validation normale, utiliser **États des encours → Récapitulatifs**, carte **Agios** (voir « Module États des encours » ci-dessous).

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un agios

> **Action réservée à l'administrateur système.** Le bouton "Supprimer" n'est visible que pour le profil SYSTEM_ADMIN. Le bouton est désactivé si l'agios est déjà Payé — un agios réglé ne peut plus être supprimé.

1. Cliquer sur l'icône **Supprimer** sur la ligne de l'agios.
2. Confirmer la suppression dans la fenêtre d'avertissement.

### Valider un agios

*Accès : tableau de bord Finance → carte d'une banque → États des encours → Récapitulatifs → carte **Agios***

Les agios en attente de paiement (statut Prévu) se valident depuis le récapitulatif de la banque, comme
toute autre opération (voir « Module États des encours » ci-dessous). Pour un agios :

1. Ouvrir la carte **Agios**. Les filtres **Libellé / Observation** et **Montant exact** permettent de
   retrouver une ligne ; **Exporter PDF** génère un état des lignes filtrées, avec le total.
2. Cliquer sur le bouton **Valider** de la ligne. La fenêtre « Passage au rapprochement bancaire »
   s'ouvre : **Date d'émission** (non modifiable), **TVA** (taux ou montant), **Date de valeur**,
   **Date de l'opération**, **Libellé** et **Observation** (tous obligatoires).
3. Cliquer sur **Valider**.

> La validation crée automatiquement une transaction dans le rapprochement affiché et l'agios passe au
> statut **Payé** ; il n'apparaît plus dans les encours.

> **Attention :** la validation n'est possible que si un rapprochement mensuel est ouvert (En cours ou
> Clôture provisoire) pour cette banque.

### Agios générés automatiquement

Certains agios récurrents sont configurés et générés **automatiquement en fin de mois**. Ils apparaissent dans la liste avec le statut **À payer (PREVU)** — aucune action n'est nécessaire pour les créer.

### Calcul automatique des agios (Facilité de Caisse, Préfinancement, Droits Constatés, Factoring)

*Accès : sidebar → Agios → Calcul des Agios*

Le tableau de bord de calcul affiche les périodes d'agios calculées automatiquement pour chaque banque, pour quatre types de charges : **Facilité de Caisse**, **Préfinancement**, **Droits Constatés** et **Factoring**. Pour chaque période :

- **OUVERT** : calculs provisoires, mis à jour chaque nuit automatiquement.
- **CLOTURÉ** : montants définitifs, agios créé automatiquement.

> Pour la Facilité de Caisse, le Préfinancement et les Droits Constatés, la période est un **trimestre** (T1 à T4). Pour le Factoring, la période est un **mois** — le tableau affiche alors "M1" à "M12" dans la colonne correspondante.

**Filtrer le tableau :**
- **Banque** : menu déroulant, chargé progressivement au défilement.
- **Type d'agios** : Facilité de Caisse, Préfinancement, Droits Constatés ou Factoring.
- **Année**, **Trimestre** (ou mois pour le Factoring), **Statut** (En cours / Clôturé).

**Consulter le détail d'une période :**
Cliquer sur l'icône **Voir le détail** sur une ligne du tableau → accès au détail journalier (solde ou encours selon le type, base de calcul, agios HT/TVA/TTC, taux appliqué). Pour le Préfinancement, les Droits Constatés et le Factoring, une colonne **Marché** identifie l'origine de chaque ligne. Un filtre **date limite** permet de consulter la situation à une date antérieure à la fin de la période. Pour ces mêmes trois types (Préfinancement, Droits Constatés, Factoring), un filtre **Marché** (menu déroulant "Marché : Tous") permet de n'afficher que les lignes journalières d'un marché précis — le menu ne propose que les marchés effectivement présents dans le détail de la période consultée. Ce filtre n'est pas disponible pour la Facilité de Caisse, qui n'est jamais rattachée à un marché.

**Exporter le tableau de bord en PDF :**

Bouton **Exporter PDF** en haut du tableau de bord. Génère un document PDF reprenant toutes les
périodes correspondant aux filtres actuellement appliqués (banque, type, année, trimestre/mois,
statut) : banque, type, période, totaux HT/TVA/TTC, jours calculés et statut de chaque ligne, ainsi
que les totaux HT/TVA/TTC cumulés. Le fichier est téléchargé automatiquement.

**Exporter le détail journalier en PDF :**

Depuis la vue détail d'une période, bouton **Exporter PDF** en haut de l'écran. Génère un document
PDF reprenant toutes les lignes journalières correspondant aux filtres actuellement appliqués (date
limite, marché) : date, marché le cas échéant, solde ou encours, base de calcul, taux appliqué et
montants HT/TVA/TTC de chaque jour, ainsi que les totaux HT/TVA/TTC et le nombre de jours avec
découvert ou encours sur la période. Le fichier est téléchargé automatiquement.

**Recalculer manuellement :**

> **Clôturer une période est une action réservée à l'administrateur système.** Le bouton correspondant n'est visible que pour le profil SYSTEM_ADMIN. Le recalcul (global ou d'une période précise) est ouvert à toute l'équipe Finance.

- **Recalculer tout** (bouton en haut du tableau de bord) : relance le calcul pour toutes les banques, tous les types et toutes les périodes encore ouvertes (période en cours et périodes passées non clôturées) — accessible à toute l'équipe Finance. Si une période ne peut pas être calculée (par exemple, rapprochement du mois en cours pas encore créé), les autres périodes sont tout de même recalculées.
- **Recalculer** (icône sur une ligne, période OUVERT uniquement) : relance le calcul pour cette banque, ce type et cette période précise — accessible à toute l'équipe Finance.

> Le calcul automatique s'exécute chaque nuit à 02h00 heure de Casablanca. Il n'est généralement pas nécessaire de le déclencher manuellement.

**Clôturer une période :**

> **Action réservée à l'administrateur système.**

La clôture est définitive et peut se faire depuis le tableau de bord (icône sur une ligne OUVERT) ou depuis la vue détail. Une fois clôturée, la période n'est plus recalculée et un agios définitif est créé automatiquement (si le montant est supérieur à 0). Cet agios entre ensuite dans le circuit normal de validation via la vue Encours.

---

## Module Impôts et Taxes

*Accès : sidebar → Impôts et Taxes*

### Consulter les impôts et taxes

La liste affiche tous les impôts et taxes avec libellé, banque, montant, motif, date d'émission,
date d'opération, date de valeur, observation (ces trois derniers ajoutés le 2026-07-27) et statut
(Prévu / Payé). Elle est filtrable par banque, motif, statut et libellé.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de l'impôt/taxe : date d'émission (du/au), date d'opération (du/au) et
date de valeur (du/au).

### Créer un impôt/taxe

Bouton **Ajouter impôt/taxe**. Champs requis : banque, motif, montant (supérieur à zéro), date
d'émission, libellé, observation.

### Modifier un impôt/taxe

Bouton crayon depuis la liste. **Désactivé si l'impôt/taxe est déjà Payé** — un impôt/taxe payé
n'est plus modifiable.

### Changer le statut d'un impôt/taxe

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer un impôt/taxe

Bouton corbeille — **réservé à l'administrateur système**. **Désactivé si l'impôt/taxe est déjà
Payé.**

### Valider les impôts et taxes en attente

Les impôts et taxes « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Impôt / Taxe** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA). Un filtre **Motif Impôt/Taxe** est disponible quand ce type est sélectionné.

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur l'écran liste des impôts et taxes. Le PDF reprend l'intégralité des lignes correspondant aux filtres actuellement appliqués à l'écran (banque, motif, statut, libellé, plages de dates) — pas seulement la page actuellement affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Le fichier
est téléchargé directement dans le navigateur.

### Génération automatique

Certains impôts et taxes récurrents (ex. TVA mensuelle, IS trimestriel) sont générés
automatiquement chaque mois pour les banques concernées, sans action de l'utilisateur. Un impôt/taxe
émis par SAHAM BANK génère aussi automatiquement une commission associée.

### TVA générée depuis le module Cautions

Quand une caution est clôturée et génère une commission, une TVA correspondante apparaît
automatiquement dans ce module — aucune saisie n'est nécessaire. La fusion de plusieurs lignes de
TVA se fait depuis l'écran "TVA sur cautions" du module Cautions.

---

## Module Commissions

*Accès : sidebar → Les Commissions*

Ce module rassemble toutes les commissions bancaires, qu'elles aient été saisies manuellement ou
générées automatiquement par un autre module (Leasing, Virements, LCN, Mises à disposition,
Cautions, Impôts et Taxes).

### Consulter les commissions

La liste affiche toutes les commissions avec libellé, banque, montant, motif (type d'opération),
date d'émission, date d'opération, date de valeur, observation (ces trois derniers ajoutés le
2026-07-27) et statut (Prévu / Payé). Elle est filtrable par banque, motif, statut, libellé et
montant.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de la commission : date d'émission (du/au), date d'opération (du/au)
et date de valeur (du/au).

### Créer une commission

Bouton **Ajouter commission**. Champs requis : banque, motif (type d'opération), montant
(strictement positif), date d'émission, libellé, observation.

### Modifier une commission

Bouton crayon depuis la liste. Tous les champs sont modifiables, y compris pour une commission déjà
payée. Si la commission modifiée est de type Caution, la TVA associée (le cas échéant) est
recalculée automatiquement sur le nouveau montant.

### Changer le statut d'une commission

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer une commission

Bouton corbeille — **réservé à l'administrateur système**. Refusé si la commission est déjà Payée :
une commission réglée ne peut plus être supprimée. Si la commission supprimée est de type Caution,
la TVA associée (le cas échéant) est supprimée avec elle.

### Valider les commissions en attente

Les commissions « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Commission** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA). Un filtre **Motif Commission** est disponible quand ce type est sélectionné.

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur l'écran liste des commissions ainsi que sur l'écran
d'encours d'une banque. Le PDF reprend l'intégralité des lignes correspondant aux filtres
actuellement appliqués à l'écran (banque, motif, statut, libellé, montant, plages de dates pour la
liste ; motif, libellé, montant pour l'encours) — pas seulement la page actuellement affichée. Le
fichier est téléchargé directement dans le navigateur.

### Génération automatique

Une commission est générée automatiquement, selon les règles configurées par l'administrateur (voir
guide Administration), lors de :

- la validation d'une échéance de loyer leasing ;
- l'émission d'un virement (sauf mouvement interne entre banques Staport, avec une exception pour
  les décaissements BMCI) ;
- l'émission d'un LCN de décaissement ;
- l'émission d'une mise à disposition de décaissement ;
- la prise, l'échéance mensuelle ou la clôture d'une caution (voir module Caution).

Un impôt/taxe émis par la banque SAHAM BANK génère systématiquement une commission fixe de 5,50 DH,
indépendamment de toute règle configurée (voir module Impôts et Taxes).

Si aucune règle de commission n'est configurée pour la banque et le type d'opération concernés,
aucune commission n'est générée — ce n'est pas une erreur.

---

## Module Impayées

*Accès : sidebar → Impayées*

### Consulter les impayées

La liste affiche toutes les impayées avec banque, date d'émission, date d'opération, date de valeur
(ces deux dernières ajoutées le 2026-07-27), motif, numéro (numéro de chèque ou de LCN d'origine,
ajouté le 2026-08-03), montant, libellé, observation, type (encaissement/décaissement) et statut
(Prévu / Payé). Elle est filtrable par banque, motif, statut, type et libellé.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de l'impayée : date d'émission (du/au), date d'opération (du/au) et
date de valeur (du/au).

### Créer une impayée

Bouton **Ajouter** (icône +). Champs requis : banque, type (encaissement/décaissement), motif,
montant (strictement positif), date d'émission, libellé, observation. Le champ **numéro** (numéro
de chèque ou de LCN d'origine) devient obligatoire lorsque le motif sélectionné est Chèque, LCN
Encaissement, LCN Décaissement ou LCN Escompte — il reste facultatif pour tous les autres motifs.

### Modifier une impayée

Bouton crayon depuis la liste, accessible quel que soit le statut de l'impayée. Le champ numéro
suit la même règle qu'à la création (obligatoire selon le motif choisi).

### Changer le statut d'une impayée

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer une impayée

Bouton corbeille — **réservé à l'administrateur système**. Refusé si l'impayée est déjà Payée : une
impayée régularisée ne peut plus être supprimée.

### Valider les impayées en attente

Les impayées « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Impayé** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA). Les encaissements et les décaissements se trouvent sous la même carte (filtre **Sens**).

### Exporter en PDF

Un bouton **Exporter PDF** est disponible sur l'écran liste des impayées. Le PDF reprend l'intégralité des lignes correspondant aux filtres actuellement appliqués à l'écran (banque, motif, type, statut, libellé, plages de dates) — pas seulement la page actuellement affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Chaque ligne inclut désormais la colonne **numéro** (2026-08-03). Le fichier
est téléchargé directement dans le navigateur.

---

## Module Opérations Erronées

*Accès : sidebar → Opérations Erronées*

### Consulter les opérations erronées

La liste affiche toutes les opérations erronées avec libellé, banque, motif, montant, date
d'émission, date d'opération, date de valeur, observation (ces trois derniers ajoutés le
2026-07-27) et statut (Prévu / Payé). Elle est filtrable par banque, motif, statut et libellé.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de l'opération erronée : date d'émission (du/au), date d'opération
(du/au) et date de valeur (du/au).

### Créer une opération erronée

Bouton **Ajouter opération erronée**. Champs requis : banque, motif, montant, date d'émission,
libellé, observation.

### Modifier une opération erronée

Bouton crayon depuis la liste, accessible quel que soit le statut de l'opération erronée.

### Changer le statut d'une opération erronée

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer une opération erronée

Bouton corbeille — **réservé à l'administrateur système**. Refusé si l'opération erronée est déjà
Payée : une opération erronée régularisée ne peut plus être supprimée.

### Valider les opérations erronées en attente

Les opérations erronées « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Opération erronée** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Bouton **Exporter PDF** disponible sur l'écran liste. Le fichier généré reprend l'intégralité des opérations erronées correspondant aux filtres actuellement appliqués à l'écran (banque, statut, motif, libellé, plages de dates) — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Il inclut le libellé, la banque, le motif, le montant,
la date d'émission, le statut de chaque ligne, ainsi qu'un total général.

---

## Module Provision

*Accès : sidebar → Provisions*

### Consulter les provisions

La liste affiche toutes les provisions avec libellé, banque, motif, sens (Encaissement /
Décaissement), montant, date d'émission, date d'opération, date de valeur, observation et statut
(Prévu / Payé). Elle est filtrable par banque, motif, sens, statut et libellé.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de la provision : date d'émission (du/au), date d'opération (du/au) et
date de valeur (du/au).

### Créer une provision

Bouton **Ajouter provision**. Champs requis : banque, motif, sens (Encaissement ou Décaissement),
montant (toujours positif — c'est le sens qui détermine si la provision augmente ou diminue le
solde une fois validée), date d'émission, libellé, observation.

### Modifier une provision

Bouton crayon depuis la liste, accessible quel que soit le statut de la provision.

### Changer le statut d'une provision

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer une provision

Bouton corbeille — **réservé à l'administrateur système**. Refusé si la provision est déjà Payée :
une provision régularisée ne peut plus être supprimée.

### Valider les provisions en attente

Les provisions « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Provision** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA). Les encaissements et les décaissements se trouvent sous la même carte (filtre **Sens**) ; la TVA est déductible pour un décaissement, collectée pour un encaissement.

### Exporter en PDF

Bouton **Exporter PDF** disponible sur l'écran liste. Le fichier généré reprend l'intégralité des provisions correspondant aux filtres actuellement appliqués à l'écran (banque, statut, motif, sens, libellé, plages de dates) — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Il inclut le libellé, la banque, le motif, le
sens, le montant, la date d'émission, le statut de chaque ligne, ainsi qu'un total général.

---

## Module Restitution

*Accès : sidebar → Les Restitutions*

### Consulter les restitutions

La liste affiche toutes les restitutions avec libellé, banque, montant, date d'émission, date
d'opération, date de valeur, observation (ces trois derniers ajoutés le 2026-07-27) et statut
(Prévu / Payé). Elle est filtrable par banque, statut et libellé.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de la restitution : date d'émission (du/au), date d'opération (du/au)
et date de valeur (du/au).

### Créer une restitution

Bouton **Ajouter restitution**. Champs requis : banque, montant, date d'émission, libellé,
observation.

### Modifier une restitution

Bouton crayon depuis la liste, accessible quel que soit le statut de la restitution.

### Changer le statut d'une restitution

Interrupteur Prévu/Payé dans la liste — **réservé à l'administrateur système**.

> **Retour « en cours » d'une opération déjà rapprochée.** Si la transaction de cette opération figure dans un rapprochement, la remettre « en cours » (ou « prévu ») **supprime cette transaction** : l'opération redevient à valider et les soldes du rapprochement sont recalculés. Une fenêtre de confirmation le rappelle. Voir « Remettre une opération rapprochée en cours » dans le « Module États des encours ».

### Supprimer une restitution

Bouton corbeille — **réservé à l'administrateur système**. Refusé si la restitution est déjà Payée :
une restitution régularisée ne peut plus être supprimée.

### Valider les restitutions en attente

Les restitutions « Prévu » dont la date d'émission est antérieure ou égale à la fin du mois en cours ne se traitent plus dans un écran propre à ce module : ils se consultent, s'exportent en PDF et se valident (un par un ou en lot) depuis **États des encours → Récapitulatifs**, sous la carte **Restitution** — voir « Module États des encours » ci-dessous pour le pas-à-pas (champs demandés : date de valeur, date de l'opération, libellé, observation, TVA).

### Exporter en PDF

Bouton **Exporter PDF** disponible sur l'écran liste. Le fichier généré reprend l'intégralité des restitutions correspondant aux filtres actuellement appliqués à l'écran (banque, statut, libellé, plages de dates) — pas seulement la page affichée. L'export des encours se fait depuis **États des encours → Récapitulatifs** (bouton **Exporter PDF** du tableau). Il inclut le libellé, la banque, le montant, la date d'émission, le
statut de chaque ligne, ainsi qu'un total général.

---

## Module Rapprochement

*Accès : sidebar → Historique des Rapprochements*

### Comprendre le rapprochement

Chaque banque possède, à tout instant, une période de rapprochement **En cours** qui accueille au
fil de l'eau toutes les transactions produites par les autres modules. Lorsque le mois se termine,
cette période est **clôturée provisoirement** en vue d'un contrôle, puis **clôturée définitivement**
une fois le contrôle terminé. Une nouvelle période En cours est ouverte automatiquement dès la
clôture provisoire de la précédente — aucune action manuelle n'est nécessaire pour démarrer le mois
suivant.

### Consulter l'historique des rapprochements

Écran principal, accessible depuis la sidebar. Liste paginée de tous les rapprochements, toutes
banques confondues, avec pour chaque ligne : la date (mois/année), la banque, le solde de début, le
solde de fin, le total débit, le total crédit, la TVA déductible, la TVA collectée, la TVA due et le
statut (En cours / Clôture provisoire / Clôturé définitivement, affiché sous forme de badge —
ajoutés le 2026-07-27). La liste est filtrable par banque, par date (au format MM/AAAA) et par
statut (En cours, Clôture provisoire, Clôturé définitivement), et triable sur les colonnes date,
solde de début, solde de fin, total débit, total crédit, TVA déductible et TVA collectée.

### Consulter le détail d'un rapprochement

Bouton œil sur une ligne de l'historique. Affiche les soldes et totaux de la période, ainsi que la
liste paginée et filtrable (libellé, observation, montant, source) des transactions qui lui sont
rattachées. Depuis cet écran, une transaction peut être corrigée (voir "Modifier une transaction"
ci-dessus) tant que le rapprochement n'est pas clôturé définitivement.

Le bouton **Période** ouvre un panneau de recherche par plage de dates, avec un filtre indépendant
pour chacune des trois dates de la transaction : date d'émission (du/au), date d'opération (du/au)
et date de valeur (du/au) — même comportement que sur l'écran Historique des transactions.

### Clôturer provisoirement un rapprochement

Action déclenchée depuis le bouton de clôture en haut de l'espace de travail **États des encours**
(voir « Module États des encours » ci-dessous), une fois la période En cours sélectionnée pour la banque concernée.
Fige le solde de fin et les totaux de la période En cours, et ouvre automatiquement le mois suivant
avec le solde de fin qui vient d'être calculé comme solde de départ. Refusée si une clôture
provisoire existe déjà pour cette banque — un message l'indique.

### Clôturer définitivement un rapprochement

Action disponible uniquement sur un rapprochement en Clôture provisoire. **Irréversible** : une
fois confirmée, plus aucune transaction de la période ne peut être ajoutée, modifiée ou annulée.
Refusée si le rapprochement n'est pas en Clôture provisoire — un message l'indique.

### Télécharger le rapport PDF d'un rapprochement

Bouton PDF sur une ligne de l'historique, ou bouton **Exporter PDF** sur l'écran de détail d'un
rapprochement (nouveau, 23/07/2026). Dans les deux cas, ouvre une fenêtre de choix entre deux
versions du rapport :
- **Finance** — inclut la TVA et la date d'émission de chaque transaction.
- **Comptabilité** — sans TVA ni date d'émission.

Depuis l'écran de détail, le rapport ne reprend que les transactions correspondant aux filtres
actuellement appliqués sur cet écran (libellé, observation, montant, source, dates d'émission,
d'opération et de valeur) ; les soldes et totaux en en-tête du rapport restent ceux de la période
entière. Depuis l'écran historique, le rapport reprend toujours l'intégralité des transactions de
la période, sans filtre.

Le fichier téléchargé est nommé `Rapport_Rapprochement_<Banque>_<MM/AAAA>.pdf`.

### Droits requis

Toutes les actions du module (consultation, clôture provisoire, clôture définitive,
téléchargement du rapport) sont accessibles à l'équipe Finance (membre ou directeur) et à
l'administrateur système — aucune action n'est réservée à un sous-ensemble de ces profils.

---

## Module États des encours

*Accès : tableau de bord Finance → cliquer sur la carte d'une banque (logo ou corps de la carte)*

### Comprendre les encours

Un encours est une opération bancaire (échéance de crédit ou de leasing, chèque, virement, LCN,
préfinancement, agios, impôt, etc.) qui n'a pas encore été validée contre le relevé bancaire réel.
Cet espace de travail regroupe, pour une banque donnée, l'ensemble des encours en attente et permet
de les consulter, de les valider, d'en créer ou d'en corriger, puis — une fois validés — de corriger
ou d'annuler les transactions qui en résultent.

C'est le **seul** endroit où l'on valide une opération : aucun module (Chèques, Virements, Agios,
etc.) n'a plus son propre écran d'encours.

### Choisir la période à travailler

Cliquer sur la carte d'une banque ouvre l'écran de sélection du rapprochement actif : une carte
**En cours** est toujours proposée, et une carte **Clôture provisoire** apparaît en plus si une
période est en cours de contrôle. Choisir une carte ouvre l'espace de travail sur cette période.

### Naviguer dans l'espace de travail

L'écran affiche en haut le contexte de la banque et de la période (solde de début, solde de fin,
variation, facilité de caisse), avec le bouton de clôture (voir « Module Rapprochement » ci-dessus).
À côté du fil d'Ariane, une pastille indique la nature de l'écran affiché — **Prévisionnel** (des
opérations attendues, pas encore passées en banque) ou **Réalisé** (des opérations déjà passées sur
le compte). Chaque écran affiche ses propres totaux, au-dessus de son tableau, pour qu'un total
prévisionnel ne puisse pas être lu comme un montant acquis. Une barre latérale à gauche donne accès à
deux écrans :
- **Récapitulatifs** (ouvert par défaut) : toutes les opérations en attente, regroupées par type.
- **Rapprochement** : les transactions déjà validées de la période affichée, avec leurs totaux
  réalisés (débit, crédit, TVA déductible, TVA collectée, TVA due).

### Lire l'écran Récapitulatifs

L'écran se lit de haut en bas :

1. **Le bouton Ajouter** (en haut à droite) — voir « Ajouter une opération ».
2. **La barre de filtres** — voir « Filtrer les opérations ».
3. **Quatre indicateurs** qui suivent les filtres : **Total débit prévisionnel**, **Total crédit
   prévisionnel**, **Opérations à valider**, **Échues à ce jour** (en rouge dès qu'au moins une
   opération est arrivée à échéance).
4. **Une carte par type d'opération** (Leasing, Crédit Moyen Terme, Chèque, LCN, Virement, Virement
   international, Mise à disposition, Préfinancement, Factoring, Droit constaté, Décomptes,
   Commission, Agios, Frais bancaire, Prélèvement, Impôt / Taxe, Restitution, Opération erronée,
   Impayé, Provision…). Chaque carte indique le nombre d'opérations à valider, le total **Débit** et
   le total **Crédit**, et — si des opérations sont échues — un repère d'alerte avec leur nombre
   (survol : « opération(s) échue(s) : date de référence atteinte à ce jour »). Un type sans
   opération n'a pas de carte.

Quand aucune opération ne correspond, le message **« Aucune opération à valider »** s'affiche ; si
des filtres sont actifs, il propose **Effacer les filtres**.

**Ouvrir une carte** : cliquer sur son en-tête. La carte s'ouvre en pleine largeur et passe en tête
de liste ; les autres cartes se compactent (une seule carte est ouverte à la fois). Elle affiche ses
statistiques — **Débit**, **Crédit**, **Net (crédit − débit)**, **Échues à ce jour** — puis le
tableau de ses opérations. Le bouton **Réduire** referme la carte.

**Le tableau d'une carte** présente une ligne par opération : **Sens** (pastille Décaissement ou
Encaissement), **Date d'émission**, **Date d'échéance** (« — » si le type n'en a pas), **Libellé**,
**Observations**, **Source**, **Débit**, **Crédit**, **ID source** et **Actions**. Il est paginé (5,
10, 25 ou 50 lignes ; 10 par défaut) et triable en cliquant sur un en-tête. Tant que vous n'avez
cliqué sur aucun en-tête, il est trié par date de référence, de la plus récente à la plus ancienne
(la date de référence est l'échéance pour le leasing, le CMT et les LCN, l'émission pour les autres
types).

### Filtrer les opérations

La barre de filtres est toujours visible. Elle s'applique à la fois aux indicateurs, aux cartes et
au tableau. Critères, tous combinables :

- **Sens** : Tous (par défaut), Encaissement ou Décaissement. Quand un sens est choisi, le total de
  l'autre tombe à zéro.
- **Type de transaction** : un ou plusieurs types (liste avec recherche) ; vide = tous les types.
- **Libellé / Observation** : un seul champ ; le terme est cherché à la fois dans le libellé et dans
  l'observation, sans tenir compte des majuscules ni des accents. Il cherche aussi le nom de
  l'émetteur (virements, virements internationaux, chèques, LCN, mises à disposition) et de
  l'organisme (leasing).
- **N° de pièce** : numéro du chèque, du LCN, du virement, de la mise à disposition, de l'impayé, du
  marché (préfinancement, factoring, droit constaté, décompte) ou du contrat (leasing). Les autres
  types n'ont pas de numéro. Une bulle d'aide explique ce champ.
- **Montant exact**.
- **Motif Commission** et **Motif Impôt/Taxe** : listes déroulantes qui n'apparaissent que si le type
  correspondant est sélectionné dans « Type de transaction ». Elles ne restreignent que ce type : les
  autres types sélectionnés restent affichés.
- **Période** (bouton **Période**, qui affiche le nombre de bornes actives) : **Date de référence —
  depuis** et **jusqu'au**. Chaque opération est comparée à sa propre date de référence (une bulle
  d'aide le rappelle) ; si vous ne saisissez pas de borne haute, la fin du mois en cours reste
  utilisée.

Le bouton **Effacer** (visible dès qu'un filtre est actif) réinitialise tous les filtres.

### Valider une opération en attente

1. Ouvrir la carte du type d'opération concerné.
2. Cliquer sur le bouton rond vert **Valider** (coche) de la ligne. Une fenêtre **« Passage au
   rapprochement bancaire »** s'ouvre après un bref chargement.
3. Compléter la fenêtre :
   - **Date d'émission** : reprise de l'opération, non modifiable ;
   - **TVA** : soit un **taux**, soit un **montant** de TVA (jamais les deux) ; le montant ne peut
     pas dépasser le montant de l'opération. Pour le leasing et le CMT, la TVA est facultative ;
   - **Date de valeur** (obligatoire) ;
   - **Date de l'opération** (obligatoire) ;
   - **Libellé** (obligatoire) ;
   - **Observation** (obligatoire).
4. Cliquer sur **Valider**. Le bouton reste grisé tant que le formulaire est incomplet ou invalide ;
   si une erreur subsiste, le message **« Veuillez remplir tous les champs obligatoires et corriger
   les erreurs. »** s'affiche. **Annuler** referme la fenêtre sans rien enregistrer.
5. Un message de confirmation s'affiche : l'opération disparaît du récapitulatif, les indicateurs se
   mettent à jour et la transaction apparaît dans l'écran **Rapprochement** de la période affichée.

Le sens de la TVA découle du sens de l'opération (déductible pour un décaissement, collectée pour un
encaissement). Si une ligne affiche un tiret à la place du bouton **Valider**, cette opération ne
peut pas être validée depuis le récapitulatif. Voir la section du guide dédiée au type d'opération
concerné pour ses cas particuliers.

### Valider plusieurs opérations en lot

1. Ouvrir la carte du type d'opération concerné.
2. Cocher les lignes à valider (la case d'en-tête coche toutes les lignes de la page). Une barre
   apparaît au-dessus du tableau : **« n opération(s) sélectionnée(s) — montant total MAD »**, avec
   les boutons **Effacer la sélection** et **Valider en lot (n)**.
3. Cliquer sur **Valider en lot**. La fenêtre **« Validation en lot »** rappelle le type, le sens, le
   nombre d'opérations et le montant total, et demande :
   - **TVA** : un **taux** (le montant libre n'est pas offert en lot) ;
   - **Date de valeur** (obligatoire) ;
   - **Date de l'opération** (obligatoire).
   Le libellé, l'observation et la date d'émission de chaque opération sont conservés tels quels.
4. Cliquer sur **Valider le lot**, puis confirmer dans la fenêtre de confirmation qui rappelle les
   valeurs saisies.
5. Le message **« n opération(s) validée(s) avec succès. »** s'affiche. Le traitement est
   **tout-ou-rien** : si une opération échoue, aucune n'est validée et le message d'erreur le précise.

> **Sélection hétérogène.** Un lot ne peut porter que sur des opérations **du même type et du même
> sens**. Si la sélection en mélange plusieurs, le message **« La validation en lot ne peut porter
> que sur des opérations d'un même type et d'un même sens… »** s'affiche : videz la sélection et
> recommencez avec une seule combinaison type/sens à la fois.

### Ajouter une opération

Le bouton **Ajouter** existe en haut de l'écran et dans la barre du tableau d'une carte ouverte.
- Dans la barre du tableau d'une carte dont le type peut être créé depuis le récapitulatif, il ouvre
  directement l'écran de création de ce type.
- Sinon (bouton en haut de l'écran, ou carte dont le type ne se crée pas d'ici), il ouvre la fenêtre
  **« Ajouter une opération »** : une grille de types d'opérations avec un champ de recherche (les
  majuscules et les accents sont ignorés : « cheque » trouve « Chèque »). Cliquer sur un type ouvre
  son écran de création ; à l'enregistrement, vous revenez au récapitulatif. Les types dont la
  création n'est pas possible depuis ici sont grisés, avec la mention « Création indisponible »
  (c'est le cas du leasing, du CMT, des mises à disposition et des cautions).

### Éditer une opération en attente

Le bouton rond bleu **Éditer** (crayon) de chaque ligne ouvre l'écran de modification de l'opération
dans son module d'origine ; à l'enregistrement ou à l'annulation, vous revenez au récapitulatif.
Le bouton est grisé (survol : « Édition non disponible pour cette source depuis le récapitulatif »)
dans les cas suivants :
- mises à disposition et LCN en décaissement : jamais éditables depuis le récapitulatif ;
- virements et chèques : éditables uniquement en encaissement ;
- échéances de leasing et de CMT : éditables uniquement par l'**administrateur système**, et
  seulement tant que l'échéance est « prévue ».

### Exporter en PDF

Le bouton **Exporter PDF** de la barre du tableau génère un PDF des lignes correspondant exactement
aux filtres appliqués, sans limite de pagination. Le fichier se télécharge directement dans le
navigateur. L'écran **Rapprochement** dispose du même bouton, pour les transactions filtrées de la
période.

### Corriger ou annuler une transaction validée

Dans l'écran **Rapprochement**, deux boutons existent à la fin de chaque transaction :

- **Modifier** (crayon) : ouvre la fenêtre **« Modifier une transaction »** avec les mêmes champs que
  lors de la validation — date d'émission (non modifiable), TVA (taux ou montant), date de valeur,
  date de l'opération, libellé, observation. Le montant et la banque ne peuvent pas être modifiés.
  Cliquer sur **Enregistrer** : la correction est répercutée automatiquement sur l'opération d'origine.
- **Supprimer** (corbeille) : annule la transaction. Le message **« Transaction supprimé avec
  success »** s'affiche ; l'opération d'origine redevient « à valider » (ses dates de valeur et
  d'opération sont effacées) et réapparaît dans l'écran **Récapitulatifs** ; les soldes du
  rapprochement sont recalculés.

> **Impossible si le rapprochement est clôturé définitivement** : ni modification ni annulation — un
> message d'erreur l'indique. Tant que la période est En cours ou en Clôture provisoire, ces deux
> actions sont possibles.

### Remettre une opération rapprochée « en cours » (administrateur système)

Dans la liste de chaque type d'opération (Chèques, Virements, Agios, Commissions, etc., ainsi que les tableaux d'amortissement du leasing et du CMT), l'interrupteur de statut réservé à l'administrateur système permet de corriger un statut à la main.

- **Vers « réglée » / « payé »** : le statut change, sans créer de transaction (correction exceptionnelle).
- **Vers « en cours » / « prévu »** : une fenêtre de confirmation rappelle que, **si l'opération est rapprochée**, sa transaction sera supprimée et les soldes du rapprochement recalculés. Après confirmation, l'opération redevient à valider (ses dates de valeur et d'opération sont effacées) et réapparaît dans **Récapitulatifs** — exactement comme après la suppression de la transaction dans l'écran **Rapprochement**. Si l'opération n'a pas de transaction (marquée réglée à la main), seul le statut change.

> **Refusé** avec un message d'erreur si le rapprochement est **clôturé définitivement**, ou si plusieurs transactions pointent sur la même opération : dans ce cas, annuler les transactions depuis l'écran **Rapprochement**.

### Consulter et corriger le rapprochement en cours

L'écran **Rapprochement** liste, de façon paginée et filtrable (libellé, source, observation,
montant, dates), les transactions déjà rattachées à la période affichée — même contenu que
« Consulter le détail d'un rapprochement » (voir « Module Rapprochement » ci-dessus).

### Droits requis

Toutes les actions de cet espace (consulter, valider, valider en lot, ajouter, éditer, corriger ou
annuler une transaction, exporter) sont accessibles à l'équipe Finance (membre ou directeur) et à
l'administrateur système. Seule exception : l'édition d'une échéance de leasing ou de CMT, réservée à
l'administrateur système.

---

## Consulter mes notifications

*Accès : icône cloche dans la barre de navigation, en haut de l'écran — disponible pour tout utilisateur connecté*

Une pastille rouge sur la cloche indique le nombre de notifications non lues. Cliquer sur la cloche
ouvre la liste de vos notifications (échéances à venir sur les lignes fonctionnelles, contrats de
leasing ou crédits moyen terme, et autres informations diffusées par G-Banque).

### Marquer une notification comme lue

Cliquer sur une notification dans la liste, puis confirmer. Le compteur de notifications non lues se
met à jour.
