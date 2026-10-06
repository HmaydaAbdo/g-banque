# G-Banque — Spécification fonctionnelle complète

> Ce document est la spécification fonctionnelle complète de G-Banque, le système de gestion et de
> suivi des opérations bancaires internes de Staport (entreprise de bâtiment et travaux publics). Il
> s'adresse à l'équipe Finance, au chef de projet et à tout futur développeur découvrant le
> fonctionnement métier de l'application — aucun profil technique n'est requis pour le lire, et
> aucun détail technique n'y figure.
>
> Il rassemble, dans un même récit organisé en onze parties, les 31 modules fonctionnels de
> G-Banque : chaque partie amène logiquement à la suivante, du socle d'accès jusqu'aux modules
> transverses de traçabilité et de notification.
>
> Pour la note de synthèse destinée à la direction : `docs/direction/README.md`.
> Pour le détail des accès et autorisations par module : `docs/rbac/README.md`.
> Pour le guide d'utilisation pas-à-pas destiné aux utilisateurs finaux : `docs/guide-utilisateur/`.

---

## Sommaire

- **Socle d'accès**
  - [Chapitre 1 — Module Authentification](#chap1)
  - [Chapitre 2 — Module Département](#chap2)
  - [Chapitre 3 — Module Utilisateurs](#chap3)
- **Comptes bancaires**
  - [Chapitre 4 — Module Comptes bancaires](#chap4)
- **Produits financiers rattachés au compte**
  - [Chapitre 5 — Module Crédit Moyen Terme (CMT)](#chap5)
  - [Chapitre 6 — Module Lignes Fonctionnelles](#chap6)
  - [Chapitre 7 — Module Leasing](#chap7)
- **Marchés et satellites**
  - [Chapitre 8 — Module Marchés](#chap8)
  - [Chapitre 9 — Module Caution](#chap9)
  - [Chapitre 10 — Module Décomptes](#chap10)
  - [Chapitre 11 — Module Préfinancements](#chap11)
  - [Chapitre 12 — Module Factoring](#chap12)
  - [Chapitre 13 — Module Droits Constatés](#chap13)
- **Moyens de paiement**
  - [Chapitre 14 — Module Chèques](#chap14)
  - [Chapitre 15 — Module LCN](#chap15)
  - [Chapitre 16 — Module Virements](#chap16)
  - [Chapitre 17 — Module Virements Internationaux](#chap17)
  - [Chapitre 18 — Module Mises à Disposition](#chap18)
  - [Chapitre 19 — Module Recap Encaissement / Recap Décaissement](#chap19)
  - [Chapitre 20 — Module Prélèvements](#chap20)
- **Transactions**
  - [Chapitre 21 — Module Transactions](#chap21)
- **Charges appliquées**
  - [Chapitre 22 — Module Frais bancaires](#chap22)
  - [Chapitre 23 — Module Agios](#chap23)
  - [Chapitre 24 — Module Impôts et Taxes](#chap24)
  - [Chapitre 25 — Module Commission](#chap25)
- **Anomalies**
  - [Chapitre 26 — Module Impayées](#chap26)
  - [Chapitre 27 — Module Opérations Erronées](#chap27)
  - [Chapitre 28 — Module Provision](#chap28)
  - [Chapitre 29 — Module Restitution](#chap29)
- **Rapprochement**
  - [Chapitre 30 — Module Rapprochement](#chap30)
- **États des encours / Récapitulatif**
  - [Chapitre 31 — Module États des encours et Récapitulatif](#chap31)
- **Transverse**
  - [Chapitre 32 — Module Logs](#chap32)
  - [Chapitre 33 — Module Notifications](#chap33)

---

# Socle d'accès

---

<a id="chap1"></a>

## Chapitre 1 — Module Authentification


> Source de vérité métier pour le module Authentification.

---

### 1. Périmètre du module

Ce module est la porte d'entrée unique de G-Banque. Il répond à deux questions pour chaque collaborateur :

- **Qui êtes-vous ?** (authentification : vérification de l'identité)
- **Que pouvez-vous faire ?** (autorisation : droits accordés selon le profil)

Ce module ne gère aucune donnée métier. Il produit les droits d'accès qui conditionnent l'ensemble des autres modules.

---

### 2. Définitions métier

**Identifiant (username)** : identifiant unique de connexion attribué à chaque collaborateur. Immuable après création.

**Session** : période d'accès actif pour un collaborateur connecté. Dans G-Banque, une session n'est pas maintenue côté serveur — elle est entièrement portée par un jeton numérique (voir ci-dessous).

**Jeton d'accès** : titre numérique émis à la connexion, valable 4 heures. Il contient l'identité du collaborateur et ses droits. Une fois expiré, le collaborateur doit se reconnecter. Aucun jeton n'est stocké côté serveur.

**Profil** : ensemble des droits accordés à un collaborateur. Un profil est défini par :
- son appartenance à un ou plusieurs départements, avec un rôle dans chacun (voir module Département et module Utilisateurs)
- son éventuel statut d'administrateur système (droits transversaux)

---

### 3. Système de droits d'accès

Les droits dans G-Banque sont organisés en deux niveaux.

#### Administrateur système

Accès complet à tous les modules, sans restriction de département. Ce profil est distinct des profils métier — un administrateur système ne fait pas partie d'un département.

Actions réservées à l'administrateur système : gestion des utilisateurs, gestion des départements, suppression de données critiques, corrections manuelles de statuts.

#### Profil département

Un collaborateur appartient à un ou plusieurs départements, avec un rôle dans chacun :

| Rôle | Droits |
|------|--------|
| Membre | Lecture et saisie standard dans le périmètre du département |
| Directeur | Accès complet au périmètre du département, incluant les actions sensibles (clôtures, annulations) |

Un collaborateur peut cumuler plusieurs appartenances : par exemple, Membre du département Finance et Directeur du département Paiements.

Les droits accordés par un département ne s'appliquent qu'au périmètre de ce département — un Directeur Finance n'a pas accès au module Trésorerie s'il n'y appartient pas.

#### Routes publiques

Seule la page de connexion est accessible sans authentification. Toutes les autres pages nécessitent un jeton d'accès valide.

---

### 4. Cycle de vie d'une session

```
Collaborateur saisit identifiant + mot de passe
        │
        ▼
Vérification des identifiants
        │
        ├── Identifiants incorrects → message d'erreur
        │
        ├── Compte verrouillé (trop de tentatives) → message de blocage temporaire
        │
        └── Identifiants corrects → émission d'un jeton d'accès (valable 4 heures)
                │
                ▼
        Accès aux modules selon les droits portés par le jeton
                │
                ▼ (au bout de 4 heures)
        Jeton expiré → redirection automatique vers la page de connexion
        Le collaborateur retrouve sa page initiale après reconnexion.
```

---

### 5. Protection contre les tentatives de connexion abusives

Le système surveille les tentatives de connexion échouées par compte.

| Paramètre | Valeur |
|-----------|--------|
| Nombre de tentatives avant blocage | 5 tentatives échouées consécutives |
| Durée du blocage | 15 minutes |
| Comportement pendant le blocage | La tentative est rejetée avant même la vérification du mot de passe |

Après la période de blocage, le compteur est remis à zéro automatiquement.

---

### 6. Règles métier

1. Aucune page de G-Banque n'est accessible sans jeton d'accès valide — sans exception.
2. Un jeton expiré (après 4 heures) est équivalent à une absence de connexion.
3. Un compte désactivé ne peut pas se connecter, même avec les bons identifiants.
4. Après 5 tentatives échouées sur un même compte, toute nouvelle tentative est bloquée 15 minutes.
5. Un collaborateur ne peut consulter que son propre profil de connexion — jamais celui d'un autre.
6. Le mot de passe ne circule jamais en clair, ni dans les réponses du système, ni dans les journaux d'activité.
7. Les droits d'accès sont recalculés à chaque connexion — un changement de droits ne prend effet qu'à la prochaine connexion.

---

### 7. Qui peut faire quoi

| Action | Qui |
|--------|-----|
| Se connecter | Tout collaborateur avec un compte actif |
| Consulter son propre profil | Tout collaborateur connecté |
| Gérer les comptes utilisateurs | Administrateur système uniquement |
| Gérer les départements | Administrateur système uniquement |

---

<a id="chap2"></a>

## Chapitre 2 — Module Département


> Source de vérité métier pour le module Département.

---

### 1. Périmètre du module

Ce module permet à l'administrateur système de définir et gérer les départements métier de l'organisation. Un département représente une unité fonctionnelle de l'entreprise (Finance, Trésorerie, Paiements, etc.) à laquelle les collaborateurs sont rattachés.

Les départements sont la brique fondamentale du système de droits d'accès de G-Banque : l'appartenance à un département, combinée à un rôle (Membre ou Directeur), détermine ce qu'un collaborateur peut faire dans l'application.

Aucun redéploiement de l'application n'est nécessaire pour ajouter, renommer ou supprimer un département — tout se fait depuis l'interface d'administration.

---

### 2. Définitions métier

**Département** : unité organisationnelle de l'entreprise, représentée dans G-Banque par un nom et une description optionnelle. Son rôle est d'organiser les droits d'accès des collaborateurs.

**Appartenance** : lien entre un collaborateur et un département, avec un rôle associé. Un collaborateur peut appartenir à plusieurs départements simultanément, avec potentiellement des rôles différents dans chacun.

**Rôle dans un département** :

| Rôle | Droits conférés |
|------|-----------------|
| Membre | Lecture et saisie standard dans les modules du département |
| Directeur | Accès complet au périmètre du département, incluant les actions sensibles (clôtures, annulations, suppressions) |

---

### 3. Lien entre départements et droits d'accès

Le système de droits fonctionne ainsi :

1. Un département est créé par l'administrateur (ex. : `FINANCE`).
2. Un ou plusieurs collaborateurs sont rattachés à ce département avec un rôle (Membre ou Directeur).
3. Ce rattachement leur donne accès aux modules et fonctionnalités du périmètre Finance dans G-Banque.

Les droits sont toujours liés à un département précis : un Directeur du département Finance n'a pas automatiquement les droits du département Trésorerie si le rattachement n'a pas été fait.

---

### 4. Cycle de vie d'un département

```
Création
  │  (nom unique + description optionnelle)
  ↓
Département actif
  │  → Des collaborateurs peuvent être rattachés
  │  → Le département peut être renommé ou sa description modifiée
  │
  ↓ (condition : aucun collaborateur rattaché)
Suppression
```

La suppression est immédiate et irréversible. Elle n'est possible que si aucun collaborateur n'y est encore rattaché.

---

### 5. Convention de nommage

Le nom d'un département est visible dans les journaux d'audit et les rapports.

**Convention recommandée : nom en majuscules, sans espace, sans accent.**

Exemples : `FINANCE`, `TRESORERIE`, `CREDITS`, `MARCHES`, `PAIEMENTS`.

Un nom mal formé (avec espaces ou caractères spéciaux) ne bloque pas le fonctionnement mais peut nuire à la lisibilité dans les journaux et rapports d'audit.

---

### 6. Règles métier

1. Le nom d'un département doit être unique dans le système.
2. La description est facultative — un département peut exister sans description.
3. Un département ne peut pas être supprimé s'il a au moins un collaborateur rattaché, quel que soit le rôle de ce collaborateur.
4. Renommer ou modifier la description d'un département est possible à tout moment, même s'il contient des collaborateurs actifs.
5. Il n'y a pas de limite au nombre de départements dans le système.
6. L'administrateur système n'appartient à aucun département — son accès est transversal et ne dépend d'aucun département.

---

### 7. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter la liste des départements | Administrateur système uniquement |
| Créer un département | Administrateur système uniquement |
| Modifier un département | Administrateur système uniquement |
| Supprimer un département | Administrateur système uniquement |

Les collaborateurs métier (membres, directeurs) ne voient pas ce module dans l'interface.

---

### 8. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|--------|------------------------|
| **Utilisateurs** | Les collaborateurs sont rattachés à des départements via le module Utilisateurs. Un département sans appartenance est vide mais peut être conservé. |
| **Authentification** | Les droits d'accès portés par le jeton de connexion sont construits à partir des appartenances à des départements. Une modification de rattachement ne prend effet qu'à la prochaine connexion. |

---

<a id="chap3"></a>

## Chapitre 3 — Module Utilisateurs


> Source de vérité métier pour le module Utilisateurs.

---

### 1. Périmètre du module

Ce module permet à l'administrateur système de gérer l'ensemble des comptes collaborateurs de G-Banque :

- créer un compte
- activer ou désactiver un compte
- attribuer des droits par département (et modifier ou retirer ces droits)
- changer un mot de passe
- accorder ou retirer le statut d'administrateur système

C'est ici que se pilote **qui accède à quoi dans G-Banque**, sans toucher au code ni redéployer l'application.

---

### 2. Définitions métier

**Compte utilisateur** : enregistrement d'un collaborateur dans G-Banque. Il est identifié par un identifiant unique (username) et porte un mot de passe, un statut actif/inactif, et ses droits d'accès.

**Identifiant (username)** : clé d'identification unique et immuable du compte. Une fois créé, il ne peut jamais être modifié.

**Statut actif / inactif** : un compte inactif ne peut plus se connecter, même avec les bons identifiants. La désactivation est immédiate et réversible.

**Appartenance à un département** : lien entre un compte utilisateur et un département, avec un rôle (Membre ou Directeur). Un utilisateur peut appartenir à plusieurs départements simultanément.

**Rôle dans un département** :

| Rôle | Droits conférés |
|------|-----------------|
| Membre | Lecture et saisie standard dans le périmètre du département |
| Directeur | Accès complet au périmètre du département, incluant les actions sensibles |

---

### 3. Les deux types de profils

#### Administrateur système

Profil transversal avec accès complet à tous les modules. Un administrateur système n'appartient à aucun département — son accès ne dépend pas d'un périmètre métier.

Ce statut est accordé ou retiré manuellement par un autre administrateur système.

#### Utilisateur avec droits département

Profil dont les droits sont définis par ses appartenances aux départements. Un utilisateur peut cumuler plusieurs départements, avec des rôles potentiellement différents dans chacun.

Exemple : Membre du département Finance et Directeur du département Paiements.

**Principe du moindre privilège** : tout nouveau compte est créé sans aucune appartenance à un département. Les accès doivent être accordés explicitement. Par défaut, un nouveau collaborateur ne voit aucune donnée métier.

---

### 4. Cycle de vie d'un compte utilisateur

```
Création du compte
  │  (username unique, mot de passe ≥ 8 caractères, statut actif par défaut)
  ↓
Compte actif, sans droits
  │
  ↓ (attribution de départements et rôles)
Compte actif, avec droits
  │
  ├── Modification des droits (ajout / changement de rôle / retrait d'un département)
  ├── Désactivation (réversible) → compte inactif
  ├── Réactivation → compte actif
  │
  └── Suppression du compte (irréversible)
        → Toutes les appartenances aux départements sont automatiquement retirées
```

---

### 5. Règles métier

#### Création

1. L'identifiant (username) doit être unique dans le système. Toute tentative de création en doublon est refusée.
2. Le mot de passe doit faire au minimum 8 caractères.
3. À la création, le compte est actif par défaut.
4. À la création, le compte ne possède aucune appartenance à un département — les droits doivent être accordés explicitement.

#### Appartenance à un département

5. Un utilisateur ne peut avoir qu'un seul rôle par département. Si on lui en assigne un nouveau, l'ancien est remplacé (pas de doublon, pas d'erreur).
6. Changer un utilisateur de Membre à Directeur dans un département se fait en un seul geste.
7. Supprimer un compte retire automatiquement toutes ses appartenances aux départements.

#### Mot de passe

8. Le mot de passe ne circule jamais en clair — ni dans les réponses du système, ni dans les journaux d'activité.

#### Statut

9. Un compte inactif ne peut pas se connecter à G-Banque.
10. La désactivation est immédiate — l'utilisateur ne peut plus se connecter dès que le statut est basculé.

---

### 6. Droits d'accès

Ce module est **entièrement réservé à l'administrateur système**. Aucun autre profil, pas même un Directeur de département, n'y a accès.

| Action | Qui peut faire |
|--------|----------------|
| Consulter la liste des utilisateurs | Administrateur système uniquement |
| Créer un compte | Administrateur système uniquement |
| Activer / désactiver un compte | Administrateur système uniquement |
| Accorder / retirer le statut admin système | Administrateur système uniquement |
| Attribuer / modifier / retirer des droits de département | Administrateur système uniquement |
| Changer un mot de passe | Administrateur système uniquement |
| Supprimer un compte | Administrateur système uniquement |

---

### 7. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|--------|------------------------|
| **Département** | Les appartenances aux départements référencent les départements créés dans le module Département. La suppression d'un département est bloquée tant qu'il contient des utilisateurs. |
| **Authentification** | Les droits accordés ici sont intégrés dans le jeton d'accès à la connexion. Un changement de droits ne prend effet qu'à la prochaine connexion du collaborateur concerné. |

---

L'authentification et les droits définissent qui peut agir dans G-Banque ; reste à définir sur quoi ces droits s'exercent. Le compte bancaire est ce point d'ancrage central : c'est autour de lui que s'organisent tous les modules financiers qui suivent.

# Comptes bancaires

---

<a id="chap4"></a>

## Chapitre 4 — Module Comptes bancaires


> Source de vérité métier pour le module Comptes bancaires.

---

### 1. Périmètre du module

Ce module centralise la liste des comptes bancaires de l'entreprise. Chaque compte correspond à une banque ou agence avec laquelle l'entreprise travaille. Il constitue le point d'ancrage central de G-Banque : tous les autres modules (rapprochements, lignes fonctionnelles, agios, moyens de paiement) se réfèrent aux comptes définis ici.

---

### 2. Définitions métier

**Compte bancaire** : enregistrement représentant un compte courant ouvert auprès d'une banque ou agence. Dans G-Banque, il est identifié par son numéro RIB (24 chiffres) et porte les informations de référence : nom de la banque, agence, devise, adresse et logo.

**RIB (Relevé d'Identité Bancaire)** : identifiant unique et immuable d'un compte. Format strict : 24 chiffres. Ce numéro est la clé d'identification du compte dans tous les modules. Il ne peut jamais être modifié après la création du compte.

**Solde** : montant disponible sur le compte à un instant donné. Le solde est toujours calculé en temps réel à partir des transactions enregistrées — il n'est jamais stocké statiquement. Toute transaction validée modifie immédiatement le solde affiché.

**Plafond FC (Facilité de Caisse)** : montant maximum de découvert autorisé par la banque, configuré dans les lignes fonctionnelles. Affiché dans le tableau de bord Finance pour chaque compte. Toujours stocké et affiché en positif.

**Disponibilité FC** : marge disponible sous la facilité de caisse, calculée en temps réel pour le jour même — Solde actuel + Plafond FC, sans aucune projection de fin de mois. Positive : il reste de la marge sous le plafond. Négative : le plafond est dépassé. Affichée sur chaque carte du tableau de bord Finance *(nouveau, 31/08/2026)*.

**Devise** : unité monétaire du compte. Valeurs possibles : MAD, EUR, USD, GNS.

**Premier rapprochement** : à la création d'un compte, un rapprochement bancaire est automatiquement ouvert pour le mois en cours. Il n'est pas nécessaire d'initier manuellement le premier rapprochement. Son solde de début reprend le solde initial saisi à la création du compte (voir "Solde initial" ci-dessous).

**Solde initial** : montant de départ du compte, saisi obligatoirement à la création (positif ou nul). Il ne représente pas un mouvement bancaire mais le point de départ à partir duquel les transactions ultérieures font évoluer le solde. Il n'est jamais stocké sur le compte lui-même — il devient le solde de début du premier rapprochement et n'est ni modifiable ni consultable après la création.

---

### 3. Cycle de vie d'un compte bancaire

```
Création du compte
  │  (RIB unique, logo obligatoire)
  ↓
Compte actif
  │  → Rapprochements mensuels
  │  → Transactions
  │  → Lignes fonctionnelles
  │  → Agios
  │
  ↓ (condition : solde nul, aucune transaction en attente, aucune ligne active)
Suppression
```

Un compte ne peut jamais être archivé ou désactivé — il est soit actif, soit supprimé (sous conditions strictes).

---

### 4. Le tableau de bord Finance

Le tableau de bord est la vue d'accueil du département Finance. Il présente pour chaque compte bancaire :

| Information | Source |
|-------------|--------|
| Solde actuel | Calculé en temps réel depuis les transactions |
| Solde de début de période | Issu du dernier rapprochement validé |
| Plafond Facilité de Caisse | Configuré dans les lignes fonctionnelles |
| Disponibilité FC *(nouveau, 31/08/2026)* | Solde actuel + Plafond FC — situation du jour, sans projection de fin de mois |
| Nombre de LCN échus | Effets de commerce non encore présentés |
| Nombre de virements échus | Virements programmés non encore exécutés |

Ces indicateurs sont chargés simultanément pour toutes les banques en un seul appel — indépendamment du nombre de comptes.

---

### 5. Règles métier

#### Création

1. Le numéro RIB doit être unique dans le système — un RIB déjà utilisé est refusé.
2. Le RIB doit contenir exactement 24 chiffres.
3. Le logo est obligatoire à la création.
4. Le solde initial est obligatoire à la création et doit être positif ou nul — un solde initial négatif est refusé.
5. À la création, un rapprochement bancaire est automatiquement ouvert pour le mois en cours, avec le solde initial saisi comme solde de début.

#### Modification

6. Le numéro RIB est immuable après création — toute tentative de modification est bloquée.
7. Le logo peut être conservé tel quel si aucun nouveau logo n'est fourni.
8. Le solde initial n'est ni modifiable ni redemandé — il n'existe qu'au moment de la création.

#### Suppression

La suppression est irréversible et soumise à trois conditions cumulatives :

9. Aucune ligne fonctionnelle active ne doit être associée au compte.
10. Aucune transaction en attente ne doit exister sur ce compte.
11. Le solde calculé doit être exactement nul.

Si l'une de ces conditions n'est pas remplie, la suppression est bloquée avec le motif affiché.

#### Solde

12. Le solde est toujours recalculé depuis les transactions — jamais stocké. Il reflète l'état réel du compte à l'instant de la consultation.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter la liste des comptes | Tout collaborateur authentifié |
| Consulter le tableau de bord Finance | Équipe Finance (membres et directeur), Administrateur système |
| Créer / modifier un compte | Équipe Finance (membres et directeur), Administrateur système |
| Supprimer un compte | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'un compte bancaire est historisée : auteur, date et valeurs avant/après restent consultables ultérieurement. Seules les informations textuelles du compte (nom, numéro de compte, devise, adresse, agence) sont conservées dans cet historique — le logo n'y figure pas.

---

### 8. Logs

La création, la modification et la suppression d'un compte génèrent chacune un événement journalisé, utile au diagnostic. Une tentative de suppression bloquée (lignes fonctionnelles actives, transactions en attente, solde non nul) est également journalisée comme anomalie gérée, sans jamais exposer le solde ou le numéro de compte concerné.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Rapprochement** | Un rapprochement mensuel est créé automatiquement à la création du compte. Toutes les transactions s'attachent à un rapprochement. |
| **Lignes fonctionnelles** | Les lignes (Facilité de Caisse, Préfinancement, etc.) sont rattachées à un compte bancaire. La suppression d'un compte est bloquée si des lignes actives existent. |
| **Agios** | Le calcul des agios FC reconstruit le solde journalier depuis les transactions du compte. |
| **Moyens de paiement** | LCN, virements, prélèvements et autres moyens de paiement référencent le compte bancaire de débit ou de crédit. |

---

Un compte bancaire ne se limite pas à un solde courant : il porte aussi des produits financiers de moyen et long terme (crédits, autorisations de financement, contrats de crédit-bail) qui engagent l'entreprise sur plusieurs mois ou années. Les trois modules suivants couvrent ces produits rattachés au compte.

# Produits financiers rattachés au compte

---

<a id="chap5"></a>

## Chapitre 5 — Module Crédit Moyen Terme (CMT)


> Source de vérité métier pour le module Crédit Moyen Terme.

---

### 1. Périmètre du module

Le crédit à moyen terme (CMT) est un emprunt bancaire contracté par la banque auprès d'un établissement financier, remboursé par prélèvements périodiques sur une banque de la banque.

Ce module couvre l'intégralité du cycle de vie de ces crédits : de la souscription jusqu'à la clôture (main levée), en passant par le suivi périodique des échéances, le calcul des encours et la notification automatique de fin de crédit.

---

### 2. Définitions métier

**Crédit à moyen terme** : emprunt caractérisé par un montant emprunté (nominal), un taux d'intérêt, une durée totale exprimée en nombre d'échéances, une périodicité entre deux échéances (en mois), et un montant d'échéance fixe. À la création, le système génère automatiquement l'intégralité du plan de remboursement.

**Nominal** : montant total emprunté.

**Taux** : taux d'intérêt du crédit, exprimé en pourcentage.

**Périodicité** : intervalle, en mois, entre deux échéances consécutives (1 pour un rythme mensuel, 3 pour trimestriel, etc.).

**Durée** : nombre total d'échéances du crédit.

**Échéance (montant)** : montant fixe prélevé à chaque échéance, identique du début à la fin du crédit.

**Échéance (statut)** : chaque échéance individuelle du plan de remboursement passe par deux états :

| Statut | Signification |
|--------|---------------|
| PRÉVU | Échéance non encore réglée |
| PAYÉ | Prélèvement effectué, intégré dans le rapprochement bancaire |

**Encours CMT** : pour une banque donnée, somme totale des montants des échéances encore PRÉVUES jusqu'à la fin du mois en cours, tous crédits actifs confondus.

**Main levée** : clôture du crédit une fois toutes les échéances PAYÉES. Le crédit est alors archivé et reste consultable.

---

### 3. Cycle de vie complet

```
[1] Souscrire un crédit à moyen terme
    (banque de prélèvement, désignation, nominal, taux,
     périodicité, durée, montant d'échéance, date de début)
    → Génération automatique du plan de remboursement complet
        ↓
[2] Suivi périodique des échéances
    → Chaque échéance est validée lors du rapprochement bancaire → statut PAYÉ
        ↓
[3] Toutes les échéances sont PAYÉES
    → Notification automatique chaque dimanche
    → Action manuelle : Main levée → crédit clôturé (archivé)
```

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|-------|-------------|
| Liste des crédits encours | Vue principale, filtrable par désignation et banque. Export PDF disponible. |
| Création / modification d'un crédit | Formulaire — la modification régénère le plan de remboursement et est bloquée si des échéances sont déjà payées |
| Crédits en attente de main levée | Crédits dont toutes les échéances sont PAYÉES. Export PDF disponible. |
| Archives | Crédits clôturés (main levée effectuée). Export PDF disponible. |
| Tableau d'amortissement | Détail des échéances d'un crédit, avec totaux (endettement, déjà payé, restant à payer), filtrable par numéro d'échéance, mois, année, statut. Export PDF disponible. |
| Encours | Encours global de CMT par banque. Export PDF disponible (nouveau, 23/07/2026). |

---

### 5. Règles métier

1. Le plan de remboursement complet (une échéance par période) est généré automatiquement à la création du crédit, à partir de la date de début, de la périodicité et de la durée.
2. Le montant de chaque échéance est fixe et identique pour toute la durée du crédit.
3. La modification d'un crédit régénère intégralement son plan de remboursement (les anciennes échéances sont supprimées et recréées) — bloquée si au moins une échéance est déjà PAYÉE.
4. La suppression d'un crédit est bloquée si au moins une échéance est déjà PAYÉE ; dans ce cas, le crédit doit être archivé plutôt que supprimé.
5. La clôture (main levée) est bloquée si au moins une échéance est encore PRÉVUE, ou si le crédit est déjà archivé.
6. La validation d'une échéance (rapprochement) crée la transaction bancaire correspondante et passe l'échéance en PAYÉ — bloquée si l'échéance est déjà PAYÉE, ou si le rapprochement ciblé n'est ni EN_COURS ni CLOTURE_PROVISOIRE.
7. Une bascule manuelle du statut d'une échéance (PRÉVU ↔ PAYÉ), sans création de transaction bancaire, est possible pour corriger une anomalie — action sensible réservée à l'administrateur système.
8. La modification manuelle des informations d'une échéance (date d'échéance, montant, dates d'opération et de valeur, observation, libellé) n'est possible que si l'échéance est encore PRÉVUE — bloquée si elle est déjà PAYÉE. Action sensible réservée à l'administrateur système.
9. Chaque dimanche, le système détecte automatiquement les crédits dont toutes les échéances sont PAYÉES et envoie une notification pour déclencher la procédure de main levée.
10. L'encours CMT d'une banque correspond à la somme des échéances encore PRÉVUES jusqu'à la fin du mois en cours.
11. Chaque écran de liste (crédits encours, en attente de main levée, archives) et le tableau d'amortissement disposent d'un export PDF : le document contient l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement la page affichée. Le tableau d'amortissement exporté reprend également les totaux affichés à l'écran (endettement, montant déjà payé, montant restant à payer).

---

### 6. Valeurs de configuration

| Paramètre | Valeur par défaut | Description |
|-----------|--------|-------------|
| Fréquence du job main levée | **Chaque dimanche à 14h30** | Détection automatique des crédits prêts pour main levée |
| Seuil d'alerte court terme | **7 jours** | Fenêtre de la métrique de suivi "échéances à venir à court terme". Administrable par un administrateur système (écran Configuration CMT). |
| Seuil d'alerte long terme | **30 jours** | Fenêtre de la métrique de suivi "échéances à venir à long terme". Administrable par un administrateur système (écran Configuration CMT). |

Les deux seuils d'alerte ne sont pas des valeurs figées : un administrateur système peut les faire évoluer à tout moment depuis l'écran d'administration dédié, sans intervention technique. Le changement s'applique immédiatement aux métriques de suivi.

---

### 7. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|--------|:---:|:---:|:---:|
| **Crédits CMT** — créer, modifier, consulter | ✅ | ✅ | ✅ |
| **Crédits CMT** — supprimer | ✅ | ❌ | ❌ |
| **Crédits CMT** — clôturer (main levée) | ✅ | ✅ | ✅ |
| **Échéances** — consulter, valider (rapprochement) | ✅ | ✅ | ✅ |
| **Échéances** — basculer statut manuellement | ✅ | ❌ | ❌ |
| **Échéances** — modifier manuellement (si PRÉVUE) | ✅ | ❌ | ❌ |
| **Batch main levée** — déclencher manuellement | ✅ | ❌ | ❌ |

---

### 8. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables ultérieurement pour toute vérification :

- création, modification, suppression ou clôture (main levée) d'un crédit à moyen terme ;
- validation d'une échéance (rapprochement) et toute synchronisation ultérieure de ses dates depuis la transaction bancaire liée ;
- basculement manuel du statut d'une échéance.

La consultation seule (listes, tableau d'amortissement, encours) n'est pas historisée.

---

### 9. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, archivage ou suppression réussie d'un crédit, ou modification d'une échéance ;
- déclenchement, progression et fin du job hebdomadaire de détection des mains levées, avec le nombre de crédits traités ;
- envoi (réussi ou échoué) des notifications de main levée.

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées, notamment :

- modification ou suppression d'un crédit alors que des échéances sont déjà payées ;
- clôture (main levée) d'un crédit dont des échéances restent encore prévues, ou déjà archivé ;
- validation d'une échéance déjà payée, ou vers un rapprochement qui n'est pas ouvert.

---

### 10. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque crédit CMT désigne une banque de prélèvement périodique. |
| **Rapprochement** | La validation d'une échéance crée une transaction bancaire dans le rapprochement mensuel de la banque de prélèvement. |
| **Notifications** | Le job du dimanche envoie une alerte pour chaque crédit dont toutes les échéances sont PAYÉES. |

---

<a id="chap6"></a>

## Chapitre 6 — Module Lignes Fonctionnelles


> Source de vérité métier pour le module Lignes Fonctionnelles.

---

### 1. Périmètre du module

Une ligne fonctionnelle représente une autorisation de financement accordée par une banque : un
plafond et un taux, valables sur une période donnée. Le module couvre trois notions :

- le **référentiel des natures** de lignes fonctionnelles (ex. Facilité de Caisse, Caution
  Administrative, Escompte) ;
- les **lignes générales**, rattachées à une banque (pour l'ensemble de son activité, ou pour un ou
  plusieurs marchés précis) ;
- les **lignes spécifiques**, rattachées directement à un marché.

---

### 2. Définitions métier

**Nature de ligne fonctionnelle** : catégorie d'autorisation de financement (ex. Facilité de
Caisse, Caution Administrative, Escompte, Avance sur marché). Chaque nature a un code court
immuable, un libellé, une description, et une **portée** qui détermine où elle peut être utilisée :

| Portée | Utilisable dans |
|---|---|
| Générale | une ligne fonctionnelle générale uniquement |
| Spécifique | une ligne fonctionnelle spécifique uniquement |
| Générale et Spécifique | les deux types de ligne |

**Ligne fonctionnelle générale** : autorisation accordée à une banque, avec un plafond, un taux, et
une période de validité (date de départ, date de fin optionnelle). Elle peut être :
- **sans marché** : couvre l'ensemble de l'activité de la banque pour cette nature ;
- **rattachée à un ou plusieurs marchés** : limitée à ces marchés.

**Ligne mère / Sous-ligne** : une ligne fonctionnelle générale de nature "Caution Administrative"
peut être décomposée en plusieurs sous-lignes (une par type de caution), chacune avec son propre
plafond mais héritant automatiquement de la banque, du taux, des marchés et des dates de la ligne
mère. Une sous-ligne ne peut pas être gérée (modifiée, désactivée, supprimée) indépendamment de sa
ligne mère.

**Ligne fonctionnelle spécifique** : autorisation accordée pour un marché précis (jamais pour
l'ensemble d'une banque), avec son propre plafond, taux et période de validité.

**Ligne active / inactive** : une ligne (générale ou spécifique) peut être désactivée sans être
supprimée ; elle reste alors consultable mais ne compte plus pour les contrôles de doublon.

**Encours d'une ligne** : suivi cumulé des opérations rattachées à un marché (droits constatés,
factoring, préfinancement) sur la ligne fonctionnelle spécifique correspondante — débit, crédit,
reste disponible.

**Plafond FC (Facilité de Caisse)** : cas particulier de ligne générale de nature "Facilité de
Caisse" (code système `FC`), consulté par le tableau de bord des comptes bancaires pour afficher
l'encours utilisé par rapport au plafond autorisé.

---

### 3. Cycle de vie complet

```
Nature de ligne fonctionnelle (référentiel)
    ↓
[Ligne générale]                          [Ligne spécifique]
Rattachée à une banque,                   Rattachée à un marché,
sans marché ou avec un/plusieurs marchés  nature à portée Spécifique
    ↓                                          ↓
Active — utilisée pour les contrôles de doublon et les tableaux de suivi
    ↓
Surveillance automatique de l'expiration (batch hebdomadaire, générale et spécifique)
    ↓
Désactivation (conservée, non supprimable si nature encore référencée)
    ↓
Modification / historique (graphique taux et plafond dans le temps)
```

#### Surveillance automatique de l'expiration

Chaque dimanche, un traitement automatique parcourt l'ensemble des lignes actives (générales et
spécifiques) dont la date de fin est renseignée et calcule le nombre de mois restants avant
expiration :

- en-deçà du **seuil d'avertissement** (3 mois par défaut), une notification d'AVERTISSEMENT est
  générée dans l'application ("planifier le renouvellement") ;
- en-deçà du **seuil d'alerte** (1 mois par défaut), une notification d'ALERTE est générée
  ("action immédiate requise") ;
- une ligne déjà expirée (date de fin dépassée) génère également une notification.

Les deux seuils sont administrables (voir section 6).

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Natures de lignes fonctionnelles | Référentiel : liste filtrable par code/libellé/description/portée, création, modification (le code est immuable), suppression. Export PDF disponible. |
| Lignes fonctionnelles — encours | Liste des lignes générales actives (mères et indépendantes), filtrable, avec gestion des sous-lignes. Export PDF disponible. |
| Lignes fonctionnelles — archivées | Lignes générales désactivées. Export PDF disponible. |
| Lignes fonctionnelles spécifiques — encours | Liste des lignes spécifiques actives, par marché. Export PDF disponible. |
| Lignes fonctionnelles spécifiques — archivées | Lignes spécifiques désactivées. Export PDF disponible. |
| Suivi d'une ligne spécifique | Détail des opérations (droits constatés / factoring / préfinancement) rattachées au marché de la ligne, avec totaux débit/crédit/reste |
| Historique (graphique) | Évolution du taux et du plafond d'une ligne générale dans le temps, par banque et nature |

---

### 5. Règles métier

1. Le **code** d'une nature est immuable après création ; seuls le libellé, la description et la
   portée peuvent être modifiés.
2. Une nature ne peut être **supprimée** que si elle n'est référencée par aucune ligne
   fonctionnelle (générale ou spécifique), existante ou archivée.
3. Une **ligne générale** ne peut utiliser qu'une nature à portée Générale ou Générale et
   Spécifique ; une **ligne spécifique** ne peut utiliser qu'une nature à portée Spécifique ou
   Générale et Spécifique.
4. Une seule **ligne active** par combinaison (banque + nature) sans marché, ou par combinaison
   (marché + nature), qu'elle soit générale ou spécifique — un marché ne peut jamais avoir
   simultanément une ligne générale et une ligne spécifique actives pour la même nature.
5. Seule la nature "Caution Administrative" peut porter des **sous-lignes**. Une sous-ligne hérite
   automatiquement de la banque, du taux, des marchés et des dates de sa ligne mère ; seuls son
   plafond et sa nature lui sont propres.
6. Une sous-ligne ne peut pas être **modifiée, désactivée ou supprimée directement** — ces
   opérations passent obligatoirement par la ligne mère, qui les répercute en cascade.
7. La **désactivation** d'une ligne mère désactive automatiquement toutes ses sous-lignes actives.
8. La **modification** d'une ligne (banque, nature ou marchés) redéclenche le contrôle de doublon
   d'une ligne active si l'un de ces éléments change.
9. Le **plafond FC** (Facilité de Caisse) d'une banque correspond au plafond de sa ligne générale
   active de nature "Facilité de Caisse" — utilisé par le tableau de bord des comptes bancaires.
10. Les cinq écrans de liste du module (Natures, Lignes générales encours/archivées, Lignes
    spécifiques encours/archivées) disposent chacun d'un **export PDF** : le document contient
    l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas
    seulement la page affichée. Pour les lignes générales et spécifiques, le même export sert les
    écrans encours et archivées — seul le statut demandé (actif/inactif) change le contenu et le
    titre du document ; les montants d'encours et de solde affichés dans le PDF sont calculés de la
    même façon qu'à l'écran (aucun recalcul distinct).

---

### 6. Valeurs de configuration

Le code système `FC` (Facilité de Caisse) identifie une nature de référence utilisée par d'autres
modules — ce n'est pas une valeur métier modifiable par un administrateur.

Le batch hebdomadaire de suivi des expirations (générales et spécifiques) applique deux seuils
administrables :

| Paramètre | Valeur par défaut | Description |
|-----------|--------|-------------|
| Fréquence du batch | **Chaque dimanche à 12h** | Détection automatique des lignes expirées ou proches de l'expiration. |
| Seuil d'alerte | **1 mois** | Nombre de mois restants avant expiration en-deçà duquel une notification d'ALERTE (action immédiate requise) est générée. Administrable par un administrateur système (écran Configuration Lignes Fonctionnelles). |
| Seuil d'avertissement | **3 mois** | Nombre de mois restants avant expiration en-deçà duquel une notification d'AVERTISSEMENT (planifier le renouvellement) est générée. Administrable par un administrateur système (écran Configuration Lignes Fonctionnelles). |

Les deux seuils ne sont pas des valeurs figées : un administrateur système peut les faire évoluer à
tout moment depuis l'écran d'administration dédié, sans intervention technique. Le changement
s'applique à la prochaine exécution du batch.

---

### 7. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|---|:---:|:---:|:---:|
| **Natures** — consulter (référentiel, dropdowns) | ✅ | ✅ | ✅ *(tout utilisateur authentifié)* |
| **Natures** — créer, modifier | ✅ | ✅ | ✅ |
| **Natures** — supprimer | ✅ | ❌ | ❌ |
| **Natures** — exporter en PDF | ✅ | ✅ | ✅ *(tout utilisateur authentifié)* |
| **Lignes générales** — consulter, créer, modifier, désactiver (dont sous-lignes) | ✅ | ✅ | ✅ |
| **Lignes générales** — supprimer | ✅ | ❌ | ❌ |
| **Lignes générales** — exporter en PDF | ✅ | ✅ | ✅ |
| **Lignes spécifiques** — consulter, créer, modifier, désactiver | ✅ | ✅ | ✅ |
| **Lignes spécifiques** — supprimer | ✅ | ❌ | ❌ |
| **Lignes spécifiques** — exporter en PDF | ✅ | ✅ | ✅ |
| **Configuration des seuils d'expiration** (alerte/avertissement) | ✅ | ❌ | ❌ |
| **Déclenchement manuel du batch de suivi des expirations** | ✅ | ❌ | ❌ |

Détail technique (endpoints, guards Angular, visibilité sidebar) : `docs/rbac/lignes.md`.

---

### 8. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification (aucun impact sur la consultation, qui n'est jamais
historisée) :

- **Nature de ligne fonctionnelle** : création, modification et suppression.
- **Ligne fonctionnelle générale** : création, modification (y compris désactivation et gestion des
  sous-lignes) et suppression.
- **Ligne fonctionnelle spécifique** : création, modification (y compris désactivation) et
  suppression.
- **Configuration des seuils d'expiration** : toute modification des seuils d'alerte/avertissement.

---

### 9. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification ou suppression d'une nature de ligne fonctionnelle ;
- création, modification, désactivation, suppression d'une ligne générale (y compris ajout/retrait
  de sous-lignes) ;
- création, modification, désactivation, suppression d'une ligne spécifique ;
- exécution du batch hebdomadaire de suivi des expirations (déclenchement, nombre de lignes
  traitées, notifications générées).

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées,
notamment :

- label ou code de nature déjà utilisé ;
- suppression d'une nature, d'une ligne générale ou spécifique encore référencée ou bloquée par une
  règle métier ;
- doublon détecté à la création ou modification d'une ligne (même banque/marché et même nature déjà
  active) ;
- tentative de gestion directe d'une sous-ligne (modification/désactivation hors de sa ligne mère).

---

### 10. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Comptes bancaires** | Chaque ligne générale est rattachée à une banque ; le plafond FC alimente le tableau de bord Finance. |
| **Marchés** | Les lignes générales (optionnellement) et les lignes spécifiques (obligatoirement) sont rattachées à un ou plusieurs marchés. |
| **Droits constatés / Factoring / Préfinancement** | Les opérations de ces modules s'imputent sur la ligne fonctionnelle spécifique du marché concerné et alimentent son suivi (débit/crédit/reste). |
| **Cautions** | La nature "Caution Administrative" structure ses lignes générales en ligne mère / sous-lignes. |

---

<a id="chap7"></a>

## Chapitre 7 — Module Leasing


> Source de vérité métier pour le module Leasing.

---

### 1. Périmètre du module

Le crédit-bail (leasing) est un produit bancaire par lequel la banque finance l'acquisition d'un bien (véhicule, matériel industriel, équipement informatique…) pour le compte d'un client, en partenariat avec un organisme de leasing spécialisé.

Ce module couvre l'intégralité du cycle de vie de ces contrats : de la signature jusqu'à la clôture (main levée), en passant par le suivi mensuel des échéances, le calcul des encours et la notification automatique de fin de contrat.

---

### 2. Définitions métier

**Organisme de leasing** : partenaire commercial de la banque qui fournit les biens en crédit-bail. Exemples : BMCI Leasing, CIH Leasing, Wafabail. Un organisme doit être enregistré dans le système avant de pouvoir signer un contrat.

**Contrat cadre** : accord commercial en vigueur entre la banque et un organisme de leasing pour une période donnée. Il fixe :
- le **plafond** : montant maximum que la banque peut engager avec cet organisme sur la période
- la **date de début et de fin** : la période de validité de l'accord
- le **statut** : ACTIF, EXPIRÉ ou ARCHIVÉ

Un organisme ne peut avoir qu'un seul contrat cadre ACTIF à la fois. Sans contrat cadre valide, aucun contrat de leasing ne peut être signé avec cet organisme.

> Analogie : le contrat cadre est l'accord-cadre entre deux entreprises. Les contrats de leasing individuels sont les commandes passées dans ce cadre.

**Contrat de leasing** : contrat individuel lié à un bien précis. Il est toujours rattaché à un organisme via son contrat cadre actif au moment de la signature. À la création, le système génère automatiquement le tableau d'amortissement complet.

**Tableau d'amortissement (plan d'échéances)** : liste de toutes les mensualités à payer mois par mois, plus une échéance finale correspondant à la valeur résiduelle. Généré automatiquement à la création du contrat.

**Valeur résiduelle** : montant que le client paie en fin de contrat pour lever l'option d'achat du bien. Toujours égale à **1% du montant d'achat TTC**. Ce calcul est automatique — la valeur saisie par l'utilisateur est ignorée.

**Phase** : tranche de loyer dans un contrat progressif ou dégressif. Chaque phase définit une durée (en mois) et un montant de mensualité.

**Échéance** : un mois de loyer à payer (ou la valeur résiduelle finale).

| Statut | Signification |
|--------|---------------|
| PRÉVU | Échéance non encore réglée |
| PAYÉ | Prélèvement effectué, intégré dans le rapprochement bancaire |

**Encours de leasing** : pour une banque donnée, somme totale des montants des échéances encore PRÉVUES sur tous les contrats actifs.

**Main levée** : clôture du contrat une fois toutes les échéances PAYÉES. Le contrat est alors archivé et reste consultable.

---

### 3. Cycle de vie complet

```
[1] Créer l'organisme de leasing
        ↓
[2] Créer un Contrat Cadre ACTIF pour cet organisme
    (plafond + dates de validité)
        ↓
[3] Signer un Contrat de leasing
    (organisme, banque de prélèvement, bien, montant, durée, type)
    → Génération automatique du tableau d'amortissement
    → Génération automatique des commissions (si règle configurée)
        ↓
[4] Suivi mensuel des échéances
    → Chaque échéance est validée lors du rapprochement bancaire → statut PAYÉ
        ↓
[5] Toutes les échéances sont PAYÉES
    → Notification automatique chaque dimanche
    → Action manuelle : Main levée → contrat clôturé (archivé)
```

---

### 4. Types de contrats

| Type | Description | Contrainte |
|------|-------------|------------|
| LINÉAIRE | Loyer constant sur toute la durée. Une seule phase. | Une phase unique, durée = durée totale du contrat |
| PROGRESSIF | Loyers croissants par tranches (phases). | La somme des durées des phases doit être égale à la durée totale |
| DÉGRESSIF | Loyers décroissants par tranches. | La somme des durées des phases doit être égale à la durée totale |

#### Cycle de vie d'un contrat cadre

```
         Création
            ↓
          ACTIF  ──── (dateFin dépassée, détectée à la demande) ──→  EXPIRÉ
            │
            └──── (archivage manuel) ──→  ARCHIVÉ
```

- Un contrat cadre expiré est détecté au moment de la création d'un contrat leasing.
- Un contrat cadre archivé est mis hors service manuellement.
- Il n'y a pas de suppression : les contrats cadres sont conservés indéfiniment pour l'historique.

---

### 5. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|-------|-------------|
| Liste des contrats actifs | Vue principale, filtrable par banque, organisme, type, numéro |
| Création de contrat (wizard 6 étapes) | Guidage pas à pas : organisme → détails → banque → type → phases → récapitulatif |
| Détail d'un contrat | Informations du contrat + tableau d'amortissement paginé |
| Modification d'un contrat | Formulaire pré-rempli — bloqué si des échéances sont déjà PAYÉES |
| Contrats en attente de main levée | Contrats dont toutes les échéances sont PAYÉES |
| Archives | Contrats clôturés (main levée effectuée) |
| Organismes | Liste et gestion des partenaires |
| Contrats cadres | Tableau paginé des accords commerciaux par organisme, avec filtres et actions |
| Encours | Encours global de leasing par banque, filtrable par organisme et numéro. Export PDF disponible (nouveau, 23/07/2026). |

#### Le wizard de création (6 étapes)

1. **Organisme** — choisir l'organisme partenaire
2. **Détails généraux** — numéro de contrat, bien financé, montant TTC, date de début
3. **Banque** — compte bancaire de prélèvement mensuel
4. **Type** — LINÉAIRE, PROGRESSIF ou DÉGRESSIF
5. **Phases** — définir les tranches de loyers selon le type
6. **Récapitulatif** — vérification avant validation

Le wizard conserve les données saisies si on revient en arrière.

#### Export PDF

Les écrans **Organismes**, **Contrats cadres**, **Liste des contrats actifs**, **Contrats en
attente de main levée**, **Archives**, ainsi que le tableau d'amortissement d'un contrat,
disposent chacun d'un bouton **Exporter PDF**. Le document généré reprend l'intégralité des
résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement la page
affichée. L'export du tableau d'amortissement reprend également les totaux affichés à l'écran
(endettement, montant déjà payé, montant restant à payer).

---

### 6. Règles métier

#### Contrat cadre

1. Un organisme ne peut avoir qu'un seul contrat cadre ACTIF à la fois.
2. L'expiration est détectée en temps réel au moment de la création d'un contrat leasing : si la date de fin est dépassée, le contrat cadre passe automatiquement en EXPIRÉ et la création du contrat leasing est bloquée.
3. Aucun contrat cadre ne peut être supprimé — l'archivage est l'action de mise hors service.
4. L'organisme d'un contrat cadre ne peut pas être modifié après création.
5. Le plafond et les dates peuvent être modifiés à tout moment.

#### Contrat de leasing

6. La valeur résiduelle est toujours égale à 1% du montant d'achat TTC — calculée automatiquement.
7. Un contrat LINÉAIRE ne peut avoir qu'une seule phase, dont la durée doit être égale à la durée totale.
8. Pour PROGRESSIF et DÉGRESSIF, la somme des durées des phases doit être égale à la durée totale.
9. Les numéros de phase doivent être séquentiels depuis 1 et uniques par contrat.
10. Le tableau d'amortissement est généré automatiquement à la création.
11. La modification d'un contrat est bloquée si au moins une échéance est PAYÉE.
12. La suppression d'un contrat est bloquée si au moins une échéance est PAYÉE.
13. La clôture (main levée) est bloquée si au moins une échéance est encore PRÉVUE.

#### Échéances

14. La modification manuelle d'une échéance est bloquée si elle est au statut PAYÉ.
15. La validation d'une échéance (rapprochement) crée la transaction bancaire correspondante et passe l'échéance en PAYÉ.

#### Commissions

16. À la création du contrat (et lors de chaque modification régénérant les échéances), une commission est générée automatiquement par échéance, si une règle de commission est configurée pour la banque de prélèvement.
17. Si aucune règle n'existe pour la banque, aucune commission n'est créée et aucune erreur n'est déclenchée.
18. Lors de la régénération des échéances suite à une modification de contrat, les anciennes commissions sont supprimées avant qu'on en crée de nouvelles.

#### Notification main levée

19. Chaque dimanche, le système détecte automatiquement les contrats dont toutes les échéances sont PAYÉES et envoie une notification pour déclencher la procédure de main levée.

---

### 7. Valeurs de configuration

| Paramètre | Valeur par défaut | Description |
|-----------|--------|-------------|
| Valeur résiduelle | **1% du montant TTC** | Calculée automatiquement — la valeur saisie par l'utilisateur est ignorée. Taux administrable par un administrateur système (écran Configuration leasing). |
| Taux TVA | **20%** | Appliqué dans le calcul HT/TVA des échéances. Taux administrable par un administrateur système (écran Configuration leasing). |
| Fréquence du job main levée | **Chaque dimanche à 14h00** | Détection automatique des contrats prêts pour main levée |
| Seuil d'alerte court terme | **7 jours** | Fenêtre de la métrique de suivi "échéances à venir à court terme". Administrable par un administrateur système (écran Configuration leasing). |
| Seuil d'alerte long terme | **30 jours** | Fenêtre de la métrique de suivi "échéances à venir à long terme". Administrable par un administrateur système (écran Configuration leasing). |

La valeur résiduelle, le taux de TVA et les deux seuils d'alerte ne sont plus des valeurs figées : un administrateur système peut les faire évoluer à tout moment depuis l'écran d'administration dédié, sans intervention technique. Toute modification de la valeur résiduelle ou du taux de TVA s'applique aux contrats créés ou modifiés après le changement — les contrats déjà signés ne sont pas recalculés rétroactivement. Le changement des seuils d'alerte s'applique immédiatement aux métriques de suivi.

---

### 8. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|--------|:---:|:---:|:---:|
| **Organismes** — créer, modifier | ✅ | ✅ | ✅ |
| **Organismes** — supprimer | ✅ | ❌ | ❌ |
| **Contrats cadres** — consulter | ✅ | ✅ | ✅ |
| **Contrats cadres** — créer, modifier, archiver | ✅ | ✅ | ✅ |
| **Contrats leasing** — créer, modifier | ✅ | ✅ | ✅ |
| **Contrats leasing** — supprimer | ✅ | ❌ | ❌ |
| **Contrats leasing** — clôturer (main levée) | ✅ | ✅ | ✅ |
| **Échéances** — consulter, valider (rapprochement) | ✅ | ✅ | ✅ |
| **Échéances** — modifier manuellement | ✅ | ❌ | ❌ |
| **Échéances** — basculer statut manuellement | ✅ | ❌ | ❌ |
| **Batch main levée** — déclencher manuellement | ✅ | ❌ | ❌ |
| **Configuration leasing** — consulter, modifier | ✅ | ❌ | ❌ |
| **Export PDF** (organismes, contrats cadres, contrats, tableau d'amortissement) | ✅ | ✅ | ✅ |

---

### 9. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables ultérieurement pour toute vérification :

- création, modification ou suppression d'un organisme de leasing ;
- création, modification ou archivage d'un contrat cadre ;
- création, modification, suppression ou clôture (main levée) d'un contrat de leasing ;
- modification ou basculement de statut d'une échéance ;
- modification de la configuration leasing (valeur résiduelle, taux de TVA).

La consultation seule (listes, tableau d'amortissement, encours, export PDF) n'est pas historisée.

---

### 10. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, archivage ou suppression réussie d'un organisme, d'un contrat cadre, d'un contrat de leasing ou d'une échéance ;
- déclenchement, progression et fin du job hebdomadaire de détection des mains levées, avec le nombre de contrats traités ;
- envoi (réussi ou échoué) des notifications de main levée et des emails d'alerte en cas d'échec du job.

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées, notamment :

- création d'un contrat cadre alors qu'un contrat cadre ACTIF existe déjà pour l'organisme ;
- signature d'un contrat de leasing avec un contrat cadre expiré ;
- modification ou suppression d'un contrat, ou modification d'une échéance, alors que des échéances sont déjà payées ;
- suppression d'un organisme encore lié à des contrats cadres existants ;
- clôture (main levée) d'un contrat dont des échéances restent encore prévues.

---

### 11. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque contrat leasing désigne une banque de prélèvement mensuel. |
| **Commission** | À la création de chaque échéance, le système consulte les règles de commission configurées et génère automatiquement les commissions applicables. |
| **Rapprochement** | La validation d'une échéance crée une transaction bancaire dans le rapprochement mensuel de la banque de prélèvement. |
| **Notifications** | Le job du dimanche envoie une alerte pour chaque contrat dont toutes les échéances sont PAYÉES. |

---

Au-delà des produits financiers génériques, l'activité de Staport en tant qu'entreprise de BTP s'organise autour de marchés — des contrats de travaux, chacun avec ses propres financements, garanties et règlements. Le chapitre suivant introduit le marché comme notion pivot, puis les modules satellites qui s'y rattachent directement.

# Marchés et satellites

---

<a id="chap8"></a>

## Chapitre 8 — Module Marchés


> Source de vérité métier pour le module Marchés.

---

### 1. Périmètre du module

Un marché désigne un contrat conclu entre Staport (entreprise de BTP) et un maître d'ouvrage pour
la réalisation de travaux, rattaché à une banque. Ce module couvre la gestion complète des
marchés : création, modification, consultation, archivage et suppression, ainsi que la
consultation des lignes fonctionnelles (générales et spécifiques) rattachées à chaque marché.

Le marché est une notion pivot du système : il est référencé par les modules Préfinancements,
Droits constatés, Factoring, Décomptes, Cautions, ainsi que par le calcul automatique des agios et
le calcul d'encours des lignes fonctionnelles.

---

### 2. Définitions métier

**Marché** : contrat BTP identifié par un numéro unique (`numero`), rattaché à une banque, avec un
nom, un montant, un maître d'ouvrage, et deux dates optionnelles (début et achèvement des
travaux).

**Numéro de marché** : identifiant métier unique — deux marchés ne peuvent jamais porter le même
numéro, y compris après modification d'un marché existant.

**Maître d'ouvrage** : entité (cliente) pour le compte de laquelle les travaux du marché sont
réalisés.

**Dates de travaux** : la date de début et la date d'achèvement des travaux sont toutes deux
optionnelles. Si les deux sont renseignées, la date d'achèvement ne peut pas être antérieure à la
date de début.

| Statut (`actif`) | Signification |
|---|---|
| `true`  | Marché actif — pris en compte dans tous les calculs financiers (agios, encours) |
| `false` | Marché archivé — exclu définitivement de tous les calculs financiers, mais reste consultable et son historique n'est pas modifié |

**Archivage** : action manuelle et réversible (via désarchivage) qui n'a aucun effet rétroactif
sur l'historique déjà généré, mais exclut le marché de tout calcul futur dès qu'elle est
appliquée — même si le marché conserve un solde non nul. L'archivage n'est pas dérivé
automatiquement de la date d'achèvement des travaux : c'est une décision explicite de l'équipe
Finance.

---

### 3. Cycle de vie complet

```
[1] Créer le marché
    (banque, numéro, nom, montant, maître d'ouvrage, dates de travaux [optionnelles])
        ↓
[2] Suivi — le marché est pris en compte dans les calculs d'agios et d'encours des lignes
    fonctionnelles auxquelles il est rattaché
        ↓
[3a] Archivage                              [3b] Suppression définitive
    → Exclusion définitive des calculs           → Uniquement si aucune donnée financière
      futurs, historique conservé,                  n'est rattachée (décomptes, préfinancements,
      marché toujours consultable                    droits constatés, factorings, lignes
        ↓                                            spécifiques) — sinon utiliser l'archivage
    [4] Désarchivage possible à tout moment
        → Le marché redevient pris en compte
          dans les calculs futurs
```

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Liste des marchés | Vue principale, filtrable par numéro, nom, maître d'ouvrage, banque, statut (Actifs / Archivés / Tous — Actifs par défaut) et par période (date de début et date d'achèvement des travaux, indépendamment l'une de l'autre, via un panneau "Période" séparé) |
| Création / modification d'un marché | Formulaire — banque, numéro, nom, montant, maître d'ouvrage, dates de travaux |
| Lignes rattachées à un marché | Vue en deux onglets (lignes spécifiques / lignes générales) listant les lignes fonctionnelles rattachées au marché, avec accès direct au détail de suivi de chacune |

#### Actions disponibles

- **Créer** un marché.
- **Modifier** un marché (tous les champs, y compris la banque et le numéro).
- **Archiver** un marché actif : il devient invisible pour les calculs futurs mais reste
  consultable.
- **Désarchiver** un marché archivé : il redevient pris en compte dans les calculs futurs.
- **Consulter les lignes fonctionnelles** (générales et spécifiques) rattachées à un marché.
- **Supprimer** définitivement un marché — réservée à l'administrateur système, et uniquement si
  aucune donnée financière n'y est rattachée.
- **Exporter en PDF** la liste des marchés correspondant aux filtres actuellement appliqués
  (numéro, nom, maître d'ouvrage, banque, statut, période sur les dates de début/achèvement des
  travaux) — le fichier contient l'intégralité des résultats filtrés, pas seulement la page
  actuellement affichée à l'écran.

---

### 5. Règles métier

1. Le numéro de marché est unique — vérifié à la création et à la modification (un marché ne peut
   pas se voir refuser sa propre valeur inchangée lors d'une modification).
2. Le montant doit être strictement supérieur à zéro.
3. Si les deux dates de travaux sont renseignées, la date d'achèvement ne peut pas être antérieure
   à la date de début.
4. Un marché archivé (`actif = false`) est exclu définitivement des calculs d'agios et d'encours
   des lignes fonctionnelles, même s'il conserve un solde non nul — aucune exception pour les
   marchés soldés.
5. Un marché archivé est également exclu des écrans de suivi (préfinancement, droits constatés,
   factoring) consultés depuis une ligne fonctionnelle — mêmes règles que le calcul d'encours.
6. La suppression définitive d'un marché est interdite s'il a des décomptes, préfinancements,
   droits constatés, factorings ou lignes fonctionnelles spécifiques rattachés — ces données
   seraient sinon supprimées en cascade avec le marché, détruisant un historique financier réel.
   Dans ce cas, utiliser l'archivage à la place.
7. L'archivage n'a aucun effet rétroactif : les calculs déjà effectués avant l'archivage ne sont
   jamais recalculés ou modifiés.

---

### 6. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|---|:---:|:---:|:---:|
| Consulter un marché ou la liste | ✅ | ✅ | ✅ |
| Créer, modifier un marché | ✅ | ✅ | ✅ |
| Archiver, désarchiver un marché | ✅ | ✅ | ✅ |
| Consulter les lignes rattachées à un marché | ✅ | ✅ | ✅ |
| Supprimer définitivement un marché | ✅ | ❌ | ❌ |
| Exporter la liste des marchés en PDF | ✅ | ✅ | ✅ |

---

### 7. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification :

- création, modification ou suppression d'un marché ;
- archivage ou désarchivage d'un marché.

La consultation seule (liste, détail, lignes rattachées) n'est pas historisée.

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, suppression, archivage ou désarchivage réussie d'un marché.

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées,
notamment :

- création ou modification d'un marché avec un numéro déjà utilisé par un autre marché ;
- date d'achèvement des travaux antérieure à la date de début ;
- suppression définitive refusée en raison de données financières rattachées.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Comptes bancaires** | Chaque marché est rattaché à une banque. |
| **Lignes fonctionnelles (générales et spécifiques)** | Un marché peut être rattaché à une ou plusieurs lignes fonctionnelles ; son statut d'archivage détermine son inclusion dans le calcul d'encours de ces lignes. |
| **Préfinancements, Droits constatés, Factoring, Décomptes** | Chaque opération de ces modules est rattachée à un marché ; ces données bloquent la suppression définitive du marché. |
| **Cautions** | Une caution peut être rattachée à un marché (facultatif). |
| **Agios (calcul automatique)** | Les calculateurs Préfinancement, Droits constatés et Factoring parcourent les marchés actifs d'une banque pour reconstituer l'encours servant au calcul des agios. |

---

<a id="chap9"></a>

## Chapitre 9 — Module Caution


> Source de vérité métier pour le module Caution.

---

### 1. Périmètre du module

Une caution bancaire est une garantie émise par une banque au bénéfice d'un tiers (organisme),
couvrant les obligations de Staport dans le cadre d'un contrat ou d'un marché. Ce module couvre
l'intégralité du cycle de vie d'une caution : de sa prise (émission) jusqu'à sa main levée
(clôture), en passant par le calcul et la génération mensuelle automatique des commissions
bancaires dues, propres à chaque banque.

---

### 2. Définitions métier

**Caution** : garantie bancaire identifiée par un numéro unique (`numeroCaution`), émise par une
banque pour un organisme bénéficiaire, avec un montant et un taux de commission. Elle peut être
liée à un marché (facultatif) et à une nature de ligne fonctionnelle (obligatoire).

**Prise** (`dateDePrise`) : date d'émission de la caution. Ne peut pas être dans le futur.

**Main levée** (`dateDeMainLevee`) : date de clôture de la caution. Vide tant que la caution est
active — impossible à saisir à la création, elle se renseigne exclusivement via l'action
**Clôturer**. Une fois la caution clôturée, cette date reste corrigeable depuis le formulaire de
modification (voir section 4, action **Modifier**).

| Statut (`actif`) | Signification |
|---|---|
| `true`  | Caution en cours |
| `false` | Caution clôturée (main levée effectuée) |

**Commission sur caution** : montant facturé par la banque, calculé au prorata journalier selon
une formule et un calendrier propres à chaque banque (voir section 6). Une commission de clôture
est également générée à la main levée. Certaines banques génèrent en plus une TVA sur la
commission.

**Configuration de commission caution** : pour chaque banque, deux paramètres administrables
(montant minimum de commission, diviseur du prorata journalier) — voir section 7. Sans
configuration enregistrée pour une banque, aucune commission ne peut être calculée pour ses
cautions.

---

### 3. Cycle de vie complet

```
[1] Créer la caution
    (banque, nature, marché [optionnel], numéro, désignation, organisme, montant, taux, date de prise)
        ↓
    → Si la stratégie de la banque le prévoit : génération immédiate d'un prorata d'entrée
      (commission couvrant les jours du mois de prise) + TVA si la banque l'exige
        ↓
[2] Suivi mensuel — génération automatique des commissions
    (fin de mois pour AWB/BMCI, début de mois pour CDM — voir section 6)
        ↓
[3] Clôture (main levée)
    → Génération de la commission de clôture (prorata jusqu'à la date de main levée)
    → TVA si la banque l'exige
    → Caution passée à actif = false, dateDeMainLevee renseignée
```

Une caution clôturée n'est plus incluse dans la génération mensuelle des commissions. Elle reste
modifiable (voir section 4, action **Modifier**) : toute correction d'un champ qui influence le
calcul de la commission de clôture (banque, montant, taux, date de prise, date de main levée)
déclenche son recalcul automatique.

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Liste des cautions | Vue principale, filtrable par numéro, banque, nature, marché, organisme, date de prise, montant, statut |
| Création / modification d'une caution | Formulaire — la date de main levée n'apparaît qu'en modification d'une caution déjà clôturée (correction) |
| Commissions sur cautions | Liste des commissions générées (prorata d'entrée, mensuelles, clôture) — export PDF disponible |
| TVA sur cautions | Liste des TVA générées sur les commissions (banques concernées uniquement) — export PDF disponible (nouveau, 2026-07-22) |
| Export (PDF ou Excel) | Export de l'intégralité des cautions filtrées (pas seulement la page affichée) — format et colonnes du document sélectionnables à la demande (Excel : nouveau, 20/09/2026) |

#### Actions disponibles

- **Créer** une caution — sans date de main levée : ce champ n'existe pas sur le formulaire de
  création, il se renseigne uniquement via l'action **Clôturer**.
- **Modifier** une caution, active ou déjà clôturée (tous les champs sont modifiables, y compris
  la banque, la nature, le marché et le numéro) :
  - pour une caution **active**, le formulaire est identique à la création (pas de champ main
    levée) ;
  - pour une caution **déjà clôturée**, le champ date de main levée apparaît en plus, pour
    correction — il reste obligatoire. Modifier un champ qui influence le calcul de la commission
    de clôture (banque, montant, taux, date de prise, date de main levée) recalcule
    automatiquement cette commission ; l'opération est bloquée si cette commission est déjà
    réglée (payée) ou si elle est introuvable (caution clôturée avant l'introduction de cette
    fonctionnalité) — dans ces deux cas, une correction manuelle par un administrateur est
    nécessaire.
- **Clôturer** (main levée) une caution active : demande la date de main levée, génère
  automatiquement la commission de clôture.
- **Supprimer** définitivement une caution active — réservée à l'administrateur système.
- **Exporter** les cautions selon les filtres appliqués, en choisissant au préalable le format
  (PDF ou Excel) et les colonnes à inclure dans le document (numéro de caution, banque, nature,
  marché, organisme, désignation, date de prise, main levée, montant, taux, statut) — au moins une
  colonne doit être sélectionnée ; toutes sont cochées par défaut, le format PDF est présélectionné.
- **Exporter en PDF** les commissions sur cautions ou les TVA sur cautions selon les filtres
  appliqués à chacun de ces deux écrans (sélection de colonnes non disponible sur ces deux exports).
- **Générer manuellement** les commissions du mois (normalement automatique, voir section 6).

---

### 5. Règles métier

1. Le numéro de caution est unique.
2. La date de prise ne peut pas être dans le futur.
3. Le marché est optionnel — une caution peut exister sans marché associé.
4. La modification d'une caution est interdite si elle est clôturée (`actif = false`).
5. La suppression définitive est interdite si la caution est clôturée — elle doit avoir été créée
   par erreur pour être supprimable.
6. La clôture (main levée) est interdite si la caution est déjà clôturée.
7. La clôture exige qu'une stratégie de commission soit enregistrée pour la banque de la caution ;
   à défaut, l'opération est bloquée.
8. Si la caution est créée et clôturée dans le même mois calendaire, le prorata d'entrée déjà
   généré (et non encore payé) est supprimé avant de générer la commission de clôture, afin
   d'éviter une double facturation des mêmes jours. S'il est déjà payé, il n'est pas supprimé.
9. La génération mensuelle des commissions est idempotente : une caution ne reçoit jamais deux
   commissions pour la même date d'émission.

---

### 6. Calcul des commissions par banque

Le calcul est propre à chaque banque partenaire. Toutes les formules reposent sur le principe
`(montant de la caution × taux × nombre de jours) / diviseur`, avec un montant minimum appliqué si
le résultat est inférieur à ce seuil. Le nombre de jours et la date d'émission varient selon la
banque :

| Banque | Génération | Date d'émission | Mois facturé | Prorata d'entrée | Prorata de sortie | TVA générée |
|---|---|---|---|---|---|---|
| **AWB** | Fin de mois | Dernier jour du mois de référence | Mois courant | Géré par le traitement mensuel (pas à la création), jour de délivrance non facturé | Oui (jusqu'à la date de main levée) | Non |
| **BMCI** | Fin de mois | Dernier jour du mois M+1 | Mois suivant (M+1) | Oui, à la création (`dateEmission = dateDePrise`) | Non (mois entier à la clôture) | Oui (10 % du montant HT) |
| **CDM** | Début de mois | Premier jour du mois de référence | Mois courant | Oui, à la création (`dateEmission = dateDePrise`) | Non (mois entier à la clôture) | Non |
| **SAHAM BANK** | Fin de mois | Dernier jour du mois de référence | Mois suivant (M+1), sauf 1er mois | Oui, à la création (`dateEmission = dateDePrise`) | Non (mois entier à la clôture) | Non |

Cas particulier commun à toutes les banques : si la date de prise et la date de main levée tombent
dans le même mois calendaire, le nombre de jours facturés à la clôture est calculé entre les deux
dates exactes (et non sur le mois entier).

**Particularité AWB — jour de délivrance non facturé.** AWB ne facture pas le jour de délivrance
de la caution (la date de prise). Pour le premier mois d'une caution AWB, le décompte des jours
commence donc le lendemain de la date de prise et va jusqu'au dernier jour du mois inclus. Il en
va de même lorsque la date de prise et la date de main levée tombent dans le même mois : le jour
de prise n'est pas compté, le jour de main levée l'est. Les mois complets et le prorata de sortie
(du 1er du mois jusqu'à la date de main levée incluse) ne sont pas concernés. Le montant minimum
de la banque continue de s'appliquer si le résultat lui est inférieur.

Exemples pour une caution AWB de 1 000 000 MAD au taux de 1,5 % (diviseur 36 000) :

| Situation | Jours facturés | Commission |
|---|---|---|
| Prise le 19/05, premier mois | du 20/05 au 31/05 = 12 jours | 500,00 MAD |
| Prise le 10/05, main levée le 20/05 | du 11/05 au 20/05 = 10 jours | 416,67 MAD |
| Mois complet de juin | 30 jours | 1 250,00 MAD |

Cette règle s'applique aux commissions calculées à partir du 06/10/2026 ; les commissions déjà
générées auparavant ne sont pas recalculées automatiquement.

---

### 7. Valeurs de configuration

#### Seuils par banque (`/cautions/commission-config`, réservé `SYSTEM_ADMIN`)

| Banque | Montant minimum de commission | Diviseur du prorata journalier |
|---|---|---|
| AWB | 20,00 MAD | 36 000 |
| BMCI | 70,00 MAD | 36 000 |
| CDM | 70,00 MAD | 36 000 |
| SAHAM BANK | 55,00 MAD | 36 000 |

Ces valeurs sont administrables à tout moment par un administrateur système, sans intervention
technique. Elles constituent les valeurs de référence à saisir lors de la mise en service de ce
module — sans configuration enregistrée pour une banque, la génération de commission pour ses
cautions échoue avec une erreur explicite (aucune valeur par défaut n'est appliquée
silencieusement).

#### Autres paramètres

| Paramètre | Valeur | Administrable |
|---|---|---|
| Taux de TVA sur commission | 10 % | Non — taux légal statutaire, non paramétrable |
| Génération fin de mois (AWB, BMCI) | Dernier jour du mois à 20h00 (heure Casablanca) | Non — fréquence fixe |
| Génération début de mois (CDM) | 1er jour du mois à 03h00 (heure Casablanca) | Non — fréquence fixe |

---

### 8. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|---|:---:|:---:|:---:|
| Consulter, créer, modifier, clôturer une caution | ✅ | ✅ | ✅ |
| Exporter en PDF | ✅ | ✅ | ✅ |
| Générer manuellement les commissions du mois | ✅ | ✅ | ✅ |
| Supprimer définitivement une caution | ✅ | ❌ | ❌ |
| Consulter / modifier la configuration des commissions (seuils, diviseur) | ✅ | ❌ | ❌ |

---

### 9. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification :

- création, modification ou suppression d'une caution ;
- clôture (main levée) d'une caution.

La consultation seule (liste, export PDF, tableau des commissions/TVA) n'est pas historisée.

---

### 10. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, suppression ou clôture réussie d'une caution ;
- déclenchement, progression et fin des deux traitements mensuels automatiques (fin de mois et
  début de mois), avec le nombre de commissions créées et ignorées ;
- création d'une configuration de commission caution (montant minimum, diviseur) pour une banque.

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées,
notamment :

- modification, suppression ou clôture d'une caution déjà clôturée ;
- clôture d'une caution sans stratégie de commission enregistrée pour sa banque ;
- génération de commission pour une banque sans configuration enregistrée (seuil minimum,
  diviseur) ;
- commission déjà existante pour une caution à une date d'émission donnée (idempotence).

---

### 11. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Comptes bancaires** | Chaque caution est rattachée à une banque émettrice. |
| **Lignes fonctionnelles** | Chaque caution est rattachée à une nature de ligne fonctionnelle. |
| **Marchés** | Une caution peut être rattachée à un marché (facultatif). |
| **Commission** | Toute commission générée pour une caution (prorata d'entrée, mensuelle, clôture) est créée dans le module Commission. |
| **Impôts et taxes** | La TVA sur commission (BMCI) est créée dans le module Impôts et Taxes. |

---

<a id="chap10"></a>

## Chapitre 10 — Module Décomptes


> Source de vérité métier pour le module Décomptes.

---

### 1. Périmètre du module

Un décompte est un règlement de travaux facturé dans le cadre d'un marché, encaissé auprès d'une
banque désignée. Ce module couvre la saisie, le suivi et le règlement de ces décomptes, ainsi que
la consultation de leur encours (décomptes non encore réglés) pour une banque donnée.

Contrairement aux modules Préfinancement, Factoring et Droits Constatés, un décompte n'est pas
rattaché à une ligne fonctionnelle et n'intervient pas dans le calcul automatique des agios.

---

### 2. Définitions métier

**Décompte** : règlement de travaux relatif à un marché, portant un montant, une date d'émission
et un libellé obligatoire, encaissé par une banque désignée à la saisie (banque d'encaissement).

**Banque d'encaissement** : banque auprès de laquelle le décompte est réglé. Elle est choisie
librement à la saisie et peut différer de la banque associée au marché lui-même.

| Statut | Signification |
|---|---|
| Prévu | Le décompte est saisi mais pas encore réglé. |
| Payé | Le décompte a été réglé (basculement manuel). |

---

### 3. Cycle de vie complet

```
[1] Créer le décompte
    (marché, banque d'encaissement, montant, date d'émission, libellé)
        ↓
[2] Le décompte reste "Prévu" tant qu'il apparaît dans l'encours de sa banque d'encaissement
        ↓
[3] Basculement du statut vers "Payé" (action réservée à l'administrateur système)
```

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Liste des décomptes | Vue principale, filtrable par marché, banque d'encaissement, statut, plage de date d'émission, plage de date d'opération, plage de date valeur, montant exact |
| Création / modification d'un décompte | Formulaire (marché, banque d'encaissement, montant, date d'émission, libellé) |
| Encours des décomptes | Consultation, par banque, des décomptes "Prévu" dont la date d'émission est antérieure ou égale à la fin du mois en cours, avec le total correspondant — export PDF disponible (nouveau, 2026-07-22) |
| Export PDF | Export de l'intégralité des décomptes filtrés (pas seulement la page affichée) |

#### Actions disponibles

- **Créer** un décompte.
- **Modifier** un décompte existant.
- **Basculer le statut** Prévu/Payé — réservé à l'administrateur système.
- **Supprimer** définitivement un décompte — réservé à l'administrateur système, refusé si le
  décompte est déjà Payé.
- **Exporter en PDF** la liste des décomptes ou l'encours d'une banque selon les filtres appliqués
  à chacun de ces deux écrans.
- **Consulter l'encours** des décomptes non réglés d'une banque.

---

### 5. Règles métier

1. Le marché et la banque d'encaissement sont obligatoires à la création comme à la modification.
2. Le libellé est un champ de saisie libre obligatoire (contrairement aux modules Préfinancement/
   Factoring/Droits Constatés, où le libellé est généré automatiquement).
3. Un décompte est créé au statut "Prévu" par défaut.
4. L'encours d'une banque ne montre que les décomptes "Prévu" dont la date d'émission est
   antérieure ou égale à la fin du mois calendaire en cours ; le tri par défaut de la liste comme
   de l'encours place les décomptes sans date valeur en dernier.
5. Un décompte ne peut être supprimé que s'il est encore au statut "Prévu". Toute tentative de
   suppression d'un décompte déjà Payé est rejetée.

---

### 6. Sécurité

- Consultation, création, modification, export PDF : équipe Finance (membre ou directeur) ou
  administrateur système.
- Basculement du statut Prévu/Payé et suppression définitive : administrateur système uniquement.

Détail technique (endpoints, guards Angular, sidebar) : `docs/rbac/decomptes.md`.

---

### 7. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification :

- création, modification ou suppression d'un décompte.

La consultation seule (liste, export PDF, encours) n'est pas historisée. Le changement de statut
Prévu/Payé n'est pas historisé (action réversible, sans effet sur les données du décompte
lui-même).

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, suppression et changement de statut réussis d'un décompte ;
- export PDF et consultation de l'encours d'une banque.

Les tentatives portant sur un décompte, un marché ou une banque introuvable sont également
journalisées comme anomalies gérées.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Marchés** | Chaque décompte est rattaché à un marché. |
| **Comptes bancaires** | Chaque décompte est rattaché à une banque d'encaissement. |

---

<a id="chap11"></a>

## Chapitre 11 — Module Préfinancements


> Source de vérité métier pour le module Préfinancements.

---

### 1. Périmètre du module

Un préfinancement est un mouvement de trésorerie lié au financement anticipé d'un marché : soit un
**déblocage** de fonds par la banque, soit un **remboursement** de ces fonds par Staport. Ce module
couvre la saisie, le suivi et le règlement de ces mouvements, ainsi que la consultation séparée de
l'encours de déblocage et de l'encours de remboursement pour une banque donnée.

Contrairement au module Décomptes, chaque préfinancement doit être rattaché à une ligne
fonctionnelle Préfinancement active (spécifique ou générale) du marché, et intervient dans le
calcul automatique des agios de cette ligne.

---

### 2. Définitions métier

**Préfinancement** : mouvement de trésorerie (déblocage ou remboursement) relatif à un marché,
portant un montant et une date d'émission. Le libellé est généré automatiquement selon le type de
mouvement (contrairement au module Décomptes, où il est saisi librement).

**Type** :

| Type | Signification |
|---|---|
| Déblocage | La banque verse des fonds de préfinancement à Staport pour ce marché (crédit). |
| Remboursement | Staport rembourse à la banque les fonds préalablement débloqués (débit). |

**Statut** :

| Statut | Signification |
|---|---|
| Prévu | Le mouvement est saisi mais pas encore réglé. |
| Payé | Le mouvement a été réglé (basculement manuel). |

**Ligne fonctionnelle Préfinancement active** : ligne fonctionnelle (spécifique au marché en
priorité, sinon générale liée au marché) de nature "PREFINANCEMENT", à laquelle chaque mouvement de
préfinancement est rattaché. Un marché sans ligne Préfinancement active ne peut pas recevoir de
nouveau préfinancement.

---

### 3. Cycle de vie complet

```
[1] Créer le préfinancement (marché, montant, date d'émission, type)
        ↓
[2] Résolution automatique de la ligne fonctionnelle Préfinancement active du marché
    (priorité : ligne spécifique > ligne générale) — échec si aucune ligne active
        ↓
[3] Le mouvement reste "Prévu" tant qu'il apparaît dans l'encours de son type (déblocage ou
    remboursement) pour la banque du marché
        ↓
[4] Basculement du statut vers "Payé" (action réservée à l'administrateur système)
```

Si le marché est modifié sur un préfinancement existant, la ligne fonctionnelle active est
re-résolue pour le nouveau marché.

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Liste des préfinancements | Vue principale, filtrable par marché, banque, type (déblocage/remboursement), statut, plage de date d'émission, plage de date d'opération, plage de date valeur (nouveau, 2026-07-24) |
| Création / modification d'un préfinancement | Formulaire (marché, montant, date d'émission, type) |
| Encours de remboursement | Consultation, par banque, des remboursements "Prévu" dont la date d'émission est antérieure ou égale à la fin du mois en cours, avec le total correspondant |
| Encours de déblocage | Même consultation pour les déblocages "Prévu" |
| Suivi de ligne (depuis une ligne fonctionnelle) | Récapitulatif agrégé (total débit, total crédit, reste) et détail des mouvements de préfinancement d'une ligne fonctionnelle générale ou spécifique |

#### Actions disponibles

- **Créer** un préfinancement.
- **Modifier** un préfinancement existant.
- **Basculer le statut** Prévu/Payé — réservé à l'administrateur système.
- **Supprimer** définitivement un préfinancement — réservé à l'administrateur système, refusé si le
  préfinancement est déjà Payé.
- **Consulter l'encours** de remboursement ou de déblocage d'une banque.
- **Consulter le suivi** des préfinancements d'une ligne fonctionnelle.
- **Exporter en PDF** la liste des préfinancements, l'encours de remboursement, ou l'encours de
  déblocage — chaque export contient l'intégralité des résultats correspondant aux filtres
  actuellement appliqués, pas seulement la page affichée à l'écran.

---

### 5. Règles métier

1. Le marché, le montant, la date d'émission et le type sont obligatoires à la création comme à la
   modification.
2. Un préfinancement est créé au statut "Prévu" par défaut.
3. À la création (ou lors d'un changement de marché en modification), le système recherche la ligne
   fonctionnelle Préfinancement active du marché : la ligne spécifique du marché est prioritaire ;
   à défaut, la ligne générale liée au marché est utilisée. Si aucune des deux n'est active, la
   création/modification est refusée.
4. Le libellé est généré automatiquement : "Déblocage préfinancement marché [nom]" ou
   "Remboursement préfinancement marché [nom]" selon le type.
5. L'encours de remboursement et l'encours de déblocage d'une banque ne montrent que les mouvements
   "Prévu" du type correspondant, dont la date d'émission est antérieure ou égale à la fin du mois
   calendaire en cours ; ils sont calculés indépendamment l'un de l'autre.
6. Le suivi de ligne fonctionnelle agrège tous les préfinancements des marchés couverts par cette
   ligne : total débit (remboursements), total crédit (déblocages), et reste (crédit − débit).
7. Un préfinancement ne peut être supprimé que s'il est encore au statut "Prévu". Toute tentative de
   suppression d'un préfinancement déjà Payé est rejetée.

---

### 6. Sécurité

- Consultation, création, modification, consultation des encours et du suivi de ligne : équipe
  Finance (membre ou directeur) ou administrateur système.
- Basculement du statut Prévu/Payé et suppression définitive : administrateur système uniquement.
- Export PDF (liste, encours de remboursement, encours de déblocage) : même niveau d'autorisation
  que la consultation correspondante — équipe Finance (membre ou directeur) ou administrateur
  système.

Détail technique (endpoints, guards Angular, sidebar) : `docs/rbac/prefinancements.md`.

---

### 7. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification :

- création, modification ou suppression d'un préfinancement.

La consultation seule (liste, encours, suivi de ligne) n'est pas historisée. Le changement de
statut Prévu/Payé n'est pas historisé (action réversible, sans effet sur les données du mouvement
lui-même).

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, suppression et changement de statut réussis d'un préfinancement ;
- consultation de l'encours (déblocage ou remboursement) d'une banque.

Les tentatives portant sur un préfinancement, un marché ou une ligne fonctionnelle Préfinancement
introuvable (ou absente/inactive) sont journalisées comme anomalies gérées.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Marchés** | Chaque préfinancement est rattaché à un marché. |
| **Comptes bancaires** | La banque d'un préfinancement est celle du marché associé. |
| **Lignes fonctionnelles** (générales et spécifiques) | Chaque préfinancement doit être couvert par une ligne fonctionnelle Préfinancement active ; alimente le calcul des agios et l'écran de suivi de ligne. |

---

<a id="chap12"></a>

## Chapitre 12 — Module Factoring


> Source de vérité métier pour le module Factoring.

---

### 1. Périmètre du module

Un factoring est un mouvement de trésorerie lié à la cession de créances d'un marché à un
organisme de factoring : soit un **déblocage** de fonds, soit un **remboursement** de ces fonds.
Ce module couvre la saisie, le suivi et le règlement de ces mouvements, ainsi que la consultation
séparée de l'encours de déblocage et de l'encours de remboursement pour une banque donnée.

Comme le module Préfinancement, chaque factoring doit être rattaché à une ligne fonctionnelle
Factoring active (spécifique ou générale) du marché, et intervient dans le calcul automatique des
agios de cette ligne.

---

### 2. Définitions métier

**Factoring** : mouvement de trésorerie (déblocage ou remboursement) relatif à un marché, portant
un montant et une date d'émission. Le libellé est généré automatiquement selon le type de
mouvement.

**Type** :

| Type | Signification |
|---|---|
| Déblocage | L'organisme de factoring verse des fonds à Staport pour ce marché (crédit). |
| Remboursement | Staport rembourse à l'organisme de factoring les fonds préalablement débloqués (débit). |

**Statut** :

| Statut | Signification |
|---|---|
| Prévu | Le mouvement est saisi mais pas encore réglé. |
| Payé | Le mouvement a été réglé (basculement manuel). |

**Ligne fonctionnelle Factoring active** : ligne fonctionnelle (spécifique au marché en priorité,
sinon générale liée au marché) de nature "FACTORING", à laquelle chaque mouvement de factoring est
rattaché. Un marché sans ligne Factoring active ne peut pas recevoir de nouveau factoring.

---

### 3. Cycle de vie complet

```
[1] Créer le factoring (marché, montant, date d'émission, type)
        ↓
[2] Résolution automatique de la ligne fonctionnelle Factoring active du marché
    (priorité : ligne spécifique > ligne générale) — échec si aucune ligne active
        ↓
[3] Le mouvement reste "Prévu" tant qu'il apparaît dans l'encours de son type (déblocage ou
    remboursement) pour la banque du marché
        ↓
[4] Basculement du statut vers "Payé" (action réservée à l'administrateur système)
```

Si le marché est modifié sur un factoring existant, la ligne fonctionnelle active est re-résolue
pour le nouveau marché.

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Liste des factorings | Vue principale, filtrable par marché, banque, type (déblocage/remboursement), statut, plage de date d'émission, plage de date d'opération, plage de date valeur (nouveau, 2026-07-24) |
| Création / modification d'un factoring | Formulaire (marché, montant, date d'émission, type) |
| Encours de remboursement | Consultation, par banque, des remboursements "Prévu" dont la date d'émission est antérieure ou égale à la fin du mois en cours, avec le total correspondant |
| Encours de déblocage | Même consultation pour les déblocages "Prévu" |
| Suivi de ligne (depuis une ligne fonctionnelle) | Récapitulatif agrégé (total débit, total crédit, reste) et détail des mouvements de factoring d'une ligne fonctionnelle générale ou spécifique |

#### Actions disponibles

- **Créer** un factoring.
- **Modifier** un factoring existant.
- **Basculer le statut** Prévu/Payé — réservé à l'administrateur système.
- **Supprimer** définitivement un factoring — réservé à l'administrateur système, refusé si le
  factoring est déjà Payé.
- **Consulter l'encours** de remboursement ou de déblocage d'une banque.
- **Consulter le suivi** des factorings d'une ligne fonctionnelle.
- **Exporter en PDF** la liste des factorings, l'encours de remboursement, ou l'encours de
  déblocage — chaque export contient l'intégralité des résultats correspondant aux filtres
  actuellement appliqués, pas seulement la page affichée à l'écran.

---

### 5. Règles métier

1. Le marché, le montant, la date d'émission et le type sont obligatoires à la création comme à la
   modification.
2. Un factoring est créé au statut "Prévu" par défaut.
3. À la création (ou lors d'un changement de marché en modification), le système recherche la
   ligne fonctionnelle Factoring active du marché : la ligne spécifique du marché est prioritaire ;
   à défaut, la ligne générale liée au marché est utilisée. Si aucune des deux n'est active, la
   création/modification est refusée.
4. Le libellé est généré automatiquement : "Déblocage factoring marché [nom]" ou "Remboursement
   factoring marché [nom]" selon le type.
5. L'encours de remboursement et l'encours de déblocage d'une banque ne montrent que les
   mouvements "Prévu" du type correspondant, dont la date d'émission est antérieure ou égale à la
   fin du mois calendaire en cours ; ils sont calculés indépendamment l'un de l'autre.
6. Le suivi de ligne fonctionnelle agrège tous les factorings des marchés couverts par cette
   ligne : total débit (remboursements), total crédit (déblocages), et reste (crédit − débit).
7. Un factoring ne peut être supprimé que s'il est encore au statut "Prévu". Toute tentative de
   suppression d'un factoring déjà Payé est rejetée.

---

### 6. Sécurité

- Consultation, création, modification, consultation des encours et du suivi de ligne : équipe
  Finance (membre ou directeur) ou administrateur système.
- Basculement du statut Prévu/Payé et suppression définitive : administrateur système uniquement.
- Export PDF (liste, encours de remboursement, encours de déblocage) : même niveau d'autorisation
  que la consultation correspondante — équipe Finance (membre ou directeur) ou administrateur
  système.

Détail technique (endpoints, guards Angular, sidebar) : `docs/rbac/factoring.md`.

---

### 7. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification :

- création, modification ou suppression d'un factoring.

La consultation seule (liste, encours, suivi de ligne) n'est pas historisée. Le changement de
statut Prévu/Payé n'est pas historisé (action réversible, sans effet sur les données du mouvement
lui-même).

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, suppression et changement de statut réussis d'un factoring ;
- consultation de l'encours (déblocage ou remboursement) d'une banque.

Les tentatives portant sur un factoring, un marché ou une ligne fonctionnelle Factoring introuvable
(ou absente/inactive) sont journalisées comme anomalies gérées.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Marchés** | Chaque factoring est rattaché à un marché. |
| **Comptes bancaires** | La banque d'un factoring est celle du marché associé. |
| **Lignes fonctionnelles** (générales et spécifiques) | Chaque factoring doit être couvert par une ligne fonctionnelle Factoring active ; alimente le calcul des agios et l'écran de suivi de ligne. |

---

<a id="chap13"></a>

## Chapitre 13 — Module Droits Constatés


> Source de vérité métier pour le module Droits Constatés.

---

### 1. Périmètre du module

Un droit constaté est un mouvement de trésorerie lié à un marché : soit un **déblocage** de fonds,
soit un **remboursement** de ces fonds. Ce module couvre la saisie, le suivi et le règlement de ces
mouvements, ainsi que la consultation séparée de l'encours de déblocage et de l'encours de
remboursement pour une banque donnée.

Particularité par rapport aux modules Préfinancement et Factoring : la banque d'un droit constaté
n'est **pas** automatiquement celle du marché — c'est un champ propre au mouvement, pré-rempli à
l'écran depuis la banque du marché mais librement modifiable par l'utilisateur (par exemple si les
fonds transitent par un autre compte que celui du marché).

Comme les modules sœurs, chaque droit constaté doit être rattaché à une ligne fonctionnelle Droit
Constaté active du marché, et intervient dans le calcul automatique des agios de cette ligne.

---

### 2. Définitions métier

**Droit constaté** : mouvement de trésorerie (déblocage ou remboursement) relatif à un marché,
portant une banque, un montant et une date d'émission. Le libellé est généré automatiquement selon
le type de mouvement.

**Type** :

| Type | Signification |
|---|---|
| Déblocage | Le Trésor verse des fonds à Staport pour ce marché (crédit). |
| Remboursement | Staport rembourse au Trésor les fonds préalablement débloqués (débit). |

**Statut** :

| Statut | Signification |
|---|---|
| Prévu | Le mouvement est saisi mais pas encore réglé. |
| Payé | Le mouvement a été réglé (basculement manuel). |

**Banque du droit constaté** : banque sur laquelle transite le mouvement. À la création, l'écran de
saisie pré-remplit ce champ avec la banque du marché sélectionné dès que celui-ci est choisi, mais
l'utilisateur peut la remplacer par une autre banque avant validation. Une fois enregistrée, la
banque du droit constaté n'est plus liée à celle du marché : modifier la banque du marché par la
suite n'a aucun effet rétroactif sur les droits constatés déjà créés.

**Ligne fonctionnelle Droit Constaté active** : ligne fonctionnelle (spécifique au marché en
priorité, sinon générale liée au marché) de nature "DROIT_CONSTATE", à laquelle chaque mouvement de
droit constaté est rattaché. Un marché sans ligne Droit Constaté active ne peut pas recevoir de
nouveau droit constaté.

---

### 3. Cycle de vie complet

```
[1] Créer le droit constaté (marché, banque, montant, date d'émission, type)
        ↓
[2] Résolution automatique de la ligne fonctionnelle Droit Constaté active du marché
    (priorité : ligne spécifique > ligne générale) — échec si aucune ligne active
        ↓
[3] Le mouvement reste "Prévu" tant qu'il apparaît dans l'encours de son type (déblocage ou
    remboursement) pour la banque du droit constaté
        ↓
[4] Basculement du statut vers "Payé" (action réservée à l'administrateur système)
```

Si le marché est modifié sur un droit constaté existant, la ligne fonctionnelle active est
re-résolue pour le nouveau marché. La banque est systématiquement mise à jour à partir de la valeur
choisie sur le formulaire, qu'il y ait ou non changement de marché.

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Liste des droits constatés | Vue principale, filtrable par marché, banque, type (déblocage/remboursement), statut, plage de date d'émission, plage de date d'opération, plage de date valeur (nouveau, 2026-07-24) |
| Création / modification d'un droit constaté | Formulaire (marché, banque, montant, date d'émission, type) — la banque est pré-remplie depuis le marché mais reste modifiable |
| Encours de remboursement | Consultation, par banque, des remboursements "Prévu" dont la date d'émission est antérieure ou égale à la fin du mois en cours, avec le total correspondant |
| Encours de déblocage | Même consultation pour les déblocages "Prévu" |
| Suivi de ligne (depuis une ligne fonctionnelle) | Récapitulatif agrégé (total débit, total crédit, reste) et détail des mouvements de droit constaté d'une ligne fonctionnelle générale ou spécifique |

#### Actions disponibles

- **Créer** un droit constaté.
- **Modifier** un droit constaté existant.
- **Basculer le statut** Prévu/Payé — réservé à l'administrateur système.
- **Supprimer** définitivement un droit constaté — réservé à l'administrateur système, refusé si le
  droit constaté est déjà Payé.
- **Consulter l'encours** de remboursement ou de déblocage d'une banque.
- **Consulter le suivi** des droits constatés d'une ligne fonctionnelle.
- **Exporter en PDF** la liste des droits constatés, l'encours de remboursement, ou l'encours de
  déblocage — chaque export contient l'intégralité des résultats correspondant aux filtres
  actuellement appliqués, pas seulement la page affichée à l'écran.

---

### 5. Règles métier

1. Le marché, la banque, le montant, la date d'émission et le type sont obligatoires à la création
   comme à la modification.
2. Un droit constaté est créé au statut "Prévu" par défaut.
3. À la création (ou lors d'un changement de marché en modification), le système recherche la ligne
   fonctionnelle Droit Constaté active du marché : la ligne spécifique du marché est prioritaire ; à
   défaut, la ligne générale liée au marché est utilisée. Si aucune des deux n'est active, la
   création/modification est refusée.
4. La banque du droit constaté est celle choisie explicitement par l'utilisateur sur le formulaire
   (l'écran la pré-remplit avec la banque du marché dès sa sélection, mais ne l'impose pas) ; elle
   est indépendante de la banque du marché en base et n'est jamais recalculée automatiquement après
   création.
5. Le libellé est généré automatiquement : "Déblocage droit constaté marché [nom]" ou
   "Remboursement droit constaté marché [nom]" selon le type.
6. L'encours de remboursement et l'encours de déblocage d'une banque ne montrent que les mouvements
   "Prévu" du type correspondant, rattachés à cette banque (la banque propre du droit constaté, pas
   celle du marché), dont la date d'émission est antérieure ou égale à la fin du mois calendaire en
   cours ; ils sont calculés indépendamment l'un de l'autre.
7. Le suivi de ligne fonctionnelle agrège tous les droits constatés des marchés couverts par cette
   ligne : total débit (remboursements), total crédit (déblocages), et reste (crédit − débit).
8. Un droit constaté ne peut être supprimé que s'il est encore au statut "Prévu". Toute tentative de
   suppression d'un droit constaté déjà Payé est rejetée.

---

### 6. Sécurité

- Consultation, création, modification, consultation des encours et du suivi de ligne : équipe
  Finance (membre ou directeur) ou administrateur système.
- Basculement du statut Prévu/Payé et suppression définitive : administrateur système uniquement.
- Export PDF (liste, encours de remboursement, encours de déblocage) : même niveau d'autorisation
  que la consultation correspondante — équipe Finance (membre ou directeur) ou administrateur
  système.

Détail technique (endpoints, guards Angular, sidebar) : `docs/rbac/droits-constate.md`.

---

### 7. Audit

Les actions suivantes sont historisées avec l'auteur et la date, et restent consultables
ultérieurement pour toute vérification :

- création, modification ou suppression d'un droit constaté.

La consultation seule (liste, encours, suivi de ligne) n'est pas historisée. Le changement de
statut Prévu/Payé n'est pas historisé (action réversible, sans effet sur les données du mouvement
lui-même).

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- création, modification, suppression et changement de statut réussis d'un droit constaté ;
- consultation de l'encours (déblocage ou remboursement) d'une banque.

Les tentatives portant sur un droit constaté, un marché, une banque ou une ligne fonctionnelle
Droit Constaté introuvable (ou absente/inactive) sont journalisées comme anomalies gérées.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Marchés** | Chaque droit constaté est rattaché à un marché. |
| **Comptes bancaires** | Le droit constaté référence une banque propre, distincte de celle du marché (pré-remplie à la saisie, modifiable). |
| **Lignes fonctionnelles** (générales et spécifiques) | Chaque droit constaté doit être couvert par une ligne fonctionnelle Droit Constaté active ; alimente le calcul des agios et l'écran de suivi de ligne. |

---

Les marchés et les produits financiers rattachés aux comptes génèrent des flux financiers à régler concrètement — encaissements et décaissements. Les moyens de paiement sont les instruments par lesquels ces flux transitent effectivement sur les comptes bancaires.

# Moyens de paiement

---

<a id="chap14"></a>

## Chapitre 14 — Module Chèques


> Source de vérité métier pour le module Chèques.

---

### 1. Périmètre du module

Ce module gère les chèques bancaires de l'entreprise, dans les deux sens :

- **Encaissement** : chèques reçus par Staport (saisis manuellement).
- **Décaissement** : chèques émis par Staport (alimentés par la synchronisation avec le système de paiement externe FunPaiement — non saisis manuellement).

---

### 2. Définitions métier

**Chèque** : moyen de paiement papier, associé à une banque, un montant, une date d'émission et, optionnellement, une date d'échéance.

**Encaissement** : chèque reçu par Staport d'un tiers (l'émetteur). Le bénéficiaire est toujours Staport.

**Décaissement** : chèque émis par Staport au profit d'un tiers (le bénéficiaire).

**Statut** : état du chèque, l'un des trois suivants :
- **ENCOURS** : chèque non encore réglé — reçu du système de paiement externe ou saisi manuellement.
- **REGLE** : chèque validé/réglé côté G-Banque.
- **ANNULE** : chèque annulé côté système de paiement externe (uniquement possible si le chèque était encore ENCOURS ; un chèque déjà REGLE ne peut plus être annulé par cette voie).

**Encours** : ensemble des chèques ENCOURS d'une banque donnée, dont la date d'émission ne dépasse pas la fin du mois en cours.

---

### 3. Cycle de vie d'un chèque

```
Création
  │  (manuelle pour un encaissement, ou reçue de FunPaiement pour un décaissement)
  ↓
ENCOURS
  │
  ├──→ Validation (rapprochement) ──→ REGLE
  │
  └──→ Annulation côté FunPaiement ──→ ANNULE
```

Un chèque REGLE ne peut plus être modifié, supprimé, ni annulé. Un chèque ANNULE ne peut plus changer de statut.

---

### 4. Règles métier

#### Création (encaissement uniquement)

1. La saisie manuelle d'un chèque ne concerne que les encaissements — un chèque de décaissement provient exclusivement de la synchronisation FunPaiement.
2. Le bénéficiaire d'un chèque d'encaissement est toujours Staport.
3. La banque associée est obligatoire.
4. Le statut initial d'un chèque créé manuellement est toujours ENCOURS.
5. Pour un chèque de décaissement issu de la synchronisation FunPaiement, le libellé et l'observation reçoivent automatiquement la même valeur (référence courte du type et du numéro, ex. « CHQ N°123 »).

#### Modification

6. Seul un chèque d'encaissement peut être modifié — un chèque de décaissement (issu de FunPaiement) ne l'est pas.
7. Un chèque déjà REGLE ne peut plus être modifié.

#### Suppression

8. Un chèque déjà REGLE ne peut pas être supprimé.
9. Un chèque issu de la synchronisation FunPaiement ne peut pas être supprimé, quel que soit son statut. La synchronisation relit l'intégralité de l'historique à chaque exécution : une ligne supprimée est systématiquement recréée à l'identique lors de la synchronisation suivante. Pour retirer un tel chèque des encours, il faut l'annuler — une ligne déjà présente n'est jamais recréée ni écrasée par la synchronisation. Seuls les chèques saisis manuellement peuvent être supprimés, et leur suppression est définitive.

#### Changement de statut manuel

10. Le statut d'un chèque peut être basculé manuellement entre ENCOURS et REGLE.
11. Un chèque ANNULE ne peut plus changer de statut par cette action.

#### Validation (rapprochement)

12. Valider un chèque (encaissement ou décaissement) crée une transaction bancaire rattachée à un rapprochement, calcule la TVA associée, puis marque le chèque REGLE.
13. Un chèque qui n'est pas ENCOURS ne peut pas être validé.

---

### 5. Encours

Les écrans d'encours (décaissement et encaissement) affichent, pour une banque donnée, l'ensemble des chèques ENCOURS dont la date d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé de ces chèques.

Les quatre écrans du module (liste des encaissements, liste des décaissements, encours d'encaissement, encours de décaissement) disposent chacun d'un **export PDF** (nouveau, 22/07/2026) : le document contient l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement la page affichée.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier un chèque | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un chèque | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'un chèque est historisée : auteur, date et valeurs avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression et le changement de statut d'un chèque génèrent chacun un événement journalisé, utile au diagnostic. Une tentative de modification ou de suppression bloquée (chèque déjà réglé, ou changement de statut sur un chèque annulé) est également journalisée comme anomalie gérée.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque chèque est rattaché à un compte bancaire. |
| **Rapprochement** | La validation d'un chèque crée une transaction rattachée à un rapprochement. |
| **Synchronisation FunPaiement** | Alimente automatiquement les chèques de décaissement et leur passage à l'état ANNULE. |

---

<a id="chap15"></a>

## Chapitre 15 — Module LCN


> Source de vérité métier pour le module LCN (lettres de change normalisées).

---

### 1. Périmètre du module

Ce module gère les LCN (lettres de change normalisées) de l'entreprise, dans les deux sens :

- **Encaissement** : LCN reçus par Staport (saisis manuellement), de nature ENCAISSEMENT ou ESCOMPTE.
- **Décaissement** : LCN émis par Staport (alimentés par la synchronisation avec le système de paiement externe FunPaiement — non saisis manuellement).

---

### 2. Définitions métier

**LCN** : lettre de change normalisée, moyen de paiement associé à une banque, un montant, une date d'émission et une date d'échéance.

**Encaissement** : LCN reçu par Staport d'un tiers (l'émetteur). Le bénéficiaire est toujours Staport.

**Décaissement** : LCN émis par Staport au profit d'un tiers (le bénéficiaire).

**Nature** (uniquement pour les LCN d'encaissement) : précise le motif de l'encaissement, l'une des deux valeurs suivantes :
- **ENCAISSEMENT** : encaissement standard.
- **ESCOMPTE** : le LCN est escompté auprès de la banque avant son échéance. Cette nature n'est autorisée que pour des LCN de type ENCAISSEMENT et déclenche une association automatique à une ligne fonctionnelle d'escompte commercial (voir section 6).

**Statut** : état du LCN, l'un des trois suivants :
- **ENCOURS** : LCN non encore réglé — reçu du système de paiement externe ou saisi manuellement.
- **REGLE** : LCN validé/réglé côté G-Banque.
- **ANNULE** : LCN annulé côté système de paiement externe (uniquement possible si le LCN était encore ENCOURS ; un LCN déjà REGLE ne peut plus être annulé par cette voie).

**Encours** : ensemble des LCN ENCOURS d'une banque donnée, dont la date de référence (échéance pour un encaissement standard ou un décaissement, émission pour un escompte) ne dépasse pas la fin du mois en cours.

---

### 3. Cycle de vie d'un LCN

```
Création
  │  (manuelle pour un encaissement, ou reçue de FunPaiement pour un décaissement)
  ↓
ENCOURS
  │
  ├──→ Validation (rapprochement) ──→ REGLE
  │
  └──→ Annulation côté FunPaiement ──→ ANNULE
```

Un LCN REGLE ne peut plus être modifié, supprimé, ni annulé. Un LCN ANNULE ne peut plus changer de statut.

---

### 4. Règles métier

#### Création (encaissement uniquement)

1. La saisie manuelle d'un LCN ne concerne que les encaissements — un LCN de décaissement provient exclusivement de la synchronisation FunPaiement.
2. Le bénéficiaire d'un LCN d'encaissement est toujours Staport.
3. La banque associée est obligatoire.
4. La nature (ENCAISSEMENT ou ESCOMPTE) est obligatoire.
5. Un LCN de nature ESCOMPTE doit obligatoirement être de type ENCAISSEMENT — toute autre combinaison est rejetée.
6. Le statut initial d'un LCN créé manuellement est toujours ENCOURS.
7. Pour un LCN de décaissement issu de la synchronisation FunPaiement, le libellé et l'observation reçoivent automatiquement la même valeur (référence courte du type et du numéro, ex. « LCN N° 123 »).

#### Modification

8. Seul un LCN d'encaissement peut être modifié — un LCN de décaissement (issu de FunPaiement) ne l'est pas.
9. Un LCN déjà REGLE ne peut plus être modifié.
10. Changer la nature d'un LCN vers ESCOMPTE associe automatiquement la ligne fonctionnelle d'escompte commercial active de la banque du LCN. Changer la nature depuis ESCOMPTE vers autre chose dissocie cette ligne.

#### Suppression

11. Un LCN déjà REGLE ne peut pas être supprimé.
12. Un LCN issu de la synchronisation FunPaiement ne peut pas être supprimé, quel que soit son statut. La synchronisation relit l'intégralité de l'historique à chaque exécution : une ligne supprimée est systématiquement recréée à l'identique lors de la synchronisation suivante. Pour retirer un tel LCN des encours, il faut l'annuler — une ligne déjà présente n'est jamais recréée ni écrasée par la synchronisation. Seuls les LCN saisis manuellement peuvent être supprimés, et leur suppression est définitive.

#### Changement de statut manuel

13. Le statut d'un LCN peut être basculé manuellement entre ENCOURS et REGLE.
14. Un LCN ANNULE ne peut plus changer de statut par cette action.

#### Validation (rapprochement)

15. Valider un LCN (encaissement ou décaissement) crée une transaction bancaire rattachée à un rapprochement, calcule la TVA associée, puis marque le LCN REGLE.
16. Un LCN qui n'est pas ENCOURS ne peut pas être validé.
17. Le sens comptable de la transaction créée dépend à la fois du type et, pour un encaissement, de la nature : décaissement, encaissement standard ou encaissement escompte sont distingués dans le suivi comptable.

---

### 5. Encours

Les écrans d'encours (décaissement et encaissement) affichent, pour une banque donnée, l'ensemble des LCN ENCOURS dont la date de référence ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé de ces LCN. Pour l'encours d'encaissement, la date de référence utilisée diffère selon la nature du LCN : date d'échéance pour un encaissement standard, date d'émission pour un escompte.

Les quatre écrans du module (liste des encaissements, liste des décaissements, encours d'encaissement, encours de décaissement) disposent chacun d'un **export PDF** (nouveau, 22/07/2026) : le document contient l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement la page affichée.

---

### 6. Suivi par ligne fonctionnelle (ESCOMPTE)

Chaque ligne fonctionnelle d'escompte commercial dispose d'un écran de suivi dédié listant l'ensemble des LCN de nature ESCOMPTE qui lui sont associés, avec le montant total cumulé de ces LCN. Cet écran est accessible depuis la fiche de la ligne fonctionnelle concernée.

---

### 7. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier un LCN | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un LCN | Administrateur système uniquement |

---

### 8. Audit

Toute création, modification ou suppression d'un LCN est historisée : auteur, date et valeurs avant/après restent consultables ultérieurement.

---

### 9. Logs

La création, la modification, la suppression et le changement de statut d'un LCN génèrent chacun un événement journalisé, utile au diagnostic. Une tentative de modification ou de suppression bloquée (LCN déjà réglé, ou changement de statut sur un LCN annulé), ainsi qu'une violation de la règle ESCOMPTE ⇒ ENCAISSEMENT, sont également journalisées comme anomalies gérées.

---

### 10. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque LCN est rattaché à un compte bancaire. |
| **Lignes fonctionnelles** | Un LCN de nature ESCOMPTE est associé à la ligne fonctionnelle d'escompte commercial active de sa banque. |
| **Rapprochement** | La validation d'un LCN crée une transaction rattachée à un rapprochement. |
| **Synchronisation FunPaiement** | Alimente automatiquement les LCN de décaissement et leur passage à l'état ANNULE. |

---

<a id="chap16"></a>

## Chapitre 16 — Module Virements


> Source de vérité métier pour le module Virements (virements nationaux).

---

### 1. Périmètre du module

Ce module gère les virements bancaires nationaux de l'entreprise, dans les deux sens :

- **Encaissement** : virements reçus par Staport (saisis manuellement).
- **Décaissement** : virements émis par Staport (alimentés par la synchronisation avec le système de paiement externe FunPaiement — non saisis manuellement).

Les virements internationaux relèvent d'un module distinct (voir `docs/modules/virement-international.md`).

---

### 2. Définitions métier

**Virement** : moyen de paiement dématérialisé, associé à un montant et une date d'émission, et impliquant deux banques potentielles : une banque émettrice et une banque bénéficiaire.

**Encaissement** : virement reçu par Staport d'un tiers (l'émetteur). Le bénéficiaire est toujours Staport. La banque concernée est la banque bénéficiaire (compte Staport crédité).

**Décaissement** : virement émis par Staport au profit d'un tiers (le bénéficiaire). La banque concernée est la banque émettrice (compte Staport débité).

**Banque bénéficiaire** : peut être renseignée soit par une référence à une banque connue de G-Banque, soit par un nom libre (`nomBanqueBeneficiaireLibre`) lorsque la banque du tiers n'est pas répertoriée. À l'affichage, le nom de la banque référencée prévaut ; à défaut, le nom libre est affiché.

**Statut** : état du virement, l'un des trois suivants :
- **ENCOURS** : virement non encore réglé — reçu du système de paiement externe ou saisi manuellement.
- **REGLE** : virement validé/réglé côté G-Banque.
- **ANNULE** : virement annulé côté système de paiement externe (uniquement possible si le virement était encore ENCOURS ; un virement déjà REGLE ne peut plus être annulé par cette voie).

**Encours** : ensemble des virements ENCOURS d'une banque donnée, dont la date d'émission ne dépasse pas la fin du mois en cours.

---

### 3. Cycle de vie d'un virement

```
Création
  │  (manuelle pour un encaissement, ou reçue de FunPaiement pour un décaissement)
  ↓
ENCOURS
  │
  ├──→ Validation (rapprochement) ──→ REGLE
  │
  └──→ Annulation côté FunPaiement ──→ ANNULE
```

Un virement REGLE ne peut plus être modifié, supprimé, ni annulé. Un virement ANNULE ne peut plus changer de statut.

---

### 4. Règles métier

#### Création (encaissement uniquement)

1. La saisie manuelle d'un virement ne concerne que les encaissements — un virement de décaissement provient exclusivement de la synchronisation FunPaiement.
2. Le bénéficiaire d'un virement d'encaissement est toujours Staport.
3. La banque bénéficiaire (banque d'encaissement) est obligatoire.
4. Le statut initial d'un virement créé manuellement est toujours ENCOURS.
5. Pour un virement de décaissement issu de la synchronisation FunPaiement, le libellé et l'observation reçoivent automatiquement la même valeur (référence courte du type et du numéro, ex. « VIREMENT N° 010009 »).

#### Modification

6. Seul un virement d'encaissement peut être modifié — un virement de décaissement (issu de FunPaiement) ne l'est pas.
7. Un virement déjà REGLE ne peut plus être modifié.

#### Suppression

8. Un virement déjà REGLE ne peut pas être supprimé.
9. Un virement issu de la synchronisation FunPaiement ne peut pas être supprimé, quel que soit son statut. La synchronisation relit l'intégralité de l'historique à chaque exécution : une ligne supprimée est systématiquement recréée à l'identique lors de la synchronisation suivante. Pour retirer un tel virement des encours, il faut l'annuler — une ligne déjà présente n'est jamais recréée ni écrasée par la synchronisation. Seuls les virements saisis manuellement peuvent être supprimés, et leur suppression est définitive.

#### Changement de statut manuel

10. Le statut d'un virement peut être basculé manuellement entre ENCOURS et REGLE.
11. Un virement ANNULE ne peut plus changer de statut par cette action.

#### Validation (rapprochement)

12. Valider un virement (encaissement ou décaissement) crée une transaction bancaire rattachée à un rapprochement, calcule la TVA associée, puis marque le virement REGLE.
13. Un virement qui n'est pas ENCOURS ne peut pas être validé.
14. La banque retenue pour la transaction dépend du sens du virement : banque émettrice pour un décaissement, banque bénéficiaire pour un encaissement. Si cette banque n'est pas renseignée sur le virement, le recalcul de solde en cascade n'est simplement pas déclenché pour cette validation (la transaction est tout de même créée).

---

### 5. Encours

Les écrans d'encours (décaissement et encaissement) affichent, pour une banque donnée, l'ensemble des virements ENCOURS dont la date d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé de ces virements.

Les quatre écrans du module (liste des encaissements, liste des décaissements, encours d'encaissement, encours de décaissement) disposent chacun d'un **export PDF** (nouveau, 22/07/2026) : le document contient l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement la page affichée.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier un virement | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un virement | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'un virement est historisée : auteur, date et valeurs avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression et le changement de statut d'un virement génèrent chacun un événement journalisé, utile au diagnostic. Une tentative de modification ou de suppression bloquée (virement déjà réglé, ou changement de statut sur un virement annulé) est également journalisée comme anomalie gérée.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque virement est rattaché à une banque émettrice et/ou une banque bénéficiaire. |
| **Rapprochement** | La validation d'un virement crée une transaction rattachée à un rapprochement. |
| **Synchronisation FunPaiement** | Alimente automatiquement les virements de décaissement et leur passage à l'état ANNULE. |

---

<a id="chap17"></a>

## Chapitre 17 — Module Virements Internationaux


> Source de vérité métier pour le module Virements Internationaux.

---

### 1. Périmètre du module

Ce module gère les virements bancaires internationaux de l'entreprise, dans les deux sens :

- **Encaissement** : virements internationaux reçus par Staport.
- **Décaissement** : virements internationaux émis par Staport.

**Particularité par rapport aux autres moyens de paiement** : contrairement aux Chèques, LCN et
Virements nationaux, ce module n'est alimenté par **aucune synchronisation automatique** avec le
système de paiement externe FunPaiement. Encaissements et décaissements sont **tous deux saisis
et modifiés manuellement**, via le même formulaire, le sens (encaissement/décaissement) étant
choisi librement à la saisie.

Les virements nationaux relèvent d'un module distinct (voir `docs/modules/virement.md`).

---

### 2. Définitions métier

**Virement international** : moyen de paiement dématérialisé impliquant une contrepartie à
l'étranger, associé à une banque, un montant et une date d'émission.

**Encaissement** : virement international reçu par Staport d'un tiers (l'émetteur).

**Décaissement** : virement international émis par Staport au profit d'un tiers (le bénéficiaire).

**Statut** : état du virement international, l'un des trois suivants :
- **ENCOURS** : virement international saisi, non encore réglé.
- **REGLE** : virement international validé/réglé côté G-Banque.
- **ANNULE** : statut atteignable uniquement par bascule manuelle depuis ENCOURS (aucune source
  externe n'annule automatiquement un virement international, à la différence des autres moyens
  de paiement synchronisés).

**Encours** : ensemble des virements internationaux ENCOURS d'une banque donnée, dont la date
d'émission ne dépasse pas la fin du mois en cours.

---

### 3. Cycle de vie d'un virement international

```
Création (saisie manuelle, encaissement ou décaissement)
  ↓
ENCOURS
  │
  ├──→ Validation (rapprochement) ──→ REGLE
  │
  └──→ Bascule manuelle de statut ──→ ANNULE / ENCOURS
```

Un virement international REGLE ne peut plus être modifié ni supprimé.

---

### 4. Règles métier

#### Création

1. La banque, le bénéficiaire, l'émetteur, le montant, la date d'émission et le type
   (ENCAISSEMENT ou DECAISSEMENT) sont obligatoires.
2. Le statut initial d'un virement international créé est toujours ENCOURS.
3. Le libellé et l'observation sont générés automatiquement à partir du type, de l'émetteur ou du
   bénéficiaire, et du montant.

#### Modification

4. Un virement international déjà REGLE ne peut plus être modifié.
5. Contrairement aux autres moyens de paiement, la modification est possible aussi bien pour un
   encaissement que pour un décaissement (les deux sont saisis manuellement).

#### Suppression

6. Un virement international déjà REGLE ne peut pas être supprimé.

#### Changement de statut manuel

7. Le statut d'un virement international peut être basculé manuellement entre ENCOURS et REGLE.

#### Validation (rapprochement)

8. Valider un virement international (encaissement ou décaissement) crée une transaction bancaire
   rattachée à un rapprochement, calcule la TVA associée, puis marque le virement international
   REGLE.
9. Un virement international qui n'est pas ENCOURS ne peut pas être validé.

---

### 5. Encours

Les écrans d'encours (décaissement et encaissement) affichent, pour une banque donnée, l'ensemble
des virements internationaux ENCOURS dont la date d'émission ne dépasse pas la fin du mois en
cours, ainsi que le montant total cumulé.

La liste combinée (encaissements et décaissements confondus) ainsi que les deux écrans d'encours
disposent chacun d'un **export PDF** (nouveau, 22/07/2026) : le document contient l'intégralité des
résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement la page
affichée.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier un virement international | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un virement international | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'un virement international est historisée : auteur,
date et valeurs avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression et le changement de statut d'un virement
international génèrent chacun un événement journalisé, utile au diagnostic. Une tentative de
modification ou de suppression bloquée (virement international déjà réglé) est également
journalisée comme anomalie gérée.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque virement international est rattaché à une banque. |
| **Rapprochement** | La validation d'un virement international crée une transaction rattachée à un rapprochement. |

Ce module n'a, à la différence des autres moyens de paiement, aucune dépendance avec la
synchronisation FunPaiement.

---

<a id="chap18"></a>

## Chapitre 18 — Module Mises à Disposition


> Source de vérité métier pour le module Mises à Disposition.

---

### 1. Périmètre du module

Ce module gère les mises à disposition de fonds de l'entreprise, dans les deux sens :

- **Encaissement** : mises à disposition reçues par Staport (saisies manuellement).
- **Décaissement** : mises à disposition émises par Staport (alimentées par la synchronisation avec le système de paiement externe FunPaiement — non saisies manuellement).

---

### 2. Définitions métier

**Mise à disposition** : moyen de paiement associé à une banque, un montant, une date d'émission, un bénéficiaire et sa CIN (carte d'identité nationale).

**Encaissement** : mise à disposition reçue par Staport d'un tiers (l'émetteur). Le bénéficiaire est toujours Staport.

**Décaissement** : mise à disposition émise par Staport au profit d'un tiers (le bénéficiaire), dont la CIN est également enregistrée.

**Statut** : état de la mise à disposition, l'un des trois suivants :
- **ENCOURS** : mise à disposition non encore réglée — reçue du système de paiement externe ou saisie manuellement.
- **REGLE** : mise à disposition validée/réglée côté G-Banque.
- **ANNULE** : mise à disposition annulée côté système de paiement externe (uniquement possible si elle était encore ENCOURS ; une mise à disposition déjà REGLE ne peut plus être annulée par cette voie).

**Encours** : ensemble des mises à disposition ENCOURS d'une banque donnée, dont la date d'émission ne dépasse pas la fin du mois en cours.

---

### 3. Cycle de vie d'une mise à disposition

```
Création
  │  (manuelle pour un encaissement, ou reçue de FunPaiement pour un décaissement)
  ↓
ENCOURS
  │
  ├──→ Validation (rapprochement) ──→ REGLE
  │
  └──→ Annulation côté FunPaiement ──→ ANNULE
```

Une mise à disposition REGLE ne peut plus être modifiée, supprimée, ni annulée. Une mise à disposition ANNULE ne peut plus changer de statut.

---

### 4. Règles métier

#### Création (encaissement uniquement)

1. La saisie manuelle d'une mise à disposition ne concerne que les encaissements — une mise à disposition de décaissement provient exclusivement de la synchronisation FunPaiement.
2. Le bénéficiaire d'une mise à disposition d'encaissement est toujours Staport.
3. La banque associée est obligatoire.
4. Le statut initial d'une mise à disposition créée manuellement est toujours ENCOURS.
5. Pour une mise à disposition de décaissement issue de la synchronisation FunPaiement, le libellé et l'observation reçoivent automatiquement la même valeur (référence courte du type et du numéro, ex. « MAD 123 »).

#### Modification

6. Seule une mise à disposition d'encaissement peut être modifiée — une mise à disposition de décaissement (issue de FunPaiement) ne l'est pas.
7. Une mise à disposition déjà REGLE ne peut plus être modifiée.

#### Suppression

8. Une mise à disposition déjà REGLE ne peut pas être supprimée.
9. Une mise à disposition issue de la synchronisation FunPaiement ne peut pas être supprimée, quel que soit son statut. La synchronisation relit l'intégralité de l'historique à chaque exécution : une ligne supprimée est systématiquement recréée à l'identique lors de la synchronisation suivante. Pour retirer une telle mise à disposition des encours, il faut l'annuler — une ligne déjà présente n'est jamais recréée ni écrasée par la synchronisation. Seules les mises à disposition saisies manuellement peuvent être supprimées, et leur suppression est définitive.

#### Changement de statut manuel

10. Le statut d'une mise à disposition peut être basculé manuellement entre ENCOURS et REGLE.
11. Une mise à disposition ANNULE ne peut plus changer de statut par cette action.

#### Validation (rapprochement)

12. Valider une mise à disposition (encaissement ou décaissement) crée une transaction bancaire rattachée à un rapprochement, calcule la TVA associée, puis marque la mise à disposition REGLE.
13. Une mise à disposition qui n'est pas ENCOURS ne peut pas être validée.

---

### 5. Encours

Les écrans d'encours (décaissement et encaissement) affichent, pour une banque donnée, l'ensemble des mises à disposition ENCOURS dont la date d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé.

Les écrans de liste et d'encours, aussi bien pour les **décaissements** (22/07/2026) que pour les
**encaissements** (23/07/2026), disposent chacun d'un **export PDF** : le document contient
l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas seulement
la page affichée.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier une mise à disposition | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours des décaissements) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer une mise à disposition | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'une mise à disposition est historisée : auteur, date et valeurs avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression et le changement de statut d'une mise à disposition génèrent chacun un événement journalisé, utile au diagnostic. Une tentative de modification ou de suppression bloquée (mise à disposition déjà réglée, ou changement de statut sur une mise à disposition annulée) est également journalisée comme anomalie gérée.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque mise à disposition est rattachée à un compte bancaire. |
| **Rapprochement** | La validation d'une mise à disposition crée une transaction rattachée à un rapprochement. |
| **Synchronisation FunPaiement** | Alimente automatiquement les mises à disposition de décaissement et leur passage à l'état ANNULE. |

---

<a id="chap19"></a>

## Chapitre 19 — Module Recap Encaissement / Recap Décaissement


> Source de vérité métier pour le Recap Encaissement / Recap Décaissement des moyens de paiement.

---

### 1. Périmètre du module

Ce module offre une vue consolidée, sur deux écrans, de tous les encaissements d'une part et de tous les décaissements d'autre part, tous moyens de paiement confondus : Chèques, LCN, Virements nationaux, Virements internationaux et Mises à disposition.

Les Prélèvements et les Cautions ne font pas partie de ce périmètre — ce sont des sections distinctes dans le menu et ils ne sont pas consolidés ici.

---

### 2. Définitions métier

**Recap Encaissement** : liste unique regroupant tous les encaissements de tous les moyens de paiement ci-dessus, quel que soit leur statut (En cours, Réglé ou Annulé) et quelle que soit leur banque.

**Recap Décaissement** : liste unique regroupant tous les décaissements de tous les moyens de paiement ci-dessus, dans les mêmes conditions.

Contrairement au Récapitulatif du module États des encours (qui ne montre que les opérations encore en cours, prévues sur le mois courant, banque par banque), ce module montre l'historique complet, toutes banques confondues, avec la banque comme simple filtre.

---

### 3. Origine de la banque affichée pour un Virement

Un Virement national porte deux banques possibles (une banque émettrice et une banque bénéficiaire). Pour rester cohérent avec les écrans d'encours déjà existants pour les virements :

- Sur le Recap Décaissement, la banque affichée est la banque émettrice (celle qui paie).
- Sur le Recap Encaissement, la banque affichée est la banque bénéficiaire (celle qui reçoit).

Les autres moyens de paiement (Chèques, LCN, Virements internationaux, Mises à disposition) ne portent qu'une seule banque : aucune ambiguïté ne se pose pour eux.

---

### 4. Règles métier

1. Le module est en lecture seule : aucune création, modification ou suppression n'est possible depuis ces deux écrans. Pour agir sur une opération, l'utilisateur doit se rendre sur l'écran dédié à son moyen de paiement d'origine.
2. Chaque ligne affiche, selon l'écran : pour Recap Encaissement, l'émetteur (celui qui a payé Staport) ; pour Recap Décaissement, le bénéficiaire (celui que Staport a payé ou doit payer).
3. La date d'échéance n'est affichée que pour les Chèques et les LCN — les autres moyens de paiement n'en portent pas et affichent un tiret.
4. Le total affiché en haut de l'écran correspond à la somme des montants filtrés (pas seulement de la page affichée).

---

### 5. Filtres, tri et export

Chaque écran propose les filtres suivants, tous combinables : banque, moyen de paiement (un ou plusieurs), statut (un ou plusieurs), numéro (contient), libellé (contient), montant (plage min/max), date d'opération (plage) et date de valeur (plage).

Le tri par défaut se fait sur la date de valeur, du plus récent au plus ancien ; toute colonne affichée peut être utilisée pour trier.

Chaque écran peut être exporté en PDF avec les mêmes filtres appliqués (export non paginé — toutes les lignes filtrées y figurent).

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter le Recap Encaissement / Recap Décaissement | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF | Équipe Finance (membres et directeur), Administrateur système |

---

### 7. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Chèques, LCN, Virements, Virements internationaux, Mises à disposition** | Chacun de ces 5 modules alimente le Recap Encaissement / Recap Décaissement ; toute opération créée, modifiée ou dont le statut change dans l'un de ces modules apparaît (ou disparaît) immédiatement dans le recap correspondant. |
| **Comptes bancaires** | Le filtre banque du recap s'appuie sur les comptes bancaires existants. |

---

<a id="chap20"></a>

## Chapitre 20 — Module Prélèvements


> Source de vérité métier pour le module Prélèvements.

---

### 1. Périmètre du module

Ce module gère les prélèvements bancaires de Staport — des débits initiés par un tiers (ex.
prélèvement d'assurance, d'abonnement, cotisation CNSS) sur un compte de Staport. Un prélèvement peut
être saisi manuellement par l'équipe Finance, ou généré automatiquement chaque mois pour les charges
récurrentes connues à l'avance (voir section 4, "Génération automatique récurrente"). Contrairement
aux autres moyens de paiement (Chèques, LCN, Virements, Mises à disposition), il n'existe en revanche
aucune synchronisation avec le système de paiement externe FunPaiement pour ce module — la génération
automatique décrite ci-dessus est un mécanisme interne à G-Banque (charges fixes), distinct de cette
synchronisation externe.

---

### 2. Définitions métier

**Prélèvement** : opération associée à une banque, un libellé, une observation, un montant et une
date d'émission.

**Statut** : état du prélèvement, l'un des deux suivants :
- **PREVU** : prélèvement enregistré, pas encore réglé.
- **PAYE** : prélèvement validé/réglé côté G-Banque.

Contrairement aux autres moyens de paiement, il n'existe pas de statut d'annulation (ANNULE) — un
prélèvement ne peut être qu'en attente (PREVU) ou réglé (PAYE).

**Charge fixe** : prélèvement récurrent, propre à une banque, dont le libellé et le montant sont
connus à l'avance (ex. cotisation CNSS) et qui est généré automatiquement chaque mois sans saisie
manuelle (voir section 4, "Génération automatique récurrente").

**Encours** : ensemble des prélèvements PREVU dont la date d'émission ne dépasse pas la fin du mois
en cours.

---

### 3. Cycle de vie d'un prélèvement

```
Création (manuelle ou automatique)
  ↓
PREVU
  │
  └──→ Validation (rapprochement) ──→ PAYE
```

---

### 4. Règles métier

#### Création manuelle

1. La banque associée est obligatoire.
2. Le statut initial d'un prélèvement créé (manuel ou automatique) est toujours PREVU.

#### Génération automatique récurrente (charges fixes)

3. Certains prélèvements récurrents sont générés automatiquement, le **dernier jour de chaque mois à
   07h00**, pour certaines banques (ex. cotisation CNSS). Cette génération est idempotente : un même
   prélèvement (même banque, même libellé, même fenêtre de période — mois ou trimestre selon le cas)
   n'est jamais généré deux fois dans la même période.
4. Le mécanisme de génération automatique est commun aux modules Prélèvements, Frais bancaires,
   Impôts et Taxes et Agios — chacun avec ses propres charges configurées par banque.
5. Ces charges (banque, libellé, montant, jour d'émission, etc.) sont administrables via l'écran
   Administration → Charges fixes bancaires (voir `docs/modules/frais-bancaires.md`, section 5, et
   `docs/rbac/frais-bancaires.md`) — aucune n'est codée en dur dans l'application.

#### Modification et suppression

4. Un prélèvement déjà PAYE ne peut plus être modifié ni supprimé — comportement aligné (11/07/2026)
   sur les Chèques/LCN/Virements/Mises à disposition, qui bloquent tous la modification/suppression
   d'une opération déjà réglée. Tenter de modifier ou supprimer un prélèvement PAYE renvoie une
   erreur explicite ("Modification interdite" / "Suppression interdite : le prélèvement id ... est
   déjà payé.").

#### Changement de statut manuel

5. Le statut d'un prélèvement peut être basculé manuellement entre PREVU et PAYE.

#### Validation (rapprochement)

6. Valider un prélèvement crée une transaction bancaire rattachée à un rapprochement, calcule la
   TVA associée, puis marque le prélèvement PAYE.
7. Un prélèvement déjà PAYE ne peut pas être validé une seconde fois.

---

### 5. Encours

L'écran d'encours affiche, pour une banque donnée, l'ensemble des prélèvements PREVU dont la date
d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé.

L'écran de liste et l'écran d'encours disposent chacun d'un **export PDF** (nouveau, 22/07/2026) :
le document contient l'intégralité des résultats correspondant aux filtres actuellement appliqués à
l'écran, pas seulement la page affichée.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier un prélèvement | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un prélèvement | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'un prélèvement est historisée : auteur, date et
valeurs avant/après restent consultables ultérieurement. La génération automatique récurrente des
prélèvements (charges fixes) est également historisée au même titre qu'une création.

---

### 8. Logs

La création, la modification, la suppression et le changement de statut d'un prélèvement génèrent
chacun un événement journalisé, utile au diagnostic. La génération automatique récurrente produit
également un événement journalisé récapitulatif.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque prélèvement est rattaché à un compte bancaire. |
| **Rapprochement** | La validation d'un prélèvement crée une transaction rattachée à un rapprochement. |
| **Frais bancaires, Impôts et Taxes, Agios** | Partagent le même mécanisme de génération automatique récurrente (charges fixes). |

Aucune dépendance avec la synchronisation FunPaiement — ce module en est totalement absent (voir
section 1). Ne pas confondre cette absence de synchronisation externe avec la génération automatique
interne (charges fixes) décrite en section 4 : ce sont deux mécanismes distincts.

---

Qu'ils proviennent d'une échéance de crédit, d'un moyen de paiement ou de tout autre module, tous ces mouvements validés convergent vers une même notion de bas niveau : la transaction bancaire élémentaire, qui centralise et historise l'ensemble des mouvements produits ailleurs dans G-Banque.

# Transactions

---

<a id="chap21"></a>

## Chapitre 21 — Module Transactions


> Source de vérité métier pour le module Transactions.

---

### 1. Périmètre du module

Une transaction représente un mouvement bancaire élémentaire (un débit ou un crédit) rattaché à un
rapprochement bancaire. Toute opération validée dans un autre module (échéance de leasing ou de
crédit moyen terme, chèque, LCN, virement, mise à disposition, prélèvement, commission, agios,
caution, impôt et taxe, préfinancement, factoring, droit constaté, décompte, frais bancaire,
restitution, opération erronée, impayée) produit exactement une transaction. Ce module n'est donc
pas un point de saisie — il centralise, historise et permet la correction ponctuelle des
mouvements déjà produits par les autres modules.

---

### 2. Définitions métier

**Transaction** : mouvement bancaire élémentaire, rattaché à un rapprochement et, sauf exception, à
une banque. Porte un débit ou un crédit (l'un des deux est à zéro), une date d'émission, une date
d'opération, une date de valeur, un libellé, une observation, une TVA déductible et une TVA
collectée.

**Source** : module d'origine de la transaction (leasing, CMT, chèque, LCN décaissement/encaissement/
escompte, virement, virement international, mise à disposition, préfinancement, droit constaté,
décompte, commission, agios, frais bancaire, factoring, prélèvement, caution, impôt et taxe,
restitution, opération erronée, impayée).

**Référent** : identifiant de l'opération source ayant produit la transaction (par exemple l'ID de
l'échéance de leasing) — absent pour toute transaction qui n'est pas synchronisable avec son
origine.

**Statut du rapprochement** : hérité du rapprochement auquel la transaction appartient — en cours,
clôture provisoire ou clôture définitive. Une transaction rattachée à un rapprochement clôturé
définitivement n'est plus modifiable.

---

### 3. Cycle de vie complet

```
[1] Validation d'une opération dans son module source
    (échéance, moyen de paiement, agios, commission, etc.)
        ↓
    → Création de la transaction, rattachée au rapprochement en cours de la banque concernée
        ↓
[2] Consultation
    → Historique global (tous rapprochements, toutes banques)
    → Vue par banque, agrégée par rapprochement
        ↓
[3] Correction ponctuelle (facultative)
    → Modification de la date de valeur, de la date d'opération ou des montants de TVA
    → Répercutée automatiquement sur l'opération source correspondante
        ↓
[4] Le rapprochement passe en clôture provisoire puis définitive
    → Après clôture définitive, la transaction n'est plus modifiable
```

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Historique des transactions | Vue globale, filtrable par libellé, banque, source, montant, statut du rapprochement |
| Détail d'un rapprochement | Liste des transactions du rapprochement, avec les mêmes filtres |

#### Actions disponibles

- **Consulter** l'historique complet, avec filtres et pagination.
- **Consulter** le détail d'une transaction.
- **Exporter en PDF** l'intégralité de l'historique correspondant aux filtres actifs (mêmes
  critères que la consultation, sans pagination) : liste complète des transactions avec leurs
  dates, montants, TVA, source, statut de rapprochement et libellé, ainsi que les totaux débit et
  crédit de la sélection.
- **Modifier** la date de valeur, la date d'opération, la TVA déductible et la TVA collectée d'une
  transaction — la répercussion vers l'opération source est automatique et transparente pour
  l'utilisateur.
- **Annuler** une transaction erronée (action disponible depuis les écrans du module
  Rapprochement) : supprime la transaction et remet l'opération source à son état initial (non
  traitée). Permet à l'équipe Finance de corriger elle-même une erreur de saisie ou un écart
  constaté après réception du relevé définitif de la banque, sans passer par un administrateur
  système.

Aucune création directe d'une transaction n'est possible depuis ce module — elle est toujours
produite par la validation d'une opération dans son module source.

---

### 5. Règles métier

1. Une transaction est toujours rattachée à un rapprochement.
2. Le débit et le crédit ne sont jamais négatifs ; l'un des deux est nécessairement à zéro.
3. La modification d'une transaction est interdite si son rapprochement est en clôture définitive.
4. Toute modification de la date de valeur ou de la date d'opération d'une transaction est
   répercutée automatiquement sur l'opération source dont elle provient (échéance, moyen de
   paiement, etc.), afin que les deux restent toujours cohérentes.
5. Une transaction sans opération source synchronisable (pas de référent) peut être modifiée sans
   répercussion — seule la transaction elle-même est mise à jour.
6. L'annulation (suppression) d'une transaction est interdite si son rapprochement est en clôture
   définitive — c'est le statut du rapprochement, et non le rôle de l'utilisateur, qui protège une
   période définitivement arrêtée. Tant que le rapprochement est en cours ou en clôture
   provisoire, l'équipe Finance peut annuler elle-même une transaction erronée, sans intervention
   d'un administrateur système.
7. Le libellé et l'observation sont **obligatoires** sur toute transaction, quelle que soit sa
   source — l'objectif est de pouvoir identifier et tracer une transaction dans l'historique global
   sans avoir à rouvrir son module d'origine. Leur contenu dépend de la source :

   | Source | Libellé | Observation |
   |---|---|---|
   | Leasing | Organisme + numéro de contrat (ex. "CIH, contrat N° 123", "CIH, V.R. pour le contrat N° 123" pour la valeur résiduelle) | "Prélèvement leasing - échéance n°X/Y - contrat N° 123" |
   | Crédit Moyen Terme (CMT) | Banque + désignation du crédit | "Prélèvement CMT - échéance n°X/Y - crédit "désignation"" |
   | Chèque / Virement / LCN | Sens (Décaissement/Encaissement) + bénéficiaire + émetteur (ex. "Décaissement - Bénéficiaire: X / Émetteur: N/A") | Type + numéro de la pièce (ex. "CHQ N°123", " LCN N° 456", " VIREMENT N° 789") |
   | Mise à disposition | Bénéficiaire + émetteur | " MAD 123" |
   | Virement international | "Encaissement de {émetteur} - Montant: X" ou "Décaissement vers {bénéficiaire} - Montant: X" | Identique au libellé |
   | Préfinancement / Factoring / Droits constatés | Marché (nom) + type d'opération (déblocage/remboursement, etc.) | Numéro du marché |
   | Décompte | Libellé saisi manuellement (obligatoire) | "Marche: {numéro}" |
   | Commission / Frais bancaire / Impôts et Taxes / Prélèvement / Agios / Impayée / Restitution | Libellé saisi manuellement (obligatoire) | Observation saisie manuellement (obligatoire) |
   | Opération erronée | Libellé saisi manuellement (obligatoire) | Observation saisie manuellement (obligatoire) |

   Cette règle s'applique aussi bien à la validation unitaire qu'à la validation en lot : lorsque le
   lot ne fournit pas de libellé/observation propre à chaque opération, la valeur appliquée à la
   transaction est celle déjà portée par l'opération source (renseignée dès sa création), jamais une
   valeur vide.

---

### 6. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|---|:---:|:---:|:---:|
| Consulter l'historique et le détail d'une transaction | ✅ | ✅ | ✅ |
| Modifier une transaction (dates, TVA) | ✅ | ✅ | ✅ |

---

### 7. Audit

Toute modification d'une transaction (date de valeur, date d'opération, TVA déductible, TVA
collectée) est historisée avec l'auteur et la date, et reste consultable ultérieurement. La
consultation seule (historique, détail) n'est pas historisée.

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- récupération de l'historique et des transactions agrégées d'une banque ;
- modification réussie d'une transaction, ainsi que la répercussion vers son opération source.

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées,
notamment la modification d'une transaction dont le rapprochement est déjà clôturé définitivement.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Rapprochement** | Chaque transaction appartient à un rapprochement ; les agrégats (débit, crédit, TVA) d'un rapprochement sont calculés à partir de ses transactions. |
| **Comptes bancaires** | Chaque transaction est rattachée à une banque. |
| **Tous les modules produisant des opérations bancaires** (Leasing, CMT, moyens de paiement, Agios, Commission, Caution, Impôts et Taxes, Préfinancement, Factoring, Droits Constatés, Décomptes, Frais bancaires, Restitution, Opérations erronées, Impayées) | Chacun produit ses propres transactions lors de la validation d'une opération, et reçoit en retour la synchronisation lors d'une correction. |

---

Outre les mouvements liés à l'activité commerciale et aux produits financiers, les banques facturent directement à Staport des charges propres à la relation bancaire — frais, agios, impôts et taxes, commissions. Ces charges suivent le même circuit de validation et produisent elles aussi des transactions.

# Charges appliquées

---

<a id="chap22"></a>

## Chapitre 22 — Module Frais bancaires


> Source de vérité métier pour le module Frais bancaires.

---

### 1. Périmètre du module

Ce module gère les frais bancaires de Staport : commissions, abonnements et autres charges fixes ou
ponctuelles facturées par les banques. Un frais bancaire est rattaché à une banque, avec un montant,
un libellé, une observation et une date d'émission. Il peut être saisi manuellement par l'équipe
Finance, ou généré automatiquement chaque mois pour les charges récurrentes connues (voir section 5).

---

### 2. Définitions métier

**Frais bancaire** : opération associée à une banque, un libellé, une observation, un montant et une
date d'émission.

**Statut** : état du frais bancaire, l'un des deux suivants :
- **PREVU** : frais enregistré, pas encore réglé.
- **PAYE** : frais réglé (validé) côté G-Banque.

**Charge fixe** : frais bancaire récurrent, propre à une banque, dont le libellé et le montant sont
connus à l'avance (abonnement, commission de tenue de compte, etc.) et qui est généré automatiquement
chaque mois sans saisie manuelle.

**Encours** : ensemble des frais bancaires PREVU d'une banque donnée, dont la date d'émission ne
dépasse pas la fin du mois en cours.

---

### 3. Cycle de vie d'un frais bancaire

```
Création (manuelle ou automatique)
  ↓
PREVU
  │
  └──→ Validation (rapprochement) ──→ PAYE
```

---

### 4. Règles métier

#### Création

1. La banque associée, le montant (strictement positif), la date d'émission, le libellé et
   l'observation sont saisis à la création manuelle.
2. Le statut initial d'un frais bancaire créé (manuel ou automatique) est toujours PREVU.

#### Modification

3. Un frais bancaire peut être modifié tant qu'il n'a pas été supprimé : banque, montant, libellé,
   observation et date d'émission sont modifiables via l'écran de modification.

#### Changement de statut manuel

4. Le statut d'un frais bancaire peut être basculé manuellement entre PREVU et PAYE.

#### Suppression

5. Un frais bancaire ne peut être supprimé que s'il est encore au statut **PREVU**. Toute tentative
   de suppression d'un frais bancaire déjà PAYE est rejetée.

#### Validation (rapprochement)

6. Valider un frais bancaire crée une transaction bancaire rattachée à un rapprochement, puis marque
   le frais bancaire PAYE. La validation n'est possible que vers un rapprochement encore ouvert (en
   cours ou en clôture provisoire).

---

### 5. Génération automatique mensuelle

Un traitement automatique génère chaque mois, pour chaque banque concernée, les charges fixes
bancaires connues à l'avance (abonnements, commissions de tenue de compte, etc.), au statut PREVU,
sans intervention de l'équipe Finance. Ce traitement s'exécute le **dernier jour de chaque mois, à
07h00**.

Règles de génération :

- Chaque charge fixe est définie une seule fois (banque, libellé, montant, jour d'émission dans le
  mois) et régénérée automatiquement chaque mois par le traitement.
- Le traitement est **idempotent** : si une charge portant le même libellé a déjà été générée pour la
  même banque dans la période courante, elle n'est pas dupliquée.
- Certains libellés intègrent automatiquement la période (mois ou trimestre) lors de la génération
  (ex. « IAM 06/2026 »).
- Ce même traitement automatique génère également, selon la charge, des impôts et taxes, des agios ou
  des prélèvements — ces types ne relèvent pas de ce module (voir `docs/modules/impots-taxes.md`,
  `docs/modules/agios.md`, module Prélèvements).
- Depuis le 16/07/2026, la liste de ces charges (banque, libellé, montant, jour d'émission, ajout de
  la période, etc.) n'est plus codée en dur dans l'application : elle est administrable via l'écran
  Administration → Charges fixes bancaires, réservé à l'administrateur système (voir
  `docs/guide-utilisateur/admin.md`, section "Configuration Charges fixes bancaires", et
  `docs/rbac/frais-bancaires.md`). Les charges historiques y ont été reprises telles quelles au moment
  de la migration.

---

### 6. Encours

L'écran d'encours affiche, pour une banque donnée, l'ensemble des frais bancaires PREVU dont la date
d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé.

L'écran de liste principal dispose d'un **export PDF** (nouveau, 22/07/2026) : le document contient
l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran (banque,
libellé, statut), pas seulement la page affichée, avec le montant total des frais exportés. L'écran
d'encours dispose du même export (nouveau, 23/07/2026), limité aux frais PREVU du mois en cours.

---

### 7. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier un frais bancaire | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF la liste des frais bancaires | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un frais bancaire | Administrateur système uniquement |

---

### 8. Audit

Toute création, modification ou suppression d'un frais bancaire est historisée : auteur, date et
valeurs avant/après restent consultables ultérieurement. La génération automatique mensuelle des
charges fixes est également historisée au même titre qu'une création.

---

### 9. Logs

La création, la modification, la suppression, le changement de statut et la validation
(rapprochement) d'un frais bancaire génèrent chacun un événement journalisé, utile au diagnostic. La
génération automatique mensuelle produit également un événement journalisé récapitulatif.

---

### 10. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque frais bancaire est rattaché à un compte bancaire. |
| **Rapprochement** | La validation d'un frais bancaire crée une transaction rattachée à un rapprochement. |
| **Impôts et Taxes / Agios / Prélèvements** | Partagent le même traitement automatique mensuel de génération des charges fixes, chacun pour son propre type de charge. |

---

<a id="chap23"></a>

## Chapitre 23 — Module Agios


> Source de vérité métier pour le module Agios.

---

### 1. Périmètre du module

Le module Agios couvre deux périmètres distincts :

**Agios ponctuels** : charges d'agios facturées par la banque et saisies dans le système (pénalités, commissions, etc.). Elles sont suivies jusqu'à leur paiement lors du rapprochement bancaire mensuel.

**Calcul automatique** : moteur qui tourne chaque nuit et calcule les agios pour les types configurés (Facilité de Caisse, Préfinancement, Droits Constatés, Factoring). Les résultats sont agrégés par période — trimestre pour les trois premiers types, mois pour le Factoring (voir §2). À la clôture, un agios définitif est créé et entre dans le circuit normal de validation.

---

### 2. Définitions métier

**Agios** : charge financière facturée par une banque sur un compte courant. Dans G-Banque, un agios est rattaché à une banque, daté, porteur d'un montant et d'un libellé, et suit un cycle de vie à deux statuts.

**Facilité de Caisse (FC)** : autorisation de découvert accordée par la banque. L'agios FC est calculé sur le montant des jours où le solde du compte est négatif (découvert).

**Découvert** : situation où le solde du compte courant d'une banque est négatif. La valeur retenue pour le calcul est la valeur absolue de ce solde négatif.

**Préfinancement (PREFI)** : financement bancaire accordé pour un marché spécifique. L'agios PREFI est calculé sur l'encours reconstitué de chaque marché.

**Encours de préfinancement** : montant encore dû à la banque pour un marché à une date donnée. Il est reconstitué comme suit :

```
Encours au jour J = total des déblocages payés jusqu'au jour J
                  − total des remboursements payés jusqu'au jour J
```

Seules les opérations effectivement payées (statut PAYE) sont prises en compte. Les opérations prévues n'ont pas d'effet sur l'encours.

**Factoring** : cession de créances à la banque pour un marché spécifique. L'agios Factoring est calculé sur l'encours reconstitué de chaque marché, selon la même logique que le Préfinancement et les Droits Constatés.

**Encours de factoring** : montant encore dû à la banque pour un marché à une date donnée. Il est reconstitué comme suit :

```
Encours au jour J = total des déblocages payés jusqu'au jour J
                  − total des remboursements payés jusqu'au jour J
```

Seules les opérations effectivement payées (statut PAYE) sont prises en compte.

**Période de calcul** : unité de gestion du calcul automatique. Sa nature dépend du type d'agios :

- **Trimestrielle** — Facilité de Caisse, Préfinancement, Droits Constatés : identifiée par un numéro de trimestre (1 à 4), une année, et la banque concernée.
- **Mensuelle** — Factoring : identifiée par un numéro de mois (1 à 12), une année, et la banque concernée. Le Factoring est le seul type dont l'agrégat est mensuel plutôt que trimestriel.

| Trimestre | Période |
|-----------|---------|
| T1 | 1er janvier — 31 mars |
| T2 | 1er avril — 30 juin |
| T3 | 1er juillet — 30 septembre |
| T4 | 1er octobre — 31 décembre |

**Date valeur** : date à laquelle une opération financière prend effet comptablement. C'est cette date (et non la date de saisie) qui est utilisée pour reconstituer les soldes et encours.

**Ligne fonctionnelle** : paramétrage négocié avec la banque qui définit le type de financement, le plafond autorisé, le taux, et les dates de validité. Une ligne peut être générale (applicable à la banque entière) ou spécifique (applicable à un marché précis).

**Statut d'un agios** :

| Statut | Signification |
|--------|---------------|
| À payer (PREVU) | Agios saisi ou calculé, en attente de paiement et de rapprochement |
| Payé (PAYE) | Agios validé — transaction bancaire créée, intégré dans le rapprochement |

---

### 3. Types d'agios gérés

#### Facilité de Caisse

- Calcul par banque.
- Déclenché pour chaque jour où le solde reconstitué du compte est négatif.
- Le solde est reconstitué depuis les rapprochements et transactions de la banque, en appliquant chaque transaction à sa date valeur.
- Un seul résultat par jour et par banque.

#### Préfinancement

- Calcul par marché, pour chaque banque.
- Déclenché pour chaque jour où l'encours d'un marché est positif.
- Chaque marché avec une ligne préfinancement active donne lieu à un résultat distinct par jour.
- Un marché sans ligne préfinancement active est ignoré silencieusement.

#### Droits Constatés

- Calcul par marché, pour chaque banque.
- Déclenché pour chaque jour où l'encours de droits constatés d'un marché est positif.
- L'encours est reconstitué depuis les opérations de déblocage et remboursement de droits constatés au statut Payé.
- Chaque marché avec une ligne droits constatés active (spécifique ou générale) donne lieu à un résultat distinct par jour.
- Un marché sans ligne droits constatés active est ignoré silencieusement.

#### Factoring

- Calcul par marché, pour chaque banque.
- Déclenché pour chaque jour où l'encours de factoring d'un marché est positif.
- L'encours est reconstitué depuis les opérations de déblocage et remboursement de factoring au statut Payé.
- Chaque marché avec une ligne factoring active (spécifique ou générale) donne lieu à un résultat distinct par jour.
- Un marché sans ligne factoring active est ignoré silencieusement.
- Contrairement aux trois autres types, l'agrégat de résultat est **mensuel** et non trimestriel (voir §2).

---

### 4. Formules de calcul

#### Facilité de Caisse

Applicable uniquement si le solde du jour est négatif (découvert).

```
Découvert du jour     = valeur absolue du solde journalier négatif

Agios HT du jour      = (Découvert × Taux FC / 100) / 360

TVA du jour           = Agios HT du jour × 10 / 100

Agios TTC du jour     = Agios HT du jour + TVA du jour
```

Si le solde du jour est positif ou nul : aucun agios calculé pour ce jour.

#### Préfinancement

Applicable uniquement si l'encours du marché au jour J est positif.

```
Encours au jour J     = Σ(déblocages PAYE, date valeur ≤ J)
                      − Σ(remboursements PAYE, date valeur ≤ J)

Agios HT du jour      = (Encours × Taux PREFI / 100) / 360

TVA du jour           = Agios HT du jour × 10 / 100

Agios TTC du jour     = Agios HT du jour + TVA du jour
```

Si l'encours du jour est nul ou négatif : aucun agios calculé pour ce marché ce jour-là.

#### Droits Constatés

Applicable uniquement si l'encours de droits constatés du marché au jour J est positif.

```
Encours au jour J     = Σ(déblocages DC PAYE, date valeur ≤ J)
                      − Σ(remboursements DC PAYE, date valeur ≤ J)

Agios HT du jour      = (Encours × Taux DC / 100) / 360

TVA du jour           = Agios HT du jour × 10 / 100

Agios TTC du jour     = Agios HT du jour + TVA du jour
```

Si l'encours du jour est nul ou négatif : aucun agios calculé pour ce marché ce jour-là.

#### Factoring

Applicable uniquement si l'encours du marché au jour J est positif.

```
Encours au jour J     = Σ(déblocages FACTORING PAYE, date valeur ≤ J)
                      − Σ(remboursements FACTORING PAYE, date valeur ≤ J)

Agios HT du jour      = (Encours × Taux Factoring / 100) / 360

TVA du jour           = Agios HT du jour × 10 / 100

Agios TTC du jour     = Agios HT du jour + TVA du jour
```

Si l'encours du jour est nul ou négatif : aucun agios calculé pour ce marché ce jour-là.

#### Agrégation par période

```
Total HT de la période  = Σ(Agios HT de chaque jour calculé)
Total TVA de la période = Σ(TVA de chaque jour calculé)
Total TTC de la période = Σ(Agios TTC de chaque jour calculé)
```

La période est un trimestre pour FC, PREFI et DC ; un mois pour le Factoring (voir §2).

Pour le comptage des jours :

- **FC** — Jours avec découvert : nombre de jours distincts où le solde était négatif.
- **PREFI / DC / Factoring** — Jours avec encours : nombre de jours distincts où au moins un marché avait un encours positif (chaque jour est compté une seule fois, indépendamment du nombre de marchés).
- **Dénominateur** : toujours le nombre total de jours calendaires de la période (ex. T2 = 91 jours ; un mois de 30 ou 31 jours pour le Factoring), indépendamment de la date du dernier calcul.

---

### 5. Valeurs de configuration

| Paramètre | Valeur | Portée |
|-----------|--------|--------|
| Base de division (convention bancaire) | **360 jours** | FC, PREFI, DC et Factoring |
| Taux de TVA sur agios | **10 %** | FC, PREFI, DC et Factoring |
| Arrondi des montants | **HALF_UP, 2 décimales** | Tous les calculs |
| Heure du calcul automatique | **02h00, heure de Casablanca** | Chaque nuit |
| Horizon du calcul | **Veille de la date courante au plus tard** | FC, PREFI, DC et Factoring |

Ces valeurs sont fixes dans le système. Toute modification nécessite une décision d'architecture.

---

### 6. Résolution du taux applicable

#### Facilité de Caisse

Le taux FC est celui de la ligne fonctionnelle générale de nature "Facilité de Caisse" active sur la banque, à la date du calcul.

Si aucune ligne FC active n'existe pour la banque, celle-ci est exclue du calcul.

#### Préfinancement

Le taux PREFI est résolu dans cet ordre de priorité, pour chaque marché :

1. **Ligne spécifique** : ligne fonctionnelle rattachée directement au marché, de nature Préfinancement, active à la date du calcul.
2. **Ligne générale** : ligne fonctionnelle générale de la banque incluant ce marché dans son périmètre, de nature Préfinancement, active à la date du calcul.
3. **Aucune ligne** : le marché est ignoré pour ce trimestre — aucun agios calculé, aucune erreur.

#### Droits Constatés

Le taux DC est résolu dans le même ordre de priorité que le PREFI, pour chaque marché :

1. **Ligne spécifique** : ligne fonctionnelle rattachée directement au marché, de nature Droits Constatés, active à la date du calcul.
2. **Ligne générale** : ligne fonctionnelle générale de la banque incluant ce marché dans son périmètre, de nature Droits Constatés, active à la date du calcul.
3. **Aucune ligne** : le marché est ignoré pour ce trimestre — aucun agios calculé, aucune erreur.

#### Factoring

Le taux Factoring est résolu dans le même ordre de priorité que le PREFI et le DC, pour chaque marché :

1. **Ligne spécifique** : ligne fonctionnelle rattachée directement au marché, de nature Factoring, active à la date du calcul.
2. **Ligne générale** : ligne fonctionnelle générale de la banque incluant ce marché dans son périmètre, de nature Factoring, active à la date du calcul.
3. **Aucune ligne** : le marché est ignoré pour ce mois — aucun agios calculé, aucune erreur.

---

### 7. Cycle de vie d'un agios

#### Agios ponctuel (saisie manuelle ou charge fixe automatique)

```
Création (manuelle ou automatique)
        │
        ▼
  Statut : À PAYER
        │
        ▼ (validation lors du rapprochement mensuel)
  Statut : PAYÉ ──── Transaction bancaire créée
        │
        ▼ (annulation si erreur)
  Statut : À PAYER ──── Transaction retirée du rapprochement
```

#### Agios calculé automatiquement (FC, PREFI, DC et Factoring)

```
Calcul quotidien (chaque nuit)
        │
        ▼
  Résultat provisoire par période — trimestre ou mois selon le type (statut : OUVERT)
        │ (recalcul chaque nuit, jusqu'à la clôture)
        ▼ (action manuelle : clôture de la période)
  Résultat définitif (statut : CLÔTURÉ)
        │
        ▼ (création automatique)
  Agios définitif → Statut : À PAYER
        │
        ▼ (validation lors du rapprochement)
  Statut : PAYÉ
```

#### Workflow de validation

1. L'utilisateur accède à la vue "Encours Agios" de la banque concernée.
2. Il sélectionne l'agios à valider et clique sur **Valider**.
3. Un formulaire s'ouvre : libellé, observation, date d'émission, date de valeur, date d'opération, TVA.
4. Après confirmation, la transaction bancaire est créée et intégrée au rapprochement mensuel.
5. L'agios passe au statut **Payé**.

#### Annulation

Si la transaction est annulée (quel qu'en soit le motif), l'agios associé repasse automatiquement au statut **À payer**. Ses dates de valeur et d'opération sont effacées. Il peut être re-validé.

---

### 8. Calcul automatique trimestriel

#### Fréquence

Le calcul tourne automatiquement chaque nuit à 02h00 (heure de Casablanca). Il couvre, pour toutes les banques éligibles, la période en cours (trimestre pour FC/PREFI/DC, mois pour le Factoring) ainsi que toutes les périodes passées encore ouvertes (non clôturées) — ce qui permet de répercuter toute modification de transaction sur les agios correspondants.

Chaque période est traitée indépendamment : si le calcul d'une période échoue (par exemple parce que le rapprochement du mois courant n'existe pas encore), les autres périodes ouvertes de la même banque sont tout de même recalculées.

Un recalcul manuel est possible à tout moment : soit pour une banque et une période précise, soit globalement via l'action « Recalculer tout », qui rejoue exactement le même traitement que le calcul nocturne (toutes banques, tous types, toutes périodes ouvertes).

#### Périmètre du calcul à une date donnée

Le calcul s'arrête toujours à la **veille de la date courante**. Il n'anticipe pas sur les jours futurs.

Exemple : si le calcul tourne le 30 juin à 02h00, il calcule du 1er avril au 29 juin (ou, pour le Factoring, du 1er au 29 juin).

#### Idempotence

Chaque résultat journalier est identifié de façon unique par la combinaison (date, banque, type, marché). Un recalcul remplace toujours le résultat précédent pour cette combinaison — il ne crée pas de doublon.

#### Périodes clôturées

Une période clôturée (trimestre ou mois selon le type) n'est plus jamais recalculée. Ses montants sont définitifs.

---

### 9. Clôture de trimestre

La clôture est une action manuelle, irréversible, effectuée en fin de période (fin de trimestre pour FC/PREFI/DC, fin de mois pour le Factoring).

**Effets de la clôture :**
1. La période passe au statut **Clôturé** — plus aucun recalcul possible.
2. Si le total TTC de la période est supérieur à zéro, un agios définitif est créé automatiquement avec le montant total TTC de la période.
3. Cet agios définitif entre dans le circuit normal (statut À payer → validation → Payé).

Si le total TTC est nul (aucun jour de découvert ou d'encours sur toute la période), aucun agios définitif n'est créé.

---

### 9 bis. Export PDF

Chacun des écrans du module dispose d'un bouton **Exporter PDF**. Le document généré reprend
l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas
seulement la page affichée, ainsi que le ou les totaux pertinents :

- **Liste des agios ponctuels** : libellé, banque, montant, date d'émission et statut de chaque
  agios correspondant aux filtres (banque, statut, libellé), avec le montant total exporté.
- **Encours Agios** (par banque) : les mêmes colonnes, limitées aux agios à payer de la banque
  concernée, avec le montant total des encours exportés.
- **Tableau de bord du calcul automatique** (trimestres/périodes) : banque, type d'agios, période,
  totaux HT/TVA/TTC, jours calculés et statut de chaque période correspondant aux filtres (banque,
  type, année, période, statut), avec les totaux HT/TVA/TTC cumulés.
- **Détail journalier d'une période** : pour chaque jour, le solde ou l'encours, la base de calcul,
  le taux appliqué, les montants HT/TVA/TTC — et, pour les types multi-marchés (Préfinancement,
  Droits Constatés, Factoring), le marché concerné — avec les totaux HT/TVA/TTC et le nombre de
  jours avec découvert ou encours sur la période.

---

### 10. Charges fixes récurrentes

Certains agios sont récurrents (mensuels ou trimestriels). Ils sont configurés dans le système et
générés automatiquement le **dernier jour de chaque mois, à 07h00**.

**Règle d'idempotence** : avant toute génération automatique, le système vérifie si un agios avec le même libellé a déjà été créé pour la période concernée. Si oui, la génération est ignorée — aucun doublon n'est possible.

Types de charges pouvant être automatisées : frais bancaires, impôts et taxes, agios récurrents, prélèvements.

Les définitions de ces charges (banque, libellé, montant, jour d'émission, etc.) sont
administrables via l'écran Administration → Charges fixes bancaires (voir
`docs/modules/frais-bancaires.md`, section 5, et `docs/rbac/frais-bancaires.md`).

---

### 11. Règles métier

#### Création d'un agios

1. La banque doit exister dans le système.
2. Le montant doit être strictement positif.
3. Le libellé est obligatoire.
4. À la création, le statut est toujours **À payer**.
5. Les dates de valeur et d'opération sont vides à la création — elles sont renseignées uniquement lors de la validation.
6. Un agios ponctuel ne peut être supprimé que s'il est encore au statut **À payer**. Toute tentative de suppression d'un agios déjà Payé est rejetée.

#### Validation

6. Un agios déjà **Payé** ne peut pas être re-validé.
7. La validation ne peut pas être effectuée sur un rapprochement finalisé ou fermé.
8. Si le rapprochement cible est en clôture provisoire, le recalcul de solde en cascade est déclenché automatiquement après la validation.

#### Calcul automatique

9. Une période clôturée (trimestre ou mois selon le type) n'est jamais recalculée.
10. Le calcul ne peut pas dépasser la veille de la date courante.
11. Un marché sans ligne préfinancement active est ignoré — aucune erreur n'est générée.
12. Seules les opérations de préfinancement au statut **Payé** contribuent à l'encours PREFI. Les opérations prévues n'ont aucun effet.
13. Un marché sans ligne droits constatés active est ignoré — aucune erreur n'est générée.
14. Seules les opérations de droits constatés au statut **Payé** contribuent à l'encours DC. Les opérations prévues n'ont aucun effet.
15. Un marché sans ligne factoring active est ignoré — aucune erreur n'est générée.
16. Seules les opérations de factoring au statut **Payé** contribuent à l'encours Factoring. Les opérations prévues n'ont aucun effet.
17. Si l'encours d'un marché (PREFI, DC ou Factoring) est nul ou négatif un jour donné, ce jour n'est pas compté dans les jours avec encours et ne contribue pas aux totaux.
18. À la clôture, l'agios définitif n'est créé que si le total TTC est strictement supérieur à zéro.
19. Le Factoring est le seul type dont l'agrégat est mensuel (1–12) plutôt que trimestriel (1–4) — le champ technique "trimestre" contient alors le numéro de mois.

#### Encours à payer

20. L'encours visible dans la vue "Encours Agios" inclut uniquement les agios au statut **À payer** dont la date d'émission est antérieure ou égale à la fin du mois courant.

---

### 12. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter les agios | Équipe Finance (membres et directeur), Administrateur système |
| Créer / modifier un agios | Équipe Finance (membres et directeur), Administrateur système |
| Supprimer un agios | Administrateur système uniquement |
| Valider un agios (rapprochement) | Équipe Finance (membres et directeur), Administrateur système |
| Consulter le calcul automatique | Équipe Finance (membres et directeur), Administrateur système |
| Déclencher un recalcul manuel | Équipe Finance (membres et directeur), Administrateur système |
| Clôturer un trimestre | Équipe Finance (membres et directeur), Administrateur système |
| Basculer manuellement le statut | Administrateur système uniquement (correction exceptionnelle) |

---

### 13. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Rapprochement** | La validation d'un agios crée une transaction dans le rapprochement mensuel. L'annulation d'une transaction retire l'agios de son état payé. |
| **Lignes fonctionnelles** | Le calcul FC, PREFI et DC dépend des lignes fonctionnelles actives de la banque pour obtenir le taux applicable. |
| **Marchés** | Le calcul PREFI et DC parcourt les marchés de la banque. Un marché sans ligne active pour le type concerné est ignoré. |
| **Préfinancement** | L'encours PREFI est reconstitué depuis les opérations de déblocage et remboursement du module Préfinancement. |
| **Droits Constatés** | L'encours DC est reconstitué depuis les opérations de déblocage et remboursement du module Droits Constatés. |
| **Factoring** | L'encours Factoring est reconstitué depuis les opérations de déblocage et remboursement du module Factoring. |
| **Charges fixes** | Le module Charges Fixes génère automatiquement des agios récurrents en début de mois. |

---

<a id="chap24"></a>

## Chapitre 24 — Module Impôts et Taxes


> Source de vérité métier pour le module Impôts et Taxes.

---

### 1. Périmètre du module

Ce module gère les impôts et taxes de Staport auprès de ses banques : TVA, IR, IS, retenue à la
source, pénalités de retard, et autres prélèvements fiscaux ou parafiscaux (DGI, ADII, taxe
professionnelle, taxe communale). Un impôt/taxe peut être saisi manuellement par l'équipe Finance,
ou généré automatiquement selon les cas décrits en section 4.

---

### 2. Définitions métier

**Impôt/Taxe** : opération associée à une banque, un libellé, une observation, un montant, un motif
et une date d'émission.

**Motif** : nature de l'impôt/taxe, l'un des suivants :
- TVA
- IR
- Retenue à la source
- Pénalité de retard
- Direction Générale des Impôts (DGI)
- Administration des Douanes et Impôts Indirects (ADII)
- Impôts sur les sociétés (IS)
- Communal
- TVA sur commission sur caution
- Taxe Professionnelle
- Vignette

**Statut** : état de l'impôt/taxe, l'un des deux suivants :
- **PREVU** : impôt/taxe enregistré, pas encore réglé.
- **PAYE** : impôt/taxe validé/réglé côté G-Banque.

**Encours** : ensemble des impôts/taxes PREVU dont la date d'émission ne dépasse pas la fin du mois
en cours.

---

### 3. Cycle de vie d'un impôt/taxe

```
Création (manuelle ou automatique — voir section 4)
  ↓
PREVU
  │
  ├──→ Validation (rapprochement) ──→ PAYE
  │
  └──→ Fusion avec d'autres impôts/taxes PREVU (TVA) ──→ nouvel impôt/taxe PREVU
```

---

### 4. Règles métier

#### Création manuelle

1. La banque associée, le motif, le libellé, l'observation, le montant et la date d'émission sont
   obligatoires.
2. Le statut initial d'un impôt/taxe créé est toujours PREVU.

#### Génération automatique récurrente (charges fixes)

3. Certains impôts/taxes récurrents sont générés automatiquement, le **dernier jour de chaque mois à
   07h00**, pour certaines banques (ex. TVA mensuelle sur relevé bancaire, IS trimestriel). Cette
   génération est idempotente : un même impôt/taxe (même banque, même libellé, même fenêtre de
   période — mois ou trimestre selon le cas) n'est jamais généré deux fois dans la même période.
4. Le mécanisme de génération automatique est commun aux modules Impôts et Taxes, Frais bancaires,
   Agios et Prélèvements — chacun avec ses propres charges configurées par banque.
5. Depuis le 16/07/2026, ces charges (banque, libellé, montant, jour d'émission, etc.) ne sont plus
   codées en dur : elles sont administrables via l'écran Administration → Charges fixes bancaires
   (voir `docs/modules/frais-bancaires.md`, section 5, et `docs/rbac/frais-bancaires.md`).

#### Commission automatique (règle SAHAM BANK)

6. Tout impôt/taxe émis par la banque **SAHAM BANK**, quel que soit son motif, génère automatiquement
   une commission fixe de 5,50 MAD à sa création. Cette commission n'est générée qu'à la création —
   elle n'est pas resynchronisée si l'impôt/taxe est modifié ou supprimé ultérieurement.

#### TVA générée depuis une Caution

6. Lorsqu'une commission est générée automatiquement sur une caution (module Cautions), une TVA
   correspondante (motif "TVA sur commission sur caution") est créée automatiquement dans ce module,
   rattachée à la fois à la commission source et à la caution d'origine.
7. Si la commission source est supprimée, la TVA liée est supprimée automatiquement. Si le montant de
   la commission source est modifié, le montant de la TVA liée est mis à jour automatiquement.

#### Fusion de TVA

8. Plusieurs impôts/taxes de motif TVA peuvent être fusionnés en un seul, sous réserve que :
   - au moins 2 TVA soient sélectionnées ;
   - toutes soient au statut PREVU ;
   - toutes appartiennent à la même banque ;
   - toutes aient la même date d'émission ;
   - toutes aient le même motif.
9. La fusion supprime les TVA sources et crée une nouvelle TVA au statut PREVU, dont le montant est
   la somme des montants sources et dont le libellé est fourni par l'utilisateur. La TVA fusionnée
   n'est plus rattachée à aucune commission source.

#### Modification et suppression

10. Un impôt/taxe déjà **PAYE** ne peut plus être ni modifié ni supprimé — harmonisé le 14/07/2026
    avec le comportement des autres moyens de paiement (Prélèvement, etc.). Toute tentative de
    modification ou de suppression sur un impôt/taxe PAYE est rejetée.

#### Changement de statut manuel

11. Le statut d'un impôt/taxe peut être basculé manuellement entre PREVU et PAYE.

#### Validation (rapprochement)

12. Valider un impôt/taxe crée une transaction bancaire rattachée à un rapprochement, calcule la TVA
    déductible/collectée associée, puis marque l'impôt/taxe PAYE.

---

### 5. Encours

L'écran d'encours affiche, pour une banque donnée, l'ensemble des impôts/taxes PREVU dont la date
d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé.

---

### 5 bis. Export PDF

L'écran **Impôts et Taxes**, l'écran d'**encours** d'une banque, ainsi que l'écran **TVA sur
cautions** du module Cautions (qui affiche les impôts/taxes de motif "TVA sur commission sur
caution"), disposent chacun d'un bouton **Exporter PDF** (encours : nouveau, 23/07/2026). Le
document généré reprend l'intégralité des résultats correspondant aux filtres actuellement
appliqués à l'écran (banque, libellé, motif, montant, statut pour la liste ; libellé, motif,
montant pour l'encours — la banque et le statut PREVU sont implicites), pas seulement la page
affichée, ainsi que le montant total cumulé.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier / fusionner un impôt/taxe | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer un impôt/taxe | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'un impôt/taxe est historisée : auteur, date et valeurs
avant/après restent consultables ultérieurement. La fusion de TVA est également historisée comme une
création (l'impôt/taxe fusionné constitue une nouvelle entité).

---

### 8. Logs

La création, la modification, la suppression, le changement de statut, la fusion et la génération
automatique (charges fixes, commission SAHAM BANK, TVA sur caution) d'un impôt/taxe génèrent chacun
un événement journalisé, utile au diagnostic.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque impôt/taxe est rattaché à un compte bancaire. |
| **Rapprochement** | La validation d'un impôt/taxe crée une transaction rattachée à un rapprochement. |
| **Commissions** | Un impôt/taxe SAHAM BANK génère automatiquement une commission. Inversement, une TVA peut être générée depuis une commission (voir Cautions). |
| **Cautions** | La clôture d'une caution génère une commission qui génère elle-même une TVA dans ce module ; la suppression ou la modification de la commission source répercute automatiquement la TVA liée. |
| **Frais bancaires, Agios, Prélèvements** | Partagent le même mécanisme de génération automatique récurrente (charges fixes). |

---

<a id="chap25"></a>

## Chapitre 25 — Module Commission


> Source de vérité métier pour le module Commission.

---

### 1. Périmètre du module

Le module Commission gère trois responsabilités distinctes :

**Génération configurable des commissions** : les autres modules (leasing, virements, LCN de décaissement, mises à disposition de décaissement, cautions, etc.) génèrent automatiquement des commissions lors de certaines opérations, selon des règles paramétrables par l'administrateur (banque, type d'opération, organisme).

**Génération automatique fixe** : certaines commissions sont générées par une règle métier fixe, indépendante des règles configurables — c'est le cas de la commission sur impôt et taxe SAHAM BANK (voir section 7).

**Suivi et règlement** : ce module enregistre toutes les commissions, quelle que soit leur origine, et en assure le suivi jusqu'au paiement.

---

### 2. Définitions métier

**Commission** : charge facturée à l'entreprise par une banque à l'occasion d'une opération financière (paiement d'un loyer leasing, émission d'un virement, etc.). Elle est générée automatiquement par le module source au moment de l'opération.

**Règle de commission** : paramétrage qui définit le montant de commission applicable pour une combinaison (banque, type d'opération, condition). Ces règles sont gérées depuis l'interface d'administration.

**Organisme** : partenaire externe impliqué dans l'opération (ex. : BMCI Leasing, CIH Leasing). Certaines règles de commission dépendent de l'organisme concerné.

**Type d'opération** : nature de l'opération qui génère la commission (leasing, virement, LCN de décaissement, mise à disposition de décaissement, caution, crédit moyen terme, etc.).

**Condition** : critère qui détermine quelle règle appliquer lorsque plusieurs règles coexistent pour la même banque et le même type d'opération :

| Condition | Signification |
|-----------|---------------|
| Par défaut (DEFAULT) | Règle générale — s'applique si aucune autre condition ne correspond |
| Si correspondance (SI_MATCH) | S'applique si l'organisme de l'opération correspond exactement à celui de la règle |
| Si non-correspondance (SI_NON_MATCH) | S'applique si une règle SI_MATCH existe pour la banque mais qu'aucune ne correspond à l'organisme de l'opération |

---

### 3. Règles de résolution du montant de commission

Lorsqu'une opération génère une commission, le système cherche le montant applicable selon cet ordre de priorité :

```
1. Existe-t-il une règle SI_MATCH pour cette banque + type d'opération + organisme ?
   → OUI : appliquer ce montant

2. Existe-t-il au moins une règle SI_MATCH pour cette banque + type d'opération (quel que soit l'organisme),
   mais aucune ne correspond à l'organisme de l'opération ?
   → OUI : chercher une règle SI_NON_MATCH et appliquer ce montant

3. Aucune règle SI_MATCH n'existe pour cette banque + type d'opération ?
   → Chercher une règle DEFAULT et appliquer ce montant

4. Aucune règle trouvée ?
   → Aucune commission générée (aucune erreur)
```

La recherche est insensible à la casse sur le nom de la banque et de l'organisme.

---

### 4. Cycle de vie d'une commission

```
Opération déclenchante (ex. : loyer leasing validé)
        │
        ↓
Résolution du montant selon les règles configurées
        │
        ├── Aucune règle → aucune commission créée
        │
        └── Règle trouvée → Commission créée (statut : À PAYER)
                │
                ↓ (validation lors du rapprochement bancaire)
        Statut : PAYÉ
```

Les commissions suivent le même circuit de validation que les autres charges : elles apparaissent dans les encours jusqu'à leur règlement lors du rapprochement mensuel.

Une commission ne peut être supprimée que si elle est encore au statut **À PAYER**. Toute tentative
de suppression d'une commission déjà PAYÉE est rejetée.

---

### 5. Configuration des règles

Les règles de commission sont entièrement gérées depuis l'interface d'administration, sans modification du code. Chaque règle définit :

- La **banque** concernée
- Le **type d'opération** (leasing, virement, LCN de décaissement, mise à disposition de décaissement, caution, etc.)
- La **condition** (DEFAULT, SI_MATCH, SI_NON_MATCH)
- L'**organisme** (obligatoire pour SI_MATCH, vide pour DEFAULT et SI_NON_MATCH)
- Le **montant** en MAD

La suppression d'une règle est immédiate et sans impact sur les commissions déjà générées.

---

### 6. Valeurs configurées

Règles actuellement en base (`commission_config`), confirmées par requête le 14/07/2026 —
modifiables à tout moment depuis l'administration :

| Banque | Organisme | Type | Condition | Montant (MAD) |
|--------|-----------|------|-----------|---------------|
| CIH | — | Leasing | Par défaut | 11,00 |
| SAHAM BANK | — | Leasing | Par défaut | 11,00 |
| BMCI | BMCI LEASING | Leasing | Si correspondance | 16,50 |
| BMCI | — | Leasing | Si non-correspondance | 55,00 |
| BMCI | BMCI | Virement | Si correspondance | 27,50 |
| BMCI | — | Virement | Si non-correspondance | 33,00 |
| CDM | — | Virement | Par défaut | 22,00 |
| BP | — | LCN de décaissement | Par défaut | 11,00 |
| BMCI | — | Mise à disposition | Par défaut | 33,00 |

---

### 7. Commission automatique sur impôt et taxe (SAHAM BANK)

**Règle métier :** tout impôt et taxe créé pour la banque **SAHAM BANK**, quel que soit son motif (TVA, IR, IS, Communal, Pénalité de retard, DGI, ADII, Taxe professionnelle, Vignette, Retenue à la source, TVA sur commission caution, etc.), génère automatiquement une commission.

Caractéristiques de la commission générée :

| Champ | Valeur |
|-------|--------|
| Montant | Fixe : 5,50 DH |
| Banque | Identique à celle de l'impôt/taxe (SAHAM BANK) |
| Date d'émission | Identique à la date d'émission de l'impôt/taxe |
| Libellé | « Commission sur impôt et taxe — {motif de l'impôt/taxe} » |
| Observation | Identique au libellé |
| Statut initial | À payer |

**Déclenchement :** uniquement à la création de l'impôt/taxe. Une modification ou une suppression ultérieure de l'impôt/taxe n'a aucun effet automatique sur la commission déjà générée — celle-ci doit être traitée manuellement si nécessaire.

**Portée :** cette règle est indépendante du mécanisme de règles configurables décrit aux sections 3 à 6 — elle ne passe pas par la résolution banque/organisme/condition et ne peut pas être désactivée depuis l'administration. Elle ne s'applique qu'à la banque SAHAM BANK.

---

### 8. Règles métier

1. Une règle SI_MATCH doit toujours préciser un organisme — une règle SI_MATCH sans organisme est invalide.
2. Une règle DEFAULT ou SI_NON_MATCH ne doit pas avoir d'organisme.
3. Il peut exister plusieurs règles SI_MATCH pour la même banque, mais chacune pour un organisme distinct.
4. Il ne doit exister qu'une seule règle DEFAULT et qu'une seule règle SI_NON_MATCH par combinaison (banque + type d'opération).
5. Si aucune règle n'est configurée pour une banque + type d'opération, aucune commission n'est générée — ce n'est pas une erreur.
6. La suppression d'une règle n'affecte pas les commissions déjà créées.
7. Tout impôt/taxe créé pour SAHAM BANK génère systématiquement une commission de 5,50 DH (voir section 7) — cette règle fixe s'ajoute aux règles configurables et ne les remplace pas.
8. **Virements internes (bénéficiaire STAPORT)** : un virement dont le bénéficiaire est STAPORT (mouvement entre banques Staport) ne génère normalement aucune commission, ni côté encaissement ni côté décaissement. Exception confirmée : côté décaissement uniquement, lorsque la banque émettrice est **BMCI**, une commission est générée normalement (résolue via les règles configurées, aucun montant codé en dur) — le côté encaissement de ce même virement interne reste exclu pour éviter une double facturation.
9. **Fusion interdite pour BMCI** : la fusion de commissions sur cautions (section 9) n'est pas autorisée lorsque la banque concernée est BMCI, car sa TVA est gérée séparément — fusionner briserait la cohérence comptable.
10. Les commissions sur LCN et mises à disposition ne sont générées que pour les opérations de **décaissement** — les opérations d'encaissement (créées manuellement) ne déclenchent jamais de commission via ce mécanisme.

---

### 9. Fusion de commissions sur cautions

La fusion regroupe plusieurs commissions sur cautions distinctes (statut PREVU) en une seule commission, avec recalcul de la TVA associée.

Conditions requises pour fusionner un ensemble de commissions :

- toutes de type **CAUTION** ;
- toutes au statut **PREVU** (une commission déjà PAYÉE ne peut pas être fusionnée) ;
- toutes rattachées à la **même banque** et à la **même date d'émission** ;
- banque différente de **BMCI** (voir règle métier 9, section 8).

Effet de la fusion (opération atomique) :

1. Suppression de la TVA liée à chaque commission source (si une TVA avait été générée).
2. Suppression des commissions sources.
3. Création d'une commission fusionnée : montant TTC = somme des montants HT des commissions sources × 1,10 (TVA 10 % incluse dans le nouveau montant).
4. Aucune nouvelle TVA n'est créée après une fusion — le montant fusionné intègre déjà la TVA.

---

### 9 bis. Export PDF

L'écran **Commissions**, ainsi que l'écran d'**encours** d'une banque (nouveau, 23/07/2026),
disposent chacun d'un bouton **Exporter PDF**. Le document généré reprend l'intégralité des
résultats correspondant aux filtres actuellement appliqués à l'écran (banque, libellé, type
d'opération, montant, statut, nature pour la liste ; libellé, type d'opération, montant pour
l'encours — la banque et le statut À payer sont implicites), pas seulement la page affichée, ainsi
que le montant total cumulé.

---

### 10. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier une commission | Équipe Finance (membres et directeur), Administrateur système |
| Fusionner des commissions sur cautions | Équipe Finance (membres et directeur), Administrateur système |
| Exporter les commissions en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Supprimer une commission | Administrateur système uniquement |
| Basculer le statut d'une commission (PRÉVU ↔ PAYÉ) | Administrateur système uniquement |
| Consulter les règles de commission | Administrateur système uniquement |
| Créer / modifier / supprimer une règle | Administrateur système uniquement |

Détail technique des accès (endpoints, guards, sidebar) : `docs/rbac/commission.md`.

---

### 11. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|--------|------------------------|
| **Leasing** | Génère automatiquement une commission par échéance leasing, selon les règles configurées. Lors d'une modification de contrat, les anciennes commissions sont supprimées et recréées. |
| **Virements** | Peut générer une commission lors de l'émission d'un virement selon les règles banque/type (voir règle métier 8, section 8, pour l'exception des virements internes). |
| **LCN** | Peut générer une commission lors de l'émission d'un LCN de décaissement selon les règles banque/type. |
| **Mise à disposition** | Peut générer une commission lors de l'émission d'une mise à disposition de décaissement selon les règles banque/type. |
| **Cautions** | Peut générer des commissions sur les cautions émises (prorata d'entrée, mensuelle, clôture) ; ces commissions peuvent ensuite être fusionnées (section 9). |
| **Impôts et taxes** | Tout impôt/taxe créé pour SAHAM BANK génère automatiquement une commission fixe de 5,50 DH (voir section 7). |
| **Rapprochement** | Les commissions générées entrent dans le circuit de validation et de rapprochement bancaire mensuel. |

---

À côté des charges normalement facturées, certains mouvements bancaires correspondent à des anomalies — rejets, erreurs de saisie, sommes à restituer — qui nécessitent un suivi et une régularisation dédiés.

# Anomalies

---

<a id="chap26"></a>

## Chapitre 26 — Module Impayées


> Source de vérité métier pour le module Impayées.

---

### 1. Périmètre du module

Ce module gère les impayées de Staport : chèques, effets ou autres moyens de paiement présentés à
l'encaissement ou à l'encaissement/décaissement et rejetés par la banque. Une impayée est toujours
saisie manuellement par l'équipe Finance, en rattachement à la banque concernée et au moyen de
paiement d'origine (motif).

---

### 2. Définitions métier

**Impayée** : opération associée à une banque, un type (encaissement ou décaissement), un motif, un
libellé, une observation, un montant et une date d'émission.

**Type** : sens de l'impayée, l'un des deux suivants :
- **ENCAISSEMENT** : le moyen de paiement rejeté était destiné à un encaissement pour Staport.
- **DECAISSEMENT** : le moyen de paiement rejeté était destiné à un décaissement de Staport.

**Motif** : moyen de paiement d'origine à l'origine de l'impayée (Chèque, LCN, Virement, Mise à
Disposition, Prélèvement, etc. — même liste que les sources de transaction utilisées ailleurs dans
G-Banque).

**Numéro** (ajouté le 03/08/2026) : numéro du chèque ou de la LCN d'origine. Obligatoire lorsque le
motif vaut Chèque, LCN Encaissement, LCN Décaissement ou LCN Escompte ; facultatif pour tous les
autres motifs.

**Statut** : état de l'impayée, l'un des deux suivants :
- **PREVU** : impayée enregistrée, pas encore régularisée.
- **PAYE** : impayée régularisée (validée) côté G-Banque.

**Encours** : ensemble des impayées PREVU, d'un type donné, dont la date d'émission ne dépasse pas
la fin du mois en cours.

---

### 3. Cycle de vie d'une impayée

```
Création manuelle
  ↓
PREVU
  │
  └──→ Validation (rapprochement) ──→ PAYE
```

---

### 4. Règles métier

#### Création

1. La banque associée, le type (encaissement/décaissement), le motif, le montant (strictement
   positif), la date d'émission, le libellé et l'observation sont saisis à la création. Le numéro
   est également saisi à la création — obligatoire si le motif est Chèque, LCN Encaissement, LCN
   Décaissement ou LCN Escompte, facultatif sinon.
2. Le statut initial d'une impayée créée est toujours PREVU.

#### Modification

3. Une impayée peut être modifiée tant qu'elle n'a pas été supprimée. Le type d'origine n'est pas
   modifiable après création (seuls banque, montant, motif, numéro, date d'émission, libellé et
   observation le sont via l'écran de modification). La même règle d'obligation du numéro selon le
   motif s'applique à la modification : si le motif est changé vers Chèque ou une variante LCN, le
   numéro devient obligatoire.

#### Changement de statut manuel

4. Le statut d'une impayée peut être basculé manuellement entre PREVU et PAYE.

#### Suppression

5. Une impayée ne peut être supprimée que si elle est encore au statut **PREVU**. Toute tentative de
   suppression d'une impayée déjà PAYE est rejetée.

#### Validation (rapprochement)

6. Valider une impayée crée une transaction bancaire rattachée à un rapprochement, calcule la TVA
   déductible ou collectée associée selon le type (DECAISSEMENT → TVA déductible, ENCAISSEMENT → TVA
   collectée), puis marque l'impayée PAYE.

---

### 5. Encours

L'écran d'encours affiche, pour une banque et un type (encaissement/décaissement) donnés, l'ensemble
des impayées PREVU dont la date d'émission ne dépasse pas la fin du mois en cours, ainsi que le
montant total cumulé.

---

### 5 bis. Export PDF

L'écran **Impayées**, ainsi que les deux écrans d'**encours** (encaissement et décaissement, nouveau,
23/07/2026), disposent chacun d'un bouton **Exporter PDF** — aucun export n'existait sur ce module
avant cette date. Le document généré reprend l'intégralité des résultats correspondant aux filtres
actuellement appliqués à l'écran (banque, libellé, motif, type, montant, statut pour la liste ;
libellé, motif, montant pour l'encours — la banque, le type et le statut PREVU sont implicites, le
type étant fixé par l'écran d'encours consulté), pas seulement la page affichée, ainsi que le
montant total cumulé. Chaque ligne du document inclut désormais la colonne numéro (03/08/2026).

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier une impayée | Équipe Finance (membres et directeur), Administrateur système |
| Exporter en PDF (liste ou encours) | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer une impayée | Administrateur système uniquement |

---

### 7. Audit

Toute création, modification ou suppression d'une impayée est historisée : auteur, date et valeurs
avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression, le changement de statut et la validation
(rapprochement) d'une impayée génèrent chacun un événement journalisé, utile au diagnostic.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque impayée est rattachée à un compte bancaire. |
| **Rapprochement** | La validation d'une impayée crée une transaction rattachée à un rapprochement. |
| **Moyens de paiement (Chèque, LCN, Virement, Mise à Disposition, Prélèvement)** | Le champ motif référence le moyen de paiement d'origine ayant généré l'impayée, sans lien technique direct (simple valeur d'énumération). |

---

<a id="chap27"></a>

## Chapitre 27 — Module Opérations Erronées


> Source de vérité métier pour le module Opérations Erronées.

---

### 1. Périmètre du module

Ce module gère les opérations erronées de Staport : des mouvements bancaires enregistrés par erreur
(saisie incorrecte, doublon, opération à tort) et devant être régularisés. Une opération erronée est
toujours saisie manuellement par l'équipe Finance, en rattachement à la banque concernée et au moyen
de paiement d'origine (motif).

---

### 2. Définitions métier

**Opération erronée** : opération associée à une banque, un motif, un libellé, une observation, un
montant et une date d'émission.

**Motif** : moyen de paiement ou origine de l'opération erronée (Chèque, LCN, Virement, Mise à
Disposition, Prélèvement, etc. — même liste que les sources de transaction utilisées ailleurs dans
G-Banque).

**Statut** : état de l'opération erronée, l'un des deux suivants :
- **PREVU** : opération erronée enregistrée, pas encore régularisée.
- **PAYE** : opération erronée régularisée (validée) côté G-Banque.

**Encours** : ensemble des opérations erronées PREVU dont la date d'émission ne dépasse pas la fin
du mois en cours.

---

### 3. Cycle de vie d'une opération erronée

```
Création manuelle
  ↓
PREVU
  │
  └──→ Validation (rapprochement) ──→ PAYE
```

---

### 4. Règles métier

#### Création

1. La banque associée, le motif, le montant, la date d'émission, le libellé et l'observation sont
   saisis à la création.
2. Le statut initial d'une opération erronée créée est toujours PREVU.

#### Modification

3. Une opération erronée peut être modifiée tant qu'elle n'a pas été supprimée.

#### Changement de statut manuel

4. Le statut d'une opération erronée peut être basculé manuellement entre PREVU et PAYE.

#### Suppression

5. Une opération erronée ne peut être supprimée que si elle est encore au statut **PREVU**. Toute
   tentative de suppression d'une opération erronée déjà PAYE est rejetée.

#### Validation (rapprochement)

6. Valider une opération erronée crée une transaction bancaire rattachée à un rapprochement, calcule
   la TVA déductible ou collectée associée, puis marque l'opération erronée PAYE.

---

### 5. Encours

L'écran d'encours affiche, pour une banque donnée, l'ensemble des opérations erronées PREVU dont la
date d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé.

---

### 5 bis. Export PDF

L'écran **Opérations Erronées**, ainsi que l'écran d'**encours** d'une banque (nouveau, 23/07/2026),
disposent chacun d'un bouton **Exporter PDF** — aucun export n'existait sur ce module avant cette
date. Le document généré reprend l'intégralité des résultats correspondant aux filtres actuellement
appliqués à l'écran (banque, libellé, motif, statut pour la liste ; libellé, motif, montant pour
l'encours — la banque et le statut PREVU sont implicites), pas seulement la page affichée, ainsi que
le montant total cumulé.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier une opération erronée | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer une opération erronée | Administrateur système uniquement |
| Exporter la liste ou l'encours en PDF | Équipe Finance (membres et directeur), Administrateur système |

---

### 7. Audit

Toute création, modification ou suppression d'une opération erronée est historisée : auteur, date et
valeurs avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression, le changement de statut et la validation
(rapprochement) d'une opération erronée génèrent chacun un événement journalisé, utile au
diagnostic.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque opération erronée est rattachée à un compte bancaire. |
| **Rapprochement** | La validation d'une opération erronée crée une transaction rattachée à un rapprochement. |
| **Moyens de paiement (Chèque, LCN, Virement, Mise à Disposition, Prélèvement)** | Le champ motif référence le moyen de paiement d'origine ayant généré l'opération erronée, sans lien technique direct (simple valeur d'énumération). |

---

<a id="chap28"></a>

## Chapitre 28 — Module Provision


> Source de vérité métier pour le module Provision.

---

### 1. Périmètre du module

Ce module gère les provisions de trésorerie de Staport : des écritures manuelles ponctuelles,
rattachées à une banque, permettant d'anticiper un encaissement ou un décaissement futur avant que
l'opération réelle ne soit constatée sur le relevé bancaire. Contrairement aux autres modules
d'anomalies (Opérations erronées, Impayées, Restitution) qui ne génèrent qu'un seul sens de
mouvement, une provision peut représenter indifféremment un encaissement ou un décaissement, selon
son sens.

---

### 2. Définitions métier

**Provision** : opération financière manuelle associée à une banque, un motif, un libellé, une
observation, un montant, une date d'émission et un sens.

**Motif** : origine ou nature de la provision (même liste que les sources de transaction utilisées
ailleurs dans G-Banque — Chèque, LCN, Virement, Frais bancaires, etc.), à titre de catégorisation
uniquement.

**Sens de l'opération** : ENCAISSEMENT ou DECAISSEMENT — détermine l'impact de la provision sur le
solde de la banque une fois validée. Le montant d'une provision est toujours saisi positif ; c'est
le sens, et lui seul, qui porte la direction du mouvement.

**Statut** : état de la provision, l'un des deux suivants :
- **PREVU** : provision enregistrée, pas encore validée.
- **PAYE** : provision validée (rapprochée) côté G-Banque.

**Encours** : ensemble des provisions PREVU dont la date d'émission ne dépasse pas la fin du mois en
cours. Parce qu'une provision peut être un encaissement ou un décaissement, l'encours est présenté
sur deux écrans distincts (décaissements et encaissements), chacun filtré sur le sens correspondant.

---

### 3. Cycle de vie d'une provision

```
Création manuelle (sens choisi à la création)
  ↓
PREVU
  │
  └──→ Validation (rapprochement) ──→ PAYE
```

---

### 4. Règles métier

#### Création

1. La banque associée, le motif, le montant (toujours positif), le sens (ENCAISSEMENT ou
   DECAISSEMENT), la date d'émission, le libellé et l'observation sont saisis à la création.
2. Le statut initial d'une provision créée est toujours PREVU.

#### Modification

3. Une provision peut être modifiée tant qu'elle n'a pas été supprimée.

#### Changement de statut manuel

4. Le statut d'une provision peut être basculé manuellement entre PREVU et PAYE.

#### Suppression

5. Une provision ne peut être supprimée que si elle est encore au statut **PREVU**. Toute tentative
   de suppression d'une provision déjà PAYE est rejetée.

#### Validation (rapprochement)

6. Valider une provision crée une transaction bancaire rattachée à un rapprochement. Le sens de la
   provision détermine l'écriture générée :
   - **DECAISSEMENT** : la transaction porte le montant au débit, et la TVA calculée est déductible.
   - **ENCAISSEMENT** : la transaction porte le montant au crédit, et la TVA calculée est collectée.
7. Une fois la transaction créée, la provision passe au statut PAYE. Comme pour les autres modules
   à validation (Frais bancaires, Opérations erronées), le taux de TVA est saisi au moment de la
   validation — il n'est pas un champ de l'entité Provision elle-même.

---

### 5. Encours

Deux écrans d'encours distincts affichent, pour une banque donnée, l'ensemble des provisions PREVU
dont la date d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé :
un écran pour les provisions de sens DECAISSEMENT, un écran pour les provisions de sens
ENCAISSEMENT — même principe que les modules à double sens existants (Virement, Chèque, LCN, Mise à
disposition, Virement International).

---

### 6. Export PDF

L'écran **Provisions**, ainsi que chacun des deux écrans d'**encours** (décaissements et
encaissements) d'une banque, disposent d'un bouton **Exporter PDF**. Le document généré reprend
l'intégralité des résultats correspondant aux filtres actuellement appliqués à l'écran, pas
seulement la page affichée, ainsi que le montant total cumulé.

---

### 7. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier une provision | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer une provision | Administrateur système uniquement |
| Exporter la liste ou l'encours en PDF | Équipe Finance (membres et directeur), Administrateur système |

---

### 8. Audit

Toute création, modification ou suppression d'une provision est historisée : auteur, date et valeurs
avant/après restent consultables ultérieurement.

---

### 9. Logs

La création, la modification, la suppression, le changement de statut et la validation
(rapprochement) d'une provision génèrent chacun un événement journalisé, utile au diagnostic.

---

### 10. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque provision est rattachée à un compte bancaire. |
| **Rapprochement** | La validation d'une provision crée une transaction rattachée à un rapprochement, et peut déclencher un recalcul du solde de la banque. |

---

<a id="chap29"></a>

## Chapitre 29 — Module Restitution


> Source de vérité métier pour le module Restitution.

---

### 1. Périmètre du module

Ce module gère les restitutions de Staport auprès de ses banques — des sommes qu'une banque doit
reverser à Staport. Une restitution est toujours saisie manuellement par l'équipe Finance, en
rattachement à la banque concernée.

---

### 2. Définitions métier

**Restitution** : opération associée à une banque, un libellé, une observation, un montant et une
date d'émission.

**Statut** : état de la restitution, l'un des deux suivants :
- **PREVU** : restitution enregistrée, pas encore encaissée.
- **PAYE** : restitution régularisée (validée) côté G-Banque.

**Encours** : ensemble des restitutions PREVU dont la date d'émission ne dépasse pas la fin du mois
en cours.

---

### 3. Cycle de vie d'une restitution

```
Création manuelle
  ↓
PREVU
  │
  └──→ Validation (rapprochement) ──→ PAYE
```

---

### 4. Règles métier

#### Création

1. La banque associée, le montant, la date d'émission, le libellé et l'observation sont saisis à la
   création.
2. Le statut initial d'une restitution créée est toujours PREVU.

#### Modification

3. Une restitution peut être modifiée quel que soit son statut.

#### Changement de statut manuel

4. Le statut d'une restitution peut être basculé manuellement entre PREVU et PAYE.

#### Suppression

5. Une restitution ne peut être supprimée que si elle est encore au statut **PREVU**. Toute
   tentative de suppression d'une restitution déjà PAYE est rejetée.

#### Validation (rapprochement)

6. Valider une restitution crée une transaction bancaire (toujours un encaissement) rattachée à un
   rapprochement, calcule la TVA collectée associée, puis marque la restitution PAYE.

---

### 5. Encours

L'écran d'encours affiche, pour une banque donnée, l'ensemble des restitutions PREVU dont la date
d'émission ne dépasse pas la fin du mois en cours, ainsi que le montant total cumulé.

---

### 5 bis. Export PDF

L'écran **Restitutions**, ainsi que l'écran d'**encours** d'une banque (nouveau, 23/07/2026),
disposent chacun d'un bouton **Exporter PDF** — aucun export n'existait sur ce module avant cette
date. Le document généré reprend l'intégralité des résultats correspondant aux filtres actuellement
appliqués à l'écran (banque, libellé, statut pour la liste ; libellé, montant pour l'encours — la
banque et le statut PREVU sont implicites), pas seulement la page affichée, ainsi que le montant
total cumulé.

---

### 6. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / créer / modifier une restitution | Équipe Finance (membres et directeur), Administrateur système |
| Basculer le statut / supprimer une restitution | Administrateur système uniquement |
| Exporter la liste ou l'encours en PDF | Équipe Finance (membres et directeur), Administrateur système |

---

### 7. Audit

Toute création, modification ou suppression d'une restitution est historisée : auteur, date et
valeurs avant/après restent consultables ultérieurement.

---

### 8. Logs

La création, la modification, la suppression, le changement de statut et la validation
(rapprochement) d'une restitution génèrent chacun un événement journalisé, utile au diagnostic.

---

### 9. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Comptes bancaires** | Chaque restitution est rattachée à un compte bancaire. |
| **Rapprochement** | La validation d'une restitution crée une transaction (encaissement) rattachée à un rapprochement. |

---

Qu'elles soient normales ou anomales, toutes ces transactions doivent être confrontées chaque mois au relevé bancaire réel avant d'être considérées comme définitivement arrêtées. C'est l'objet du rapprochement bancaire mensuel.

# Rapprochement

---

<a id="chap30"></a>

## Chapitre 30 — Module Rapprochement


> Source de vérité métier pour le module Rapprochement.

---

### 1. Périmètre du module

Un rapprochement représente une période mensuelle de rapprochement bancaire pour une banque
donnée : il regroupe l'ensemble des mouvements (transactions) survenus sur cette banque durant le
mois, et permet de vérifier que le solde calculé correspond au relevé bancaire réel avant de
clôturer la période. Ce module ne saisit aucune opération directement — il agrège les transactions
produites par tous les autres modules (leasing, moyens de paiement, agios, commissions, etc.) et
pilote le passage d'un mois à l'autre.

---

### 2. Définitions métier

**Rapprochement** : période mensuelle (une par mois et par banque) durant laquelle les transactions
de cette banque sont regroupées. Porte un solde de début, un statut, et permet de dériver un solde
de fin et des totaux (débit, crédit, TVA) à partir des transactions qui lui sont rattachées.

**Statut du rapprochement** :
- **En cours** — période active, les transactions continuent d'y être rattachées.
- **Clôture provisoire** — période figée en vue d'un contrôle avant verrouillage définitif ; reste
  modifiable (une transaction peut encore y être ajoutée, corrigée ou annulée).
- **Clôturé définitivement** — période verrouillée de façon permanente ; plus aucune transaction
  rattachée ne peut être ajoutée, modifiée ou annulée.

**Solde de début** : solde bancaire au premier jour de la période. Pour le tout premier
rapprochement d'une banque, il correspond au solde initial saisi à la création de la banque. Pour
tous les suivants, il correspond au solde de fin calculé de la période précédente.

**Solde de fin** : jamais stocké — toujours recalculé à la demande à partir du solde de début et des
totaux débit/crédit des transactions de la période (solde de fin = solde de début + total crédit −
total débit).

**TVA due** : différence entre la TVA collectée et la TVA déductible des transactions de la
période, calculée à la demande, jamais stockée.

**Rapprochements actifs** : à tout instant, une banque possède exactement un rapprochement
En cours, et au maximum un rapprochement en Clôture provisoire (les deux peuvent coexister le temps
du contrôle avant verrouillage définitif). Aucun autre statut ne peut coexister avec ces deux-là.

---

### 3. Cycle de vie complet

```
[1] Création de la banque
        ↓
    → Premier rapprochement créé automatiquement (statut En cours),
      solde de début = solde initial saisi à la création de la banque
        ↓
[2] En cours
    → Les transactions des autres modules s'y rattachent au fil de l'eau
        ↓
[3] Clôture provisoire (déclenchée manuellement)
    → Le solde de fin est calculé et figé
    → Le mois suivant est ouvert automatiquement (nouveau rapprochement En cours),
      avec pour solde de début le solde de fin qui vient d'être calculé
    → Toute correction encore apportée à une transaction de la période provisoire
      recalcule le solde de fin et le répercute automatiquement comme solde de
      début du mois suivant (tant que celui-ci n'a pas lui-même été modifié)
        ↓
[4] Clôture définitive (déclenchée manuellement, depuis une clôture provisoire)
    → Verrouillage permanent — aucune transaction de la période ne peut plus
      être ajoutée, modifiée ou annulée
```

---

### 4. Fonctionnement de l'interface

#### Vues disponibles

| Écran | Description |
|---|---|
| Historique des rapprochements | Liste paginée et filtrable (banque, période, statut) de tous les rapprochements |
| Détail d'un rapprochement | Soldes, totaux, TVA, et liste paginée et filtrable des transactions de la période |
| Sélection du rapprochement actif | Snapshot du rapprochement En cours et, s'il existe, de celui en Clôture provisoire, pour une banque donnée |

#### Actions disponibles

- **Consulter** l'historique des rapprochements et le détail de chacun.
- **Clôturer provisoirement** le rapprochement En cours d'une banque — ouvre automatiquement le
  mois suivant.
- **Clôturer définitivement** un rapprochement en Clôture provisoire — verrouillage permanent, sans
  action de retour possible.
- **Télécharger le rapport PDF** d'un rapprochement, en deux versions : *Finance* (inclut la TVA et
  la date d'émission de chaque transaction) ou *Comptabilité* (sans TVA ni date d'émission).
  Déclenchable depuis l'écran historique (rapport complet, toutes les transactions de la période)
  ou depuis l'écran détail (nouveau, 23/07/2026 — rapport limité aux transactions correspondant aux
  filtres actuellement appliqués sur cet écran : libellé, observation, montant, source, dates
  d'opération). Dans les deux cas, les soldes et totaux affichés en en-tête du rapport restent ceux
  de la période entière, non filtrés.

---

### 5. Règles métier

1. Une banque possède à tout instant exactement un rapprochement En cours.
2. Une banque ne peut avoir plus d'un rapprochement en Clôture provisoire à la fois — la clôture
   provisoire est refusée si une clôture provisoire existe déjà pour cette banque.
3. La clôture définitive n'est possible que depuis le statut Clôture provisoire — elle est refusée
   sur un rapprochement En cours ou déjà clôturé définitivement.
4. Le solde de fin et la TVA due ne sont jamais stockés : toujours recalculés à partir des
   transactions rattachées au moment de la consultation ou de la clôture.
5. Toute modification ou annulation d'une transaction rattachée à un rapprochement en Clôture
   provisoire recalcule automatiquement le solde de fin de cette période et le répercute comme
   solde de début du rapprochement En cours suivant.
6. Aucune transaction ne peut être ajoutée, modifiée ou annulée sur un rapprochement Clôturé
   définitivement — cette règle protège l'intégrité d'une période arrêtée, quel que soit le rôle de
   l'utilisateur qui tente l'opération (voir `docs/modules/transactions.md`, règle 6).

---

### 6. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|---|:---:|:---:|:---:|
| Consulter l'historique et le détail d'un rapprochement | ✅ | ✅ | ✅ |
| Clôturer provisoirement | ✅ | ✅ | ✅ |
| Clôturer définitivement | ✅ | ✅ | ✅ |
| Télécharger le rapport PDF | ✅ | ✅ | ✅ |

La clôture définitive, bien qu'irréversible, n'est pas réservée à l'administrateur système : le
garde-fou métier réel est l'enchaînement strict des statuts (règles 2 et 3 ci-dessus), pas une
restriction de rôle supplémentaire — même principe que l'annulation de transaction (voir
`docs/modules/transactions.md`).

---

### 7. Audit

La création du premier rapprochement d'une banque, la clôture provisoire et la clôture définitive
sont toutes historisées avec l'auteur et la date, et restent consultables ultérieurement. La
consultation seule (historique, détail, téléchargement du rapport) n'est pas historisée.

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :

- recalcul et répercussion du solde entre un rapprochement en Clôture provisoire et le rapprochement
  En cours suivant, à la suite d'une correction de transaction.

Les tentatives d'opérations invalides sont également journalisées comme anomalies gérées,
notamment une clôture provisoire demandée alors qu'une clôture provisoire existe déjà pour la
banque, ou une clôture définitive demandée sur un rapprochement qui n'est pas en Clôture
provisoire.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Transactions** | Chaque rapprochement contient les transactions qui lui sont rattachées ; les totaux (débit, crédit, TVA) et le solde de fin sont calculés à partir d'elles. |
| **Comptes bancaires** | Chaque rapprochement appartient à une banque ; le premier rapprochement d'une banque est créé automatiquement à sa création, avec le solde initial saisi comme solde de début. |
| **Tous les modules produisant des opérations bancaires** (Leasing, CMT, moyens de paiement, Agios, Commission, Caution, Impôts et Taxes, Préfinancement, Factoring, Droits Constatés, Décomptes, Frais bancaires, Restitution, Opérations erronées, Impayées) | Chacun rattache la transaction qu'il produit au rapprochement En cours (ou à celui explicitement désigné) de la banque concernée. |

---

Le rapprochement organise les transactions déjà validées ; mais avant validation, l'équipe Finance a besoin d'un espace de travail unique pour consulter et traiter, banque par banque, tout ce qui reste encore en attente à travers l'ensemble des modules. C'est le rôle des États des encours et de son sous-module Récapitulatif.

# États des encours / Récapitulatif

---

<a id="chap31"></a>

## Chapitre 31 — Module États des encours et Récapitulatif


> Source de vérité métier pour le module États des encours (et son sous-module Récapitulatif).
> Ce module ne redéfinit aucune règle métier propre à un type d'opération — chacun reste régi par
> son propre module (voir section 9, "Dépendances fonctionnelles", et `docs/modules/<module>.md`).

---

### 1. Périmètre du module

États des encours est l'espace de travail consolidé qui regroupe, pour une banque et une période de
rapprochement données, l'ensemble des opérations bancaires encore en attente de validation —
échéances de crédit, moyens de paiement, préfinancement, factoring, droits constatés, décomptes,
commissions, agios, prélèvements, frais bancaires, impôts et taxes, restitutions, opérations
erronées, impayées. Il offre un point d'entrée unique pour consulter ces opérations et les valider,
individuellement ou en lot.

Le sous-module Récapitulatif fait partie intégrante de cet espace : il consolide ces mêmes
opérations en attente dans une vue transversale (tous types confondus) et dans des rapports PDF —
un récapitulatif simple (liste des opérations) et un récapitulatif général détaillé (situation du
jour et projection de fin de mois, regroupées par catégorie métier).

---

### 2. Définitions métier

**Encours** : une opération bancaire (d'un des types listés en section 1) dont le statut est encore
« à payer »/« en cours » — `PREVU` pour les échéances et opérations calculées, `ENCOURS` pour les
moyens de paiement — et dont la date de référence (date d'émission pour la plupart des types, date
d'échéance pour les LCN en décaissement) est antérieure ou égale à la fin du mois en cours. Une fois
validé, un encours quitte les écrans « Encours » et devient une transaction rattachée au
rapprochement.

**Sens** : chaque encours est soit un **Décaissement** (sortie d'argent), soit un **Encaissement**
(entrée d'argent).

**Validation d'un encours** : action qui transforme un encours en transaction bancaire, en lui
associant une date de valeur, une date d'opération et, selon le type d'opération, une TVA
déductible ou collectée. C'est cette action qui fait apparaître l'opération dans le rapprochement de
la banque (voir `docs/modules/rapprochement.md`).

**Validation en lot** : variante permettant de valider en une seule fois plusieurs encours du même
type (et du même sens le cas échéant), avec une date de valeur, une date d'opération et un taux de
TVA communs à tout le lot. Le libellé, l'observation et la date d'émission restent propres à chaque
opération et ne sont jamais saisis en lot — chaque opération conserve automatiquement les siens.
Traitement tout-ou-rien : si une opération du lot échoue, aucune n'est validée.

**Récapitulatif** *(écran unique depuis le 06/09/2026 — remplace les deux écrans Récapitulatif
Décaissements et Récapitulatif Encaissements)* : vue consolidée, pour une banque, de tous les
encours prévisionnels du mois, toutes catégories confondues et **les deux sens réunis** dans un
seul tableau paginé, triable et filtrable. Chaque ligne porte une pastille indiquant son sens, et
les deux totaux — Débit prévisionnel et Crédit prévisionnel — sont affichés côte à côte en en-tête
du tableau ; lorsqu'un sens est filtré, le total de l'autre vaut zéro.

Le **sens** est devenu un critère de recherche optionnel (« Tous » par défaut) au lieu d'être fixé
par l'écran choisi. Il se combine à tous les autres : catégorie d'origine, libellé, observation,
numéro de pièce *(nouveau, 07/09/2026)*, montant exact *(nouveau, 06/09/2026)*, et date de
référence « depuis » *(nouveau, 06/09/2026)* / « jusqu'à » *(31/08/2026)*. La barre de filtres est
repliable et résume en pastilles les critères actifs lorsqu'elle est fermée, afin qu'un filtre en
vigueur ne passe jamais inaperçu — les totaux affichés suivent toujours les filtres.

Le filtre date de référence compare chaque opération à sa propre colonne de référence métier — date
d'échéance pour le leasing, le CMT et les LCN (sauf escompte) ; date d'émission pour toutes les
autres sources — sans que l'utilisateur ait à savoir laquelle s'applique ; si aucune borne haute
n'est choisie, elle reste la fin du mois en cours (comportement historique). L'export PDF reprend
exactement les lignes filtrées, filtre de sens compris.

**Récapitulatif global** : version non paginée du même contenu (Décaissements et Encaissements
réunis), utilisée pour la génération du rapport PDF. Depuis le tableau de bord Finance, l'utilisateur
choisit la période avant le téléchargement : **à ce jour** (n'inclut que les encours déjà échus/émis)
ou **jusqu'à fin de mois** (comportement historique, par défaut) *(nouveau, 31/08/2026)* — les
libellés du rapport (« Solde... », « Disponibilité FC... ») s'adaptent au choix effectué. Dans les
deux cas, aucune borne basse n'est appliquée : les encours en retard non encore validés, antérieurs
au mois en cours, sont toujours inclus.

**Disponibilité FC fin du mois** (rapport Récapitulatif global, anciennement « Reste fin du mois ») :
Solde projeté de fin de mois + Plafond FC. Positive : marge résiduelle sous la facilité de caisse à
fin de mois ; négative : dépassement projeté. Cohérente avec la « Disponibilité » du jour J affichée
dans le Récapitulatif général détaillé *(formule corrigée le 31/08/2026 — une inversion de signe la
rendait auparavant opposée à son intitulé)*.

**Récapitulatif général détaillé** : rapport distinct combinant, pour une banque (ou pour toutes les
banques, colonne par colonne) : la situation du jour (solde actuel, plafond de facilité de caisse,
disponibilité, encours arrivant à échéance jusqu'à aujourd'hui inclus, solde projeté) et les
décaissements/encaissements attendus du reste du mois (du lendemain à la fin du mois), regroupés par
catégorie métier — par exemple la catégorie « Règlement Frns » regroupe les Chèques, LCN, Virements
et Mises à disposition d'un même sens. Dans la version consolidée (toutes banques), une catégorie
n'apparaît dans le tableau que si au moins une banque y porte un montant non nul — une catégorie à
0,00 pour toutes les banques est omise du rapport *(nouveau, 31/08/2026)*.

---

### 3. Parcours utilisateur

```
[1] Tableau de bord Finance
        ↓ clic sur la carte d'une banque
[2] Écran de sélection du rapprochement actif
        → une carte « En cours » toujours présente
        → une carte « Clôture provisoire » si une période est en cours de contrôle
        ↓ choix de la période à consulter
[3] Espace de travail « États des encours » (pour la banque et la période choisies)
        → onglet « Rapprochement » (ouvert par défaut) : transactions déjà validées de la période
        → onglets « Récapitulatifs » : vue consolidée Décaissements / Encaissements
        → un écran par type d'opération, sous « Encaissements » ou « Décaissements »
```

---

### 4. Fonctionnement de l'interface

#### Écran de sélection du rapprochement actif

Affiche, pour la banque choisie, une carte pour la période **En cours** (toujours présente) et, si
elle existe, une carte pour la période en **Clôture provisoire** — chacune avec son solde de début,
son solde de fin, ses totaux débit/crédit et son nombre d'opérations. Le choix d'une carte ouvre
l'espace de travail sur cette période.

#### Espace de travail

Se compose de :
- un **en-tête contextuel** rappelant la banque, le statut de la période (En cours / Clôture
  provisoire), le solde de début, le solde de fin (ou actuel), la variation et la facilité de
  caisse. Les totaux débit/crédit/TVA n'y figurent plus *(07/09/2026)* : ils étaient calculés sur
  les transactions **réalisées** du rapprochement et voisinaient, sur l'écran Récapitulatifs, des
  totaux **prévisionnels** de nature différente, sans que rien ne distingue les deux. Chaque écran
  affiche désormais ses propres totaux au-dessus de son tableau ;
- une **pastille de nature** *(nouveau, 07/09/2026)* à côté du fil d'Ariane, indiquant si l'écran
  affiché présente du **Prévisionnel** (opérations attendues, pas encore passées en banque) ou du
  **Réalisé** (opérations déjà passées sur le compte). Un seul écran du module est de nature
  réalisée, le Rapprochement ; tous les autres sont des encours ;
- un **bouton de clôture** (Clôture provisoire ou Clôture définitive selon le statut actuel de la
  période affichée) ;
- une **barre latérale** organisée en catégories : Rapprochement, Récapitulatifs (écran unique
  depuis le 06/09/2026, les deux sens réunis), Encaissements (un écran par type d'opération),
  Décaissements (un écran par type d'opération).

#### Onglet Rapprochement

Liste paginée et filtrable (libellé, source, observation, montant, dates d'opération) des
transactions déjà validées et rattachées à la période affichée. Depuis cet écran, une transaction
peut être corrigée (date de valeur, date d'opération, TVA déductible, TVA collectée) ou annulée —
voir `docs/modules/transactions.md` et `docs/modules/rapprochement.md` pour le détail de ces règles.
Un bouton **Exporter PDF** (nouveau, 23/07/2026) génère la liste complète des transactions
correspondant aux filtres actuellement appliqués, sans limite de pagination.

#### Écrans « Encours » par type d'opération

Chacun des types d'opération — une vingtaine depuis l'ajout des provisions — dispose de son propre
écran (par sens quand les deux existent),
listant les encours de ce type pour la banque et la période sélectionnées. Depuis chaque écran, un
ou plusieurs encours peuvent être validés — individuellement (formulaire propre à chaque type,
pouvant demander des champs additionnels) ou en lot (formulaire commun décrit en section 2). Le
détail des champs et des règles de validation propres à chaque type d'opération est documenté dans
le module correspondant (section 9), y compris la disponibilité de l'**export PDF** propre à chacun
de ces écrans — tous les types d'opération en disposent désormais (23/07/2026).

#### Onglets Récapitulatifs Décaissements / Encaissements

Tableau consolidé, tous types d'opération confondus, des encours d'un même sens pour la banque
sélectionnée. Paginé et triable (date d'émission, date d'échéance affichée, montant), filtrable par
catégorie d'origine, libellé, observation, et date de référence « jusqu'à » (nouveau, 31/08/2026) —
ce dernier filtre compare chaque ligne à sa propre colonne de référence métier (émission, échéance,
etc. selon la catégorie) et retombe sur la fin du mois en cours si aucune date n'est choisie. Le
total du sens correspondant est affiché en en-tête, calculé sur l'ensemble des lignes filtrées (pas
seulement la page affichée). Écran de consultation uniquement — aucune validation ne s'y effectue
directement. Un bouton **Exporter PDF** (nouveau, 23/07/2026) génère la liste complète correspondant
aux filtres actuellement appliqués.

#### Téléchargements PDF (depuis le tableau de bord Finance)

- **Récap global** (bouton d'en-tête) : rapport consolidé de trésorerie pour l'ensemble des banques.
  Un choix de période est demandé avant le téléchargement — **à ce jour** ou **jusqu'à fin de mois**
  (nouveau, 31/08/2026) — voir la définition « Récapitulatif global » en section 2.
- **Récap détaillé** (bouton d'en-tête) : récapitulatif général détaillé pour l'ensemble des
  banques. Les catégories à 0,00 pour toutes les banques n'y figurent pas (nouveau, 31/08/2026).
- **Récap détaillé** (bouton sur la carte d'une banque) : récapitulatif général détaillé pour cette
  seule banque.

---

### 5. Règles métier

1. Un encours est éligible à validation lorsque sa date de référence est antérieure ou égale à la
   fin du mois en cours. Pour la situation du jour affichée sur le tableau de bord (résumé, encours
   à échéance), la date de référence retenue est aujourd'hui, pas la fin du mois.
2. Un encours est validé dans le rapprochement de la période affichée à l'écran (En cours ou Clôture
   provisoire selon la période sélectionnée à l'étape 2 du parcours) — jamais sur un rapprochement
   Clôturé définitivement, ce statut n'apparaissant plus dans l'écran de sélection.
3. Une fois validé, un encours n'apparaît plus dans les écrans « Encours » ni dans les
   Récapitulatifs — il devient consultable comme transaction dans l'onglet Rapprochement.
4. La validation en lot applique la même date de valeur, la même date d'opération et le même taux
   de TVA à toutes les opérations sélectionnées ; elle est tout-ou-rien. Le libellé, l'observation et
   la date d'émission restent ceux d'origine de chaque opération.
5. L'écran Récapitulatifs (en ligne comme en PDF) est un écran de consultation — aucune validation
   ne s'y effectue ; elle se fait depuis l'écran dédié au type d'opération concerné.
6. Chaque type d'opération conserve ses propres règles de validation (champs additionnels requis,
   contraintes de statut) — voir le module correspondant pour le détail (section 9).

---

### 6. Droits d'accès

| Action | Admin système | Directeur Finance | Membre Finance |
|---|:---:|:---:|:---:|
| Consulter l'écran de sélection du rapprochement actif | ✅ | ✅ | ✅ |
| Consulter les écrans Encours et l'onglet Rapprochement | ✅ | ✅ | ✅ |
| Valider un encours (individuel ou en lot) | ✅ | ✅ | ✅ |
| Consulter les Récapitulatifs Décaissements/Encaissements | ✅ | ✅ | ✅ |
| Télécharger un récapitulatif ou un récapitulatif général détaillé (PDF) | ✅ | ✅ | ✅ |

Aucune action de ce module n'est réservée à un rôle en particulier au-delà de l'appartenance au
département Finance (ou `SYSTEM_ADMIN`, qui a un accès transversal à tous les départements).

---

### 7. Audit

La validation d'un encours (individuelle ou en lot) et l'annulation d'une transaction depuis
l'onglet Rapprochement sont historisées avec l'auteur et la date — voir le module source de chaque
type d'opération pour le détail de ce qui est tracé (`docs/modules/<module>.md`). La consultation
seule (écrans Encours, Récapitulatifs, téléchargement PDF) n'est pas historisée.

---

### 8. Logs

Les traitements suivants produisent un événement utile au diagnostic :
- une validation en lot dont au moins une opération échoue (aucune n'est alors validée) ;
- une tentative de validation ou d'annulation refusée parce que le rapprochement concerné n'est plus
  dans un statut qui l'autorise.

---

### 9. Dépendances fonctionnelles

| Module | Nature de la dépendance |
|---|---|
| **Rapprochement** | Chaque validation d'encours crée une transaction rattachée à la période affichée ; l'onglet Rapprochement de cet espace de travail est la même donnée que le détail d'un rapprochement. |
| **Transactions** | Les transactions issues d'une validation sont consultables et corrigibles selon les règles de ce module. |
| **Comptes bancaires** | Point d'entrée de l'espace de travail (carte d'une banque sur le tableau de bord) ; solde et facilité de caisse affichés dans l'en-tête contextuel et dans le récapitulatif général détaillé. |
| **Validation** (module transverse, voir `docs/rbac/validation.md`) | Orchestre, pour chaque type d'opération, la création de la transaction correspondante lors d'une validation individuelle ou en lot. |
| **Leasing, CMT, Chèque, LCN, Virement, Virement international, Mise à disposition, Prélèvement, Préfinancement, Factoring, Droits constatés, Décomptes, Commission, Agios, Frais bancaires, Impôts et taxes, Restitution, Opérations erronées, Impayées** | Chacun produit les encours affichés dans son écran dédié, et porte les règles de validation propres à son type d'opération. |

---

Enfin, deux modules transversaux ne portent aucune donnée métier propre mais servent l'ensemble de l'application : la traçabilité des opérations sensibles, et la diffusion d'informations et d'alertes à tous les utilisateurs.

# Transverse

---

<a id="chap32"></a>

## Chapitre 32 — Module Logs


> Source de vérité métier pour le module Logs (journal d'audit applicatif).

---

### 1. Périmètre du module

Ce module donne accès, en lecture, à l'historique de toutes les opérations sensibles (création,
modification, suppression) réalisées sur les données métier de G-Banque, tous modules confondus. Il
ne modifie aucune donnée métier lui-même : il consulte l'historique déjà constitué par chaque module
lors de ses propres opérations.

---

### 2. Définitions métier

**Journal d'audit** : ensemble des événements historisés, un événement par opération sensible
réalisée sur une donnée métier (qui, quoi, quand, sur quelle entité).

**Entrée de journal** : un événement individuel — date et heure, utilisateur à l'origine de
l'opération, type d'opération (création, modification, suppression), notion métier concernée et son
identifiant.

**Détail champ-par-champ** : pour une entrée de journal donnée, la liste des champs modifiés avec leur
valeur avant et après, disponible à la demande.

---

### 3. Règles métier

1. Chaque opération sensible (création, modification, suppression) réalisée sur une donnée métier
   auditée, dans n'importe quel module de G-Banque, produit une entrée de journal.
2. L'utilisateur enregistré sur une entrée est celui qui a déclenché l'opération ; pour les
   traitements automatiques (batch), c'est le traitement lui-même qui est enregistré comme auteur.
3. Une entrée de journal peut être recherchée par utilisateur, type d'opération, notion métier
   concernée et période.
4. Le détail champ-par-champ (valeurs avant/après) d'une entrée est consultable à la demande, sans
   être chargé systématiquement avec la liste.
5. Seuls les champs réellement modifiés apparaissent dans le détail. Un champ qui n'existait pas
   avant l'opération (création) ou qui n'existe plus après (suppression) est présenté sans valeur.
6. Lorsque le champ modifié désigne une autre notion métier (par exemple la banque ou le marché
   rattaché à une opération), le détail affiche l'identifiant de la notion désignée suivi de son
   libellé — par exemple « 7 — BMCE ». L'identifiant est toujours affiché : c'est la seule valeur
   qui reste exacte si la notion a été renommée ou supprimée depuis. Une notion qui ne porte aucun
   libellé textuel (le rapprochement, par exemple) n'est identifiée que par son identifiant.
7. Les listes rattachées à une notion (par exemple l'ensemble des transactions d'un compte)
   n'apparaissent pas dans le détail : leur modification est journalisée sur la notion qui porte
   effectivement le changement.
8. Une entrée de journal peut être supprimée manuellement. Cette suppression n'est, à ce jour, pas
   elle-même historisée dans le journal.

---

### 4. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter le journal d'audit (liste, détail, noms d'entités) | Administrateur système uniquement |
| Supprimer une entrée de journal | Administrateur système uniquement |

---

### 5. Audit

Ce module est le point de consultation de l'audit des autres modules — il n'audite pas ses propres
opérations. La suppression d'une entrée de journal n'est donc pas elle-même tracée.

---

### 6. Logs

Ce module ne produit pas d'événement journalisé technique propre au-delà de ce qu'il consulte.

---

### 7. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Tous les modules métier** | Chaque création, modification ou suppression auditée dans un module alimente le journal consulté ici. |
| **Utilisateurs** | Les noms d'utilisateurs affichés dans le journal proviennent du module Utilisateurs. |

---

<a id="chap33"></a>

## Chapitre 33 — Module Notifications


> Source de vérité métier pour le module Notifications.

---

### 1. Périmètre du module

Ce module diffuse des notifications applicatives à l'ensemble des utilisateurs de G-Banque :
informations générales déclenchées manuellement par un administrateur, et alertes automatiques
d'échéance à venir générées par les traitements automatiques des modules Leasing, Crédit Moyen Terme
et Lignes Fonctionnelles.

---

### 2. Définitions métier

**Notification** : message diffusé à tous les utilisateurs, composé d'un titre, d'un contenu et d'un
type (Information, Avertissement, Alerte, Succès), horodaté à sa création.

**Statut lu/non-lu** : propre à chaque utilisateur — une même notification peut être lue par un
utilisateur et non lue par un autre.

**Type** : nature de la notification, l'un des suivants :
- **INFO** : information générale.
- **AVERTISSEMENT** : signal à surveiller (ex. échéance approchant, planifier une action).
- **ALERTE** : action requise rapidement (ex. échéance imminente).
- **SUCCES** : confirmation d'un traitement réussi.

---

### 3. Règles métier

1. Une notification est visible par tous les utilisateurs dès sa création.
2. Le statut lu/non-lu est individuel : marquer une notification comme lue ne l'affecte que pour
   l'utilisateur qui l'a fait.
3. Une notification peut être créée manuellement par un administrateur système (diffusion
   d'information générale), ou automatiquement par les traitements hebdomadaires de suivi des
   échéances (Leasing, Crédit Moyen Terme, Lignes Fonctionnelles générales et spécifiques) lorsqu'une
   échéance approche des seuils d'alerte configurés.
4. Le nombre de notifications non lues est affiché en continu dans la barre de navigation.

---

### 4. Droits d'accès

| Action | Qui peut faire |
|--------|----------------|
| Consulter / marquer comme lues ses propres notifications | Tout utilisateur authentifié |
| Déclencher manuellement une notification publique | Administrateur système uniquement |

---

### 5. Audit

Toute notification diffusée (manuelle ou automatique) est historisée : auteur (ou traitement
automatique à l'origine), date et contenu restent consultables ultérieurement. Le statut lu/non-lu
propre à chaque utilisateur est également historisé depuis le 16/07/2026 (auteur, date, valeur
avant/après) — cette information n'étant dérivable d'aucune autre donnée, sa perte serait
irrécupérable, contrairement à une donnée recalculée.

---

### 6. Logs

La diffusion d'une notification (manuelle ou automatique) génère un événement journalisé, utile au
diagnostic.

---

### 7. Dépendances fonctionnelles entre modules

| Module | Nature de la dépendance |
|--------|------------------------|
| **Leasing** | Le traitement hebdomadaire de suivi des échéances de main levée déclenche des notifications automatiques. |
| **Crédit Moyen Terme (CMT)** | Idem, pour les échéances de main levée CMT. |
| **Lignes Fonctionnelles** | Le traitement hebdomadaire de suivi des expirations (générales et spécifiques) déclenche des notifications automatiques, selon les seuils configurés (`docs/guide-utilisateur/admin.md`, section "Configuration Lignes Fonctionnelles"). |
| **Utilisateurs** | Chaque notification a un statut lu/non-lu propre à chaque utilisateur. |

---
