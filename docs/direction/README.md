# G-Banque — Synthèse direction

Ce document est la synthèse direction complète de G-Banque, le système de gestion et de suivi des
opérations bancaires internes de Staport. Il consolide en un seul récit les notes de valeur de
chaque module de l'application, organisées pour montrer comment G-Banque construit, module après
module, une maîtrise complète des opérations bancaires de l'entreprise — des accès qui la
protègent jusqu'aux rapports qui l'éclairent.

---

## Sommaire

**Socle d'accès**
- [Chapitre 1 — Authentification](#chapitre-1--authentification)
- [Chapitre 2 — Gestion des départements](#chapitre-2--gestion-des-départements)
- [Chapitre 3 — Gestion des utilisateurs](#chapitre-3--gestion-des-utilisateurs)

**Comptes bancaires**
- [Chapitre 4 — Comptes bancaires](#chapitre-4--comptes-bancaires)

**Produits financiers rattachés au compte**
- [Chapitre 5 — Crédit Moyen Terme (CMT)](#chapitre-5--crédit-moyen-terme-cmt)
- [Chapitre 6 — Lignes Fonctionnelles](#chapitre-6--lignes-fonctionnelles)
- [Chapitre 7 — Leasing](#chapitre-7--leasing)

**Marchés et satellites**
- [Chapitre 8 — Marchés](#chapitre-8--marchés)
- [Chapitre 9 — Caution](#chapitre-9--caution)
- [Chapitre 10 — Décomptes](#chapitre-10--décomptes)
- [Chapitre 11 — Préfinancements](#chapitre-11--préfinancements)
- [Chapitre 12 — Factoring](#chapitre-12--factoring)
- [Chapitre 13 — Droits Constatés](#chapitre-13--droits-constatés)

**Moyens de paiement**
- [Chapitre 14 — Chèques](#chapitre-14--chèques)
- [Chapitre 15 — LCN](#chapitre-15--lcn)
- [Chapitre 16 — Virements](#chapitre-16--virements)
- [Chapitre 17 — Virements Internationaux](#chapitre-17--virements-internationaux)
- [Chapitre 18 — Mises à Disposition](#chapitre-18--mises-à-disposition)
- [Chapitre 19 — Recap Encaissement / Recap Décaissement](#chapitre-19--recap-encaissement--recap-décaissement)
- [Chapitre 20 — Prélèvements](#chapitre-20--prélèvements)

**Transactions**
- [Chapitre 21 — Transactions](#chapitre-21--transactions)

**Charges appliquées**
- [Chapitre 22 — Frais bancaires](#chapitre-22--frais-bancaires)
- [Chapitre 23 — Agios bancaires](#chapitre-23--agios-bancaires)
- [Chapitre 24 — Impôts et Taxes](#chapitre-24--impôts-et-taxes)
- [Chapitre 25 — Commissions bancaires](#chapitre-25--commissions-bancaires)

**Anomalies**
- [Chapitre 26 — Impayées](#chapitre-26--impayées)
- [Chapitre 27 — Opérations Erronées](#chapitre-27--opérations-erronées)
- [Chapitre 28 — Provision](#chapitre-28--provision)
- [Chapitre 29 — Restitution](#chapitre-29--restitution)

**Rapprochement**
- [Chapitre 30 — Rapprochement](#chapitre-30--rapprochement)

**États des encours / Récapitulatif**
- [Chapitre 31 — États des encours et Récapitulatif](#chapitre-31--états-des-encours-et-récapitulatif)

**Transverse**
- [Chapitre 32 — Logs](#chapitre-32--logs)
- [Chapitre 33 — Notifications](#chapitre-33--notifications)

---

## Chapitre 1 — Authentification

### Ce que ce module résout

G-Banque centralise et protège l'accès à l'ensemble des données financières de Staport derrière un
point d'entrée unique et sécurisé. Chaque collaborateur est identifié, chaque accès est contrôlé
selon son profil, et chaque action réalisée dans l'application est tracée — un socle de confiance
indispensable pour un système qui pilote les comptes bancaires, les crédits et les paiements de
l'entreprise.

### Ce que le système apporte

- **Contrôle d'accès automatique par profil** : chaque collaborateur ne voit que ce que son profil
  autorise  . Périmètre protégé : l'intégralité de
  G-Banque, aucune page accessible sans authentification.
- **Révocation effective** : un accès peut être désactivé en quelques secondes depuis l'interface
  (départ, changement de poste) ; la session encore ouverte du collaborateur se ferme d'elle-même
  dans les 15 minutes, et un changement de droits s'applique dans le même délai, sans reconnexion.
- **Sessions protégées contre le vol** : les titres de connexion sont inaccessibles aux pages web
  (une faille ou un script malveillant ne peut pas les dérober), renouvelés toutes les 15 minutes,
  et toute tentative de réutilisation d'un titre copié ferme immédiatement la session. Les sessions
  sont limitées à une journée de travail (8 heures) et la déconnexion les ferme réellement côté
  serveur — un niveau de protection aligné sur les recommandations de sécurité (OWASP) pour une
  application financière.
- **Traçabilité complète** : chaque action réalisée dans G-Banque est associée à un utilisateur
  identifié, consultable à tout moment — élimine l'impossibilité de savoir qui a modifié une donnée
  en cas de litige.
- **Profils et droits** : administrateur système (transversal) et profils métier organisés par
  département, avec deux rôles cumulables par département (Membre, Directeur).

## Chapitre 2 — Gestion des départements

### Ce que ce module résout

La structure organisationnelle de Staport évolue avec le temps : nouvelles fonctions,
réorganisations, création de pôles. G-Banque rend cette structure entièrement pilotable depuis
l'interface d'administration, sans aucune intervention technique — les droits d'accès de chaque
collaborateur suivent automatiquement l'organisation réelle de l'entreprise.

### Ce que le système apporte

- **Création en autonomie** : ajouter un département  prend moins d'une minute depuis l'interface.
- **Droits automatiquement liés à l'organisation** : dès qu'un collaborateur est rattaché à un
  département, ses droits sont configurés selon son rôle (Membre  ou Directeur ) 
- **Aucune limite** : le nombre de départements n'est pas limité ; la structure organisationnelle
  reste entièrement modifiable depuis l'administration, réservée à l'administrateur système.

## Chapitre 3 — Gestion des utilisateurs

### Ce que ce module résout

G-Banque donne à l'administrateur système un contrôle complet et instantané sur les accès de
chaque collaborateur de Staport, directement depuis l'interface — création de compte, attribution
de droits, désactivation en cas de départ, sans passer par une intervention IT séparée.

### Ce que le système apporte

- **Contrôle total en temps réel** : modification des droits d'un collaborateur en quelques
  secondes ; désactivation immédiate et visible d'un compte au départ de l'entreprise — élimine
  tout accès conservé après un départ ou un changement de poste, sans dépendre de l'équipe IT.
- **Sécurité par défaut** : tout nouveau compte est créé sans aucun droit d'accès, à accorder
  explicitement — élimine le risque qu'un nouveau collaborateur accède par inadvertance à des
  données sensibles.
- **Traçabilité complète** : chaque création, modification, activation ou désactivation de compte
  est historisée avec auteur et horodatage — élimine l'impossibilité de savoir qui avait accès à
  quoi en cas de litige.
- **Couverture** : tous les collaborateurs ayant accès à G-Banque, gérés par l'administrateur
  système uniquement.

---

Une fois les accès sécurisés et la structure organisationnelle de Staport pilotable depuis
l'application, G-Banque peut s'appuyer sur cette base de confiance pour donner à l'équipe Finance
une vue fiable et consolidée de l'ensemble des comptes bancaires de l'entreprise.

---

## Chapitre 4 — Comptes bancaires

### Ce que ce module résout

G-Banque centralise l'ensemble des comptes bancaires de Staport en un seul endroit et offre à
l'équipe Finance une vue consolidée et en temps réel de leur situation, sans avoir à consulter
chaque banque séparément.

### Ce que le système apporte

- **Vue consolidée instantanée** : le tableau de bord Finance affiche en un seul écran la situation
  de tous les comptes bancaires actifs (solde actuel , plafond de facilité de caisse utilisé, effets et virements
  échus à venir).
- **Synchronisation FunPaiement en un clic** : la récupération des données depuis le système de
  paiement externe se déclenche depuis le tableau de bord, sans intervention technique — élimine
  la double saisie entre les deux systèmes.
- **Récapitulatif PDF** : téléchargeable à tout moment, global (toutes banques) ou par banque, sans
  préparation manuelle.
- **Traçabilité et garde-fous** : toute création ou modification de compte est auditée (auteur,
  horodatage) ; la suppression d'un compte est bloquée tant que son solde n'est pas nul ou que des
  transactions restent en attente — élimine la suppression accidentelle d'un compte encore actif.
- **Module central** : tous les autres modules se réfèrent aux comptes définis ici, garantissant une seule source de vérité pour
  l'état réel des comptes en cours de journée.

### Portefeuille bancaire actuel

G-Banque centralise aujourd'hui **7 comptes bancaires actifs**, répartis sur 7 établissements
différents, chacun rattaché à une agence identifiée et suivi en temps réel.

| Banque | Agence | Devise |
|---|---|---|
| AWB | Agence Guéliz | MAD |
| BMCE | Agence Liberté | MAD |
| BMCI | Agence Majorelle | MAD |
| CDM | Agence Yacoub El Mansour | MAD |
| SAHAM BANK | Agence Semlalia | MAD |
| BP | Agence Princesse | MAD |
| CIH | Agence Massira | MAD |

---

Les comptes bancaires étant centralisés, G-Banque peut ensuite suivre automatiquement les produits
financiers qui leur sont rattachés — crédits, lignes fonctionnelles et contrats de leasing — avec
le même niveau de fiabilité et de traçabilité.

---

## Chapitre 5 — Crédit Moyen Terme (CMT)

### Ce que ce module résout

G-Banque automatise l'intégralité du cycle de vie des crédits à moyen terme de Staport, de la
souscription à la main levée : plan de remboursement, suivi des paiements et alertes d'échéance
sont pris en charge automatiquement par le système.

### Ce que le système apporte

- **Plan de remboursement automatique** : dès la saisie d'un crédit (banque, nominal, taux,
  périodicité, durée, montant d'échéance), l'ensemble des échéances est généré automatiquement —
  aucun calcul manuel, aucune feuille Excel, aucun risque d'erreur de formule.
- **Suivi intégré au rapprochement** : chaque échéance est validée directement dans le
  rapprochement bancaire mensuel, transaction créée automatiquement — aucune double saisie, aucun
  oubli d'échéance.
- **Alerte main levée automatique** : chaque dimanche à 14h30, le système identifie les crédits
  entièrement soldés et notifie l'équipe — élimine le risque de crédit terminé non clôturé.
- **Vue encours par banque** : l'encours total de CMT restant dû est consultable en temps réel,
  par banque.
- **Recherche par période** : les crédits se filtrent par plage de date de début, et les échéances
  du tableau d'amortissement par plages de dates d'échéance, d'opération et de valeur — retrouver
  un crédit ou une échéance précise sans parcourir toute la liste.
- **Garde-fous automatiques** : modification ou suppression bloquée dès qu'une échéance est payée ;
  clôture bloquée tant qu'une échéance reste due — évite les erreurs de manipulation.
- **Traçabilité complète** : chaque modification de crédit (qui, quand, quoi) est historisée,
  consultable à tout moment en cas de litige.
- **Export PDF à la demande** : chaque liste de crédits et chaque tableau d'amortissement peut être
  exporté en PDF en un clic, prêt à être partagé ou archivé — plus besoin de ressaisir les données
  dans un autre outil pour produire un document présentable.

## Chapitre 6 — Lignes Fonctionnelles

### Ce que ce module résout

Les lignes fonctionnelles matérialisent les autorisations de financement accordées à Staport par
chaque banque . G-Banque centralise ce référentiel et automatise les contrôles de cohérence.

### Ce que le système apporte

- **Référentiel unique des natures** : G-Banque gère aujourd'hui **16 natures de lignes
  fonctionnelles** (Facilité de Caisse, Escompte Commercial, Cautions, Avances sur marchés nantis,
  CREDOC, AVAL, Refinancement, Préfinancement, Droit constaté, Factoring, Confirming, etc.), chacune
  définie une seule fois avec un code stable non modifiable — création et modification réservées à
  l'équipe Finance, suppression réservée à l'administrateur système et bloquée tant qu'une ligne
  existante l'utilise encore.
- **Ligne générale ou ligne spécifique, selon le besoin** : une ligne générale couvre l'activité
  d'une banque dans son ensemble, ou un ensemble de marchés ciblés ; une ligne spécifique est
  dédiée à un seul marché. Un marché ne peut jamais avoir simultanément une ligne générale et une
  ligne spécifique actives pour la même nature — élimine les autorisations en double ou
  contradictoires.
- **Suivi de la consommation en temps réel** : chaque ligne spécifique agrège automatiquement les
  opérations imputéeset affiche le débit cumulé, le
  crédit cumulé et le solde disponible — visibilité immédiate sur ce qui reste mobilisable, sans
  reconstitution manuelle.
- **Cautions structurées en sous-lignes** : une caution administrative peut être décomposée en
  sous-lignes qui héritent automatiquement des conditions de la ligne mère — pas de ressaisie, pas
  d'incohérence possible.
- **Surveillance automatique des échéances** : chaque semaine, les lignes (générales et
  spécifiques) approchant de leur fin ou déjà expirées déclenchent une alerte à l'équipe Finance,
  avec des seuils d'avertissement ajustables par un administrateur — plus besoin de suivi manuel
  dans un tableur.
- **Export PDF à la demande** : le référentiel des natures, ainsi que chaque liste de lignes
  générales et spécifiques (en cours ou archivées), peut être exporté en PDF en un clic, prêt à
  être partagé ou archivé — plus besoin de ressaisir les données dans un autre outil pour produire
  un document présentable.
- **Cycle couvert** : définition du référentiel → création et gestion des lignes → suivi des
  opérations imputées → surveillance des échéances → désactivation ou archivage.

### Natures de lignes fonctionnelles actuellement supportées

| Nature | Portée |
|---|---|
| Facilité de Caisse | Générale |
| Escompte Commercial | Générale |
| Cautions Provisoires | Générale et Spécifique |
| Cautions Définitives | Générale et Spécifique |
| Cautions Retenue de Garantie | Générale et Spécifique |
| Cautions Diverses | Générale et Spécifique |
| Avance sur Marchés Nantis | Générale et Spécifique |
| CREDOC | Générale |
| AVAL | Générale |
| Refinancement | Générale |
| Préfinancement | Générale et Spécifique |
| Droit constaté | Générale et Spécifique |
| Factoring | Spécifique |
| Caution restitution d'acompte | Générale et Spécifique |
| Confirming | Générale |
| Caution Administrative | Générale et Spécifique |

## Chapitre 7 — Leasing

### Ce que ce module résout

G-Banque automatise l'intégralité du cycle de vie des contrats de crédit-bail (leasing) de
Staport, de la signature à la main levée : calcul du tableau d'amortissement, suivi des paiements,
commissions et alertes d'échéance sont pris en charge automatiquement par le système.

### Ce que le système apporte

- **Tableau d'amortissement automatique** : dès la saisie d'un contrat (organisme, montant, durée,
  type de loyer), le système génère les mensualités et la valeur résiduelle finale — aucun calcul manuel. Trois structures supportées : loyer constant (linéaire), croissant
  (progressif), décroissant (dégressif), avec un tableau adapté automatiquement à chacune.
- **Suivi intégré au rapprochement** : chaque échéance est validée directement dans le
  rapprochement mensuel, transaction créée automatiquement — aucune double saisie.
- **Commissions automatiques** : pour chaque contrat, les commissions bancaires dues sur les loyers
  sont calculées et enregistrées automatiquement selon les règles configurées (voir module
  Commission) — aucun risque d'omission.
- **Alerte main levée automatique** : chaque dimanche à 14h00, les contrats entièrement soldés
  déclenchent une notification — élimine le risque de contrat terminé non clôturé.
- **Vue encours par banque** : l'encours total de leasing est consultable en temps réel, filtrable
  par organisme (BMCI Leasing, CIH Leasing, Wafabail, etc.) ou numéro de contrat.
- **Recherche par période** : les contrats se filtrent par plage de date de début, et les échéances
  du tableau d'amortissement par plages de dates d'échéance, d'opération et de valeur — retrouver
  un contrat ou une échéance précise sans parcourir toute la liste.
- **Traçabilité complète** : chaque modification de contrat (qui, quand, quoi) est historisée,
  consultable à tout moment en cas de litige.
- **Export PDF prêt à partager** : chaque liste (organismes, contrats cadres, contrats encours, en
  attente de main levée, archivés) et le tableau d'amortissement peuvent être exportés en un clic,
  filtres appliqués — un document prêt à transmettre sans ressaisie.

---

Ce suivi des financements se prolonge naturellement vers les marchés de Staport eux-mêmes :
G-Banque centralise chaque marché et l'ensemble des opérations financières qui s'y rattachent —
cautions, décomptes, préfinancements, factoring et droits constatés — dans un même référentiel
cohérent.

---

## Chapitre 8 — Marchés

### Ce que ce module résout

G-Banque centralise la gestion de tous les marchés de Staport et de leur
rattachement bancaire, et garantit que chaque marché est pris en compte — ou exclu, une fois
archivé — de manière fiable et systématique dans l'ensemble des calculs financiers qui en
dépendent : agios, encours des lignes fonctionnelles, préfinancements, droits constatés,
factoring, décomptes et cautions.

### Ce que le système apporte

- **Numérotation contrôlée** : aucun marché ne peut porter le même numéro qu'un autre, à la
  création comme à la modification.
- **Archivage réversible sans perte d'historique** : un marché terminé peut être archivé en un
  clic — il sort immédiatement et définitivement des calculs financiers futurs (agios, encours)
  sans altérer l'historique déjà généré ; l'opération reste réversible à tout moment.
- **Protection contre la perte de données** : la suppression définitive est bloquée dès qu'un
  marché porte des données financières (décomptes, préfinancements, droits constatés, factorings,
  lignes spécifiques) — le système oriente alors vers l'archivage, éliminant tout risque de perte
  accidentelle d'historique.
- **Visibilité consolidée** : chaque marché donne un accès direct à l'ensemble de ses lignes
  fonctionnelles (générales et spécifiques), sans recherche module par module.
- **Traçabilité complète** : chaque création, modification, suppression, archivage ou désarchivage
  est historisé avec auteur et date.
- **Recherche par période** : filtrage par plage de dates sur le début et l'achèvement des travaux,
  indépendamment l'un de l'autre, pour retrouver rapidement les marchés d'une période donnée.
- **Export PDF** : état complet des marchés (avec filtres) exportable en un clic pour le reporting
  ou le partage externe.

## Chapitre 9 — Caution

### Ce que ce module résout

G-Banque automatise la gestion complète des cautions bancaires de Staport, de leur émission à leur
main levée : calcul des commissions dues à chaque banque partenaire, génération automatique
mensuelle, et suivi du statut de chaque caution sont pris en charge directement par le système.

### Ce que le système apporte

- **Commissions calculées automatiquement** : chaque banque partenaire (AWB, BMCI, CDM, SAHAM
  BANK) applique sa propre formule et son propre calendrier de facturation — génération
  automatique chaque fin de mois (AWB, BMCI) ou début de mois (CDM), y compris les cas
  particuliers de prorata (caution ouverte et clôturée dans le même mois, premier mois d'une
  caution) et les règles propres à chaque banque, comme la non-facturation du jour de délivrance
  chez AWB — les commissions prévues correspondent ainsi au montant réellement prélevé par la
  banque.
- **TVA automatique** : calculée et enregistrée en même temps que la commission pour les banques
  qui l'exigent.
- **Paramètres administrables** : seuils de commission minimum et mode de calcul du prorata
  journalier configurables par banque depuis un écran d'administration dédié, sans intervention
  technique.
- **Traçabilité complète** : chaque création, modification, suppression et clôture de caution est
  historisée (auteur, date), consultable à tout moment.
- **Correction fiabilisée après clôture** : une erreur de saisie sur une caution déjà clôturée
  (date de main levée, montant, taux…) reste corrigeable, avec recalcul automatique de la
  commission de clôture concernée — sans double saisie ni risque d'écart entre la caution et la
  commission facturée.
- **Recherche par période** : la date de prise et la date de main levée peuvent chacune être
  filtrées indépendamment sur une plage précise.
- **Export PDF ou Excel** : état complet des cautions (avec filtres) exportable en un clic, au
  format PDF pour le partage externe ou Excel pour le retraitement et l'analyse en interne.
- **Couverture** : prise → suivi mensuel des commissions → main levée, pour les quatre banques
  partenaires citées.

## Chapitre 10 — Décomptes

### Ce que ce module résout

G-Banque centralise le suivi des décomptes de travaux facturés dans le cadre des marchés de
Staport : saisie, suivi du règlement et consultation de l'encours par banque sont pris en charge
directement par le système, avec une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Suivi automatique de l'encours par banque** : les décomptes non réglés à échéance de fin de
  mois sont identifiés et totalisés automatiquement, sans recensement manuel.
- **Traçabilité complète** : chaque création, modification et suppression est historisée (auteur,
  date), consultable à tout moment.
- **Recherche par période** : les décomptes se retrouvent instantanément sur une plage de dates
  d'émission, d'opération ou de valeur, au choix — utile pour tout rapprochement ou reporting
  périodique.
- **Export PDF** : état complet des décomptes (avec filtres) exportable en un clic.


## Chapitre 11 — Préfinancements

### Ce que ce module résout

G-Banque centralise le suivi des préfinancements liés aux marchés de Staport : déblocages et
remboursements de fonds sont saisis, rattachés automatiquement à la bonne ligne fonctionnelle du
marché, et consultables en encours par banque, avec une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Rattachement automatique fiabilisé** : le système identifie la ligne fonctionnelle
  Préfinancement active du marché (spécifique en priorité, sinon générale) et refuse toute saisie
  si aucune ligne valide n'existe — élimine le risque d'un préfinancement mal imputé.
- **Suivi séparé déblocage/remboursement** : déblocages et remboursements non réglés à échéance de
  fin de mois sont distingués et totalisés automatiquement, par banque.
- **Vue agrégée par ligne fonctionnelle** : total débit, total crédit et reste disponible, avec le
  détail des mouvements, directement depuis chaque ligne.
- **Traçabilité complète** : chaque création, modification et suppression est historisée (auteur,
  date).
- **Recherche par période** : les préfinancements se retrouvent instantanément sur une plage de
  dates d'émission, d'opération ou de valeur, au choix (nouveau, 2026-07-24) — utile pour tout
  rapprochement ou reporting périodique.
- **Export PDF** : la liste est exportable en un clic, filtres appliqués, pour le reporting ou le partage externe ; les encours (déblocage, remboursement) s'exportent depuis le Récapitulatif des États des encours.
.

## Chapitre 12 — Factoring

### Ce que ce module résout

G-Banque centralise le suivi des opérations de factoring liées aux marchés de Staport : déblocages
et remboursements de fonds sont saisis, rattachés automatiquement à la bonne ligne fonctionnelle du
marché, et consultables en encours par banque, avec une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Rattachement automatique fiabilisé** : le système identifie la ligne fonctionnelle Factoring
  active du marché (spécifique en priorité, sinon générale) et refuse toute saisie si aucune ligne
  valide n'existe — élimine le risque d'un factoring mal imputé.
- **Suivi séparé déblocage/remboursement** : déblocages et remboursements non réglés à échéance de
  fin de mois sont distingués et totalisés automatiquement, par banque.
- **Vue agrégée par ligne fonctionnelle** : total débit, total crédit et reste disponible, avec le
  détail des mouvements, directement depuis chaque ligne.
- **Traçabilité complète** : chaque création, modification et suppression est historisée (auteur,
  date).
- **Recherche par période** : les opérations de factoring se retrouvent instantanément sur une
  plage de dates d'émission, d'opération ou de valeur, au choix (nouveau, 2026-07-24) — utile pour
  tout rapprochement ou reporting périodique.
- **Export PDF** : la liste est exportable en un clic, filtres appliqués, pour le reporting ou le partage externe ; les encours (déblocage, remboursement) s'exportent depuis le Récapitulatif des États des encours.


## Chapitre 13 — Droits Constatés

### Ce que ce module résout

G-Banque centralise le suivi des droits constatés liés aux marchés de Staport : déblocages et
remboursements de fonds sont saisis, rattachés automatiquement à la bonne ligne fonctionnelle du
marché, et consultables en encours par banque, avec une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Rattachement automatique fiabilisé** : le système identifie la ligne fonctionnelle Droit
  Constaté active du marché (spécifique en priorité, sinon générale) et refuse toute saisie si
  aucune ligne valide n'existe — élimine le risque d'un droit constaté mal imputé.
- **Pré-remplissage intelligent de la banque** : dès la sélection du marché, la banque du mouvement
  est proposée automatiquement, tout en restant modifiable si les fonds transitent par un autre
  compte.
- **Suivi séparé déblocage/remboursement** : déblocages et remboursements non réglés à échéance de
  fin de mois sont distingués et totalisés automatiquement, par banque.
- **Vue agrégée par ligne fonctionnelle** : total débit, total crédit et reste disponible, avec le
  détail des mouvements, directement depuis chaque ligne.
- **Traçabilité complète** : chaque création, modification et suppression est historisée (auteur,
  date).
- **Recherche par période** : les droits constatés se retrouvent instantanément sur une plage de
  dates d'émission, d'opération ou de valeur, au choix (nouveau, 2026-07-24) — utile pour tout
  rapprochement ou reporting périodique.
- **Export PDF** : la liste est exportable en un clic, filtres appliqués, pour le reporting ou le partage externe ; les encours (déblocage, remboursement) s'exportent depuis le Récapitulatif des États des encours.


---

Au-delà du suivi des marchés, G-Banque couvre également l'ensemble des moyens de paiement
quotidiens de Staport — chèques, LCN, virements nationaux et internationaux, mises à disposition
et prélèvements — avec la même exigence de traçabilité et de fiabilité.

---

## Chapitre 14 — Chèques

### Ce que ce module résout

G-Banque centralise le suivi des chèques de Staport, qu'ils soient reçus (encaissements) ou émis
(décaissements), avec un état à jour en permanence et une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Suivi centralisé des deux sens** : chèques reçus et émis par Staport suivis dans les mêmes
  écrans, avec un état (en cours, réglé, annulé) toujours à jour.
- **Alimentation automatique des décaissements** : les chèques émis sont récupérés automatiquement
  depuis le système de paiement externe (FunPaiement) — aucune ressaisie, aucune double saisie.
- **Vue des encours par banque** : montant total des chèques non encore réglés consultable
  instantanément, par banque et par sens.
- **Rapprochement bancaire intégré** : la validation d'un chèque le rattache directement au
  rapprochement du mois, sans ressaisie.
- **Protection contre les erreurs** : un chèque déjà réglé ne peut plus être modifié, supprimé ou
  altéré ; toute création, modification ou suppression est auditée (auteur, horodatage).
- **Recherche par période** : les chèques (encaissements et décaissements) se retrouvent
  instantanément sur une plage de dates d'émission, d'échéance, d'opération ou de valeur, au choix,
  utile pour tout rapprochement ou reporting périodique.
- **Export PDF** : chaque liste (encaissements, décaissements) peut être exportée en PDF en un clic, filtres appliqués, prête à partager ou archiver ; les encours s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 15 — LCN

### Ce que ce module résout

G-Banque centralise le suivi des LCN (lettres de change normalisées) de Staport, qu'ils soient reçus (encaissements, y compris escomptés) ou émis (décaissements), avec un état à jour en permanence et une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Suivi centralisé des deux sens** : LCN reçus (standards ou escomptés) et émis par Staport
  suivis dans les mêmes écrans, avec un état (en cours, réglé, annulé) toujours à jour.
- **Alimentation automatique des décaissements** : les LCN émis sont récupérés automatiquement
  depuis le système de paiement externe — aucune ressaisie.
- **Suivi de l'escompte commercial par ligne** : chaque ligne fonctionnelle d'escompte dispose d'un
  suivi dédié des LCN qui lui sont rattachés, avec le montant total cumulé — visibilité immédiate
  sur l'utilisation de chaque ligne.
- **Vue des encours par banque** : montant total des LCN non encore réglés consultable
  instantanément, par banque et par sens.
- **Rapprochement bancaire intégré** : la validation d'un LCN le rattache directement au
  rapprochement du mois, sans ressaisie.
- **Protection contre les erreurs** : un LCN déjà réglé ne peut plus être modifié, supprimé ou
  altéré ; un escompte ne peut être créé que dans les conditions prévues (LCN d'encaissement
  uniquement).
- **Recherche par période** : les LCN (encaissements et décaissements) se retrouvent
  instantanément sur une plage de dates d'émission, d'échéance, d'opération ou de valeur, au choix,
  utile pour tout rapprochement ou reporting périodique.
- **Export PDF** : chaque liste (encaissements, décaissements) peut être exportée en PDF en un clic, filtres appliqués, prête à partager ou archiver ; les encours s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 16 — Virements

### Ce que ce module résout

G-Banque centralise le suivi des virements nationaux de Staport, qu'ils soient reçus
(encaissements) ou émis (décaissements), avec un état à jour en permanence et une traçabilité
complète de chaque opération.

### Ce que le système apporte

- **Suivi centralisé des deux sens** : virements reçus et émis par Staport suivis dans les mêmes
  écrans, avec un état (en cours, réglé, annulé) toujours à jour.
- **Alimentation automatique des décaissements** : les virements émis sont récupérés
  automatiquement depuis le système de paiement externe — aucune ressaisie.

- **Vue des encours par banque** : montant total des virements non encore réglés consultable
  instantanément, par banque et par sens.
- **Rapprochement bancaire intégré** : la validation d'un virement le rattache directement au
  rapprochement du mois, sans ressaisie.
- **Protection contre les erreurs** : un virement déjà réglé ne peut plus être modifié, supprimé ou
  altéré ; toute création, modification ou suppression est auditée.
- **Recherche par période** : les virements (encaissements et décaissements) se retrouvent
  instantanément sur une plage de dates d'émission, d'opération ou de valeur, au choix, utile pour
  tout rapprochement ou reporting périodique.
- **Export PDF** : chaque liste (encaissements, décaissements) peut être exportée en PDF en un clic, filtres appliqués, prête à partager ou archiver ; les encours s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 17 — Virements Internationaux

### Ce que ce module résout

G-Banque centralise le suivi des virements internationaux de Staport, reçus comme émis, avec un
état à jour en permanence et une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Suivi centralisé des deux sens** : virements internationaux reçus et émis par Staport suivis
  dans le même écran, avec un état (en cours, réglé, annulé) toujours à jour — les deux sens sont
  saisis manuellement (aucune synchronisation FunPaiement pour ce module).
- **Vue des encours par banque** : montant total non encore réglé consultable instantanément, par
  banque et par sens.
- **Rapprochement bancaire intégré** : la validation rattache directement l'opération au
  rapprochement du mois, sans ressaisie.
- **Protection contre les erreurs** : un virement international déjà réglé ne peut plus être
  modifié, supprimé ou altéré ; toute création, modification ou suppression est auditée.
- **Recherche par période** : les virements internationaux se retrouvent instantanément sur une
  plage de dates d'émission, d'opération ou de valeur, au choix, utile pour tout rapprochement ou
  reporting périodique.
- **Export PDF** : la liste (encaissements et décaissements confondus) peut être exportée en PDF en un clic, filtres appliqués, prête à partager ou archiver ; les encours s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 18 — Mises à Disposition

### Ce que ce module résout

G-Banque centralise le suivi des mises à disposition de fonds émises par Staport, avec un état à jour
en permanence et une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Alimentation automatique** : les mises à disposition sont récupérées automatiquement depuis le
  système de paiement externe — aucune ressaisie, donc aucun risque d'erreur de saisie.
- **Suivi de l'état** : en cours, réglé, annulé — toujours à jour.
- **Vue des encours par banque** : montant total non encore réglé consultable instantanément, par
  banque.
- **Rapprochement bancaire intégré** : la validation rattache directement l'opération au
  rapprochement du mois, sans ressaisie.
- **Protection contre les erreurs** : une mise à disposition ne se supprime pas (la synchronisation la
  recréerait) ; elle s'annule, et une mise à disposition déjà réglée ne peut plus être altérée ;
  les changements de statut sont audités.
- **Recherche par période** : les mises à disposition se retrouvent instantanément sur une plage de
  dates d'émission, d'opération ou de valeur, au choix, utile pour tout rapprochement ou reporting
  périodique.
- **Export PDF** : la liste peut être exportée en PDF en un clic, filtres appliqués, prête à partager
  ou archiver ; les encours s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 19 — Recap Encaissement / Recap Décaissement

### Ce que ce module résout

G-Banque offre une vision consolidée de tous les encaissements d'une part, et de tous les
décaissements d'autre part, tous moyens de paiement confondus (chèques, LCN, virements nationaux,
virements internationaux, mises à disposition), sans avoir à ouvrir chaque écran de moyen de
paiement un par un.

### Ce que le système apporte

- **Une vue unique par sens** : deux écrans — Recap Encaissement et Recap Décaissement — qui
  regroupent automatiquement les opérations des 5 moyens de paiement, quel que soit leur statut ou
  leur banque.
- **Filtrage riche et immédiat** : par banque, par moyen de paiement, par statut, par numéro, par
  libellé, par montant ou par période (date d'opération ou date de valeur), combinables librement.
- **Total instantané** : le montant total filtré s'affiche en continu, sans calcul manuel.
- **Export PDF** : chaque recap s'exporte en un clic, filtres appliqués, prêt à partager avec la
  direction ou à archiver.

## Chapitre 20 — Prélèvements

### Ce que ce module résout

G-Banque centralise le suivi des prélèvements bancaires de Staport, avec un état à jour en
permanence et une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Suivi centralisé** : tous les prélèvements suivis dans un écran unique, avec un état (prévu,
  réglé) toujours à jour.
- **Génération automatique des charges récurrentes** : les prélèvements connus à l'avance (ex.
  cotisation CNSS) sont créés automatiquement chaque mois, pour chaque banque concernée, sans
  intervention de l'équipe Finance.
- **Vue des encours par banque** : montant total non encore réglé consultable instantanément, par
  banque.
- **Rapprochement bancaire intégré** : la validation rattache directement l'opération au
  rapprochement du mois, sans ressaisie.
- **Traçabilité complète** : toute création, modification ou suppression est auditée (auteur,
  horodatage).
- **Export PDF** : la liste des prélèvements peut être exportée en PDF en un clic, filtres appliqués, prête à partager ou archiver ; les encours s'exportent depuis le Récapitulatif des États des encours.

---

Toutes ces opérations, quel que soit le module qui les produit, convergent ensuite vers un
historique unique : le module Transactions, qui donne une vue consolidée de chaque
mouvement bancaire de l'entreprise.

---

## Chapitre 21 — Transactions

### Ce que ce module résout

G-Banque centralise en un seul historique consultable tous les mouvements bancaires produits par
l'ensemble des modules financiers de Staport (leasing, crédits, moyens de paiement, agios,
commissions, cautions, impôts et taxes, et plus généralement toute opération validée dans un
rapprochement). Ceci nous permet une vue unique et fiable de chaque mouvement, sans
avoir à consulter chaque module séparément.

### Ce que le système apporte

- **Traçabilité unifiée** : chaque opération validée dans n'importe quel module (leasing, crédit
  moyen terme, chèques, LCN, virements, virements internationaux, mises à disposition,
  prélèvements, préfinancements, factoring, droits constatés, décomptes, commissions, agios,
  cautions, impôts et taxes, frais bancaires, restitutions, opérations erronées, impayées) produit
  automatiquement une transaction dans un historique commun, avec sa source clairement identifiée.
- **Cohérence garantie** : toute correction ponctuelle (date de valeur, date d'opération, TVA) est
  automatiquement répercutée sur l'opération source — impossible que les deux divergent
  silencieusement.
- **Verrouillage automatique** : une fois un rapprochement définitivement clôturé, ses transactions
  ne sont plus modifiables, garantissant l'intégrité des comptes arrêtés.
- **Recherche et filtrage transverses** : l'historique est consultable par banque, libellé,
  montant, source ou statut de rapprochement, tous modules confondus, sans naviguer module par
  module.
- **Recherche par période** : chacune des trois dates d'une transaction (émission, opération,
  valeur) peut être filtrée indépendamment sur une plage précise, pour isoler rapidement les
  mouvements d'un intervalle donné.
- **Export PDF à la demande** : l'historique filtré est exportable en un clic en document PDF prêt
  à être partagé ou archivé, avec le détail des mouvements et les totaux débit/crédit — sans
  ressaisie ni outil externe.

---

Cette vue consolidée des mouvements s'enrichit ensuite du suivi automatique des charges appliquées
par les banques — frais bancaires, agios, impôts et taxes, commissions — que G-Banque calcule et
rattache directement aux comptes concernés.

---

## Chapitre 22 — Frais bancaires

### Ce que ce module résout

G-Banque centralise le suivi de tous les frais bancaires de Staport — commissions, abonnements et
charges diverses facturées par les banques — avec un état à jour en permanence et une génération
automatique des charges récurrentes connues à l'avance.

### Ce que le système apporte

- **Suivi centralisé** : tous les frais bancaires suivis dans un écran unique, avec un état (prévu,
  réglé) toujours à jour, filtrable par banque et libellé.
- **Recherche par période** : chacune des trois dates d'un frais bancaire (émission, opération,
  valeur) peut être filtrée indépendamment sur une plage précise.
- **Génération automatique des charges récurrentes** : les charges fixes connues à l'avance
  (abonnements, commissions de tenue de compte, etc.) sont créées automatiquement chaque mois, par
  banque, sans ressaisie ni risque de doublon.
- **Paramétrage centralisé** : la liste des charges récurrentes est administrable directement dans
  l'application, sans intervention technique pour ajuster un montant ou ajouter une charge.
- **Vue des encours par banque** : montant total non encore réglé consultable instantanément.
- **Rapprochement bancaire intégré** : le règlement rattache directement le frais au rapprochement
  du mois, sans ressaisie.
- **Protection contre les erreurs** : un frais bancaire déjà réglé ne peut plus être supprimé ;
  toute création, modification ou suppression est auditée.
- **Export PDF à la demande** : la liste des frais bancaires filtrée est exportable en un clic en
  document PDF prêt à être partagé ou archivé, avec le montant total des frais exportés.

## Chapitre 23 — Agios bancaires

### Ce que ce module résout

G-Banque automatise l'intégralité du suivi des agios bancaires de Staport : calcul quotidien,
clôture trimestrielle ou mensuelle selon le type, et intégration au rapprochement bancaire, sans calcul manuel ni fichier de
suivi isolé.

### Ce que le système apporte

- **Calcul automatique chaque nuit** : le système reconstruit le solde de chaque compte et calcule
  les agios dus pour la journée, sur quatre types de charges :
  - **Facilité de Caisse** : charge calculée chaque jour de découvert, selon le taux négocié avec
    la banque.
  - **Préfinancement** : encours reconstitué par marché (déblocages reçus moins remboursements
    effectués), intérêts journaliers calculés marché par marché, banque par banque, avec une vue
    détaillée par marché dans le tableau de bord trimestriel.
  - **Droits Constatés** : même logique de reconstitution que le Préfinancement, taux Droits
    Constatés configuré ; un marché sans ligne active est automatiquement exclu, sans erreur ni
    agios fantôme.
  - **Factoring** : même logique, taux Factoring configuré, mais agrégation **mensuelle** plutôt
    que trimestrielle — un suivi plus fin pour cette charge.
  - Pour les trois charges liées aux marchés (Préfinancement, Droits Constatés, Factoring), le taux
    appliqué est résolu selon une règle de priorité : ligne spécifique au marché en premier, sinon
    ligne générale de la banque.
- **Clôture trimestrielle automatisée** : une action de clôture consolide les montants des 3 mois,
  génère automatiquement la charge d'agios définitive, et l'intègre au circuit de validation et de
  rapprochement existant.
- **Intégration au rapprochement** : les agios validés créent automatiquement la transaction dans
  le rapprochement mensuel, sans double saisie ; une annulation reste possible et remet le système
  dans un état cohérent.
- **Traçabilité de bout en bout** : chaque agios est traçable — qui l'a créé et quand, qui l'a
  validé et dans quel rapprochement, et en cas d'annulation, par qui et pour quel motif. La
  direction peut à tout moment consulter les agios en attente par banque, le détail journalier du
  calcul (solde du jour, base de calcul, montant) et l'historique par trimestre (HT, TVA, TTC).
- **Recherche par période** : chacune des trois dates d'un agios ponctuel (émission, opération,
  valeur) peut être filtrée indépendamment sur une plage précise.
- **Couverture** : toutes les banques actives dans G-Banque ; quatre types calculés automatiquement
  (Facilité de Caisse, Préfinancement, Droits Constatés, Factoring) sans aucune intervention
  manuelle ; calcul chaque nuit, 7j/7 ; horizon de suivi = période courante (trimestre pour
  FC/PREFI/DC, mois pour Factoring) avec détail jour par jour ; tous les marchés avec une ligne de
  préfinancement active sont suivis.
- **Export PDF à chaque niveau de consultation** : la liste des agios ponctuels, la vue des
  encours par banque, le tableau de bord des périodes calculées et le détail journalier d'une
  période peuvent chacun être exportés en PDF, reprenant exactement les filtres appliqués à
  l'écran — un document prêt à partager ou archiver sans ressaisie.


## Chapitre 24 — Impôts et Taxes

### Ce que ce module résout

G-Banque centralise le suivi des impôts et taxes de Staport auprès de ses banques — TVA, IR, IS,
retenue à la source et prélèvements fiscaux ou parafiscaux — avec un état à jour en permanence et
une traçabilité complète de chaque opération.

### Ce que le système apporte

- **Suivi centralisé** : tous les impôts et taxes suivis dans un écran unique, avec un état (prévu,
  réglé) toujours à jour.
- **Génération automatique des charges récurrentes** : impôts et taxes périodiques connus (TVA
  mensuelle, IS trimestriel, etc.) générés automatiquement chaque mois, avec protection intégrée
  contre les doublons.
- **Rattachement automatique aux commissions et cautions** : la TVA générée par une commission sur
  caution est créée, mise à jour et supprimée automatiquement, en cohérence permanente avec sa
  source.
- **Fusion de TVA** : plusieurs lignes de TVA d'une même banque, même date et même motif peuvent
  être regroupées en une seule écriture, simplifiant le suivi et le rapprochement.
- **Recherche par période** : chacune des trois dates d'un impôt/taxe (émission, opération, valeur)
  peut être filtrée indépendamment sur une plage précise.
- **Vue des encours par banque** : montant total non encore réglé consultable instantanément.
- **Rapprochement bancaire intégré** : la validation rattache directement l'opération au
  rapprochement du mois, sans ressaisie.
- **Export PDF à la demande** : la liste des impôts et taxes peut être exportée en PDF en un clic, filtres appliqués — un document prêt à partager ou archiver sans ressaisie ; les encours par banque s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 25 — Commissions bancaires

### Ce que ce module résout

Chaque opération financière de Staport (paiement d'un loyer leasing, émission d'un virement, etc.)
peut entraîner une commission bancaire. G-Banque automatise entièrement la génération et le suivi
de ces commissions, en appliquant des règles configurables selon la banque et le type d'opération.

### Ce que le système apporte

- **Génération automatique** : à chaque opération éligible (échéance leasing, virement, LCN de
  décaissement, mise à disposition de décaissement, prise/échéance/clôture de caution), la
  commission due est calculée et enregistrée automatiquement selon les règles configurées — aucune
  saisie manuelle en fin de mois.
- **Règles flexibles par banque et organisme** : les montants varient selon la banque et parfois
  l'organisme (ex. BMCI Leasing ≠ CIH Leasing) ; logique de résolution : règle spécifique à
  l'organisme > règle pour les autres organismes > règle générale par défaut.
- **Suivi jusqu'au paiement** : les commissions générées sont intégrées au circuit de validation et
  de rapprochement mensuel, visibles jusqu'à leur règlement effectif.
- **Recherche par période** : chacune des trois dates d'une commission (émission, opération, valeur)
  peut être filtrée indépendamment sur une plage précise.
- **Configuration sans intervention technique** : montants et règles modifiables depuis
  l'interface d'administration .
- **Export PDF à la demande** : la liste des commissions peut être exportée en PDF en un clic, filtres appliqués — un document prêt à partager ou archiver sans ressaisie ; les encours par banque s'exportent depuis le Récapitulatif des États des encours.

---

G-Banque prend également en charge les situations qui s'écartent du cours normal des opérations —
impayées, opérations erronées et restitutions — avec la même traçabilité que les opérations
standards.

---

## Chapitre 26 — Impayées

### Ce que ce module résout

G-Banque centralise le suivi des chèques, effets et autres moyens de paiement rejetés par les
banques de Staport — encaissements comme décaissements — avec un état à jour en permanence et une
traçabilité complète de chaque régularisation.

### Ce que le système apporte

- **Suivi centralisé** : toutes les impayées suivies dans un écran unique, avec un état (prévue,
  réglée) toujours à jour, filtrable par banque, motif, type (encaissement/décaissement) et statut.
- **Recherche par période** : chacune des trois dates d'une impayée (émission, opération, valeur)
  peut être filtrée indépendamment sur une plage précise.
- **Vue des encours par banque** : montant total non encore régularisé consultable instantanément,
  par banque et par type.
- **Rapprochement bancaire intégré** : la régularisation rattache directement l'impayée au
  rapprochement du mois, avec calcul automatique de la TVA associée, sans ressaisie.
- **Protection contre les erreurs** : une impayée déjà régularisée ne peut plus être supprimée.
- **Traçabilité complète** : chaque impayée reste reliée au moyen de paiement d'origine qui l'a
  générée ; toute création, modification ou suppression est auditée.
- **Export PDF à la demande** : la liste des impayées peut être exportée en PDF en un clic, filtres appliqués — un document prêt à partager ou archiver sans ressaisie ; les encours s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 27 — Opérations Erronées

### Ce que ce module résout

G-Banque centralise le suivi des opérations bancaires enregistrées par erreur et devant être
régularisées, avec un état à jour en permanence et une traçabilité complète de chaque correction.

### Ce que le système apporte

- **Suivi centralisé** : toutes les opérations erronées suivies dans un écran unique, avec un état
  (prévue, réglée) toujours à jour, filtrable par banque, motif, statut et libellé.
- **Recherche par période** : chacune des trois dates d'une opération erronée (émission, opération,
  valeur) peut être filtrée indépendamment sur une plage précise.
- **Vue des encours par banque** : montant total non encore régularisé consultable instantanément.
- **Rapprochement bancaire intégré** : la régularisation rattache directement l'opération au
  rapprochement du mois, avec calcul automatique de la TVA associée, sans ressaisie.
- **Protection contre les erreurs** : une opération erronée déjà régularisée ne peut plus être
  supprimée.
- **Traçabilité complète** : chaque opération erronée reste reliée au moyen de paiement d'origine
  concerné ; toute création, modification ou suppression est auditée.
- **Export PDF à la demande** : la liste des opérations erronées peut être exportée en PDF en un clic, filtres appliqués — un document prêt à partager ou archiver sans ressaisie ; les encours par banque s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 28 — Provision

### Ce que ce module résout

G-Banque permet désormais d'anticiper les mouvements de trésorerie à venir — encaissements comme
décaissements — via des provisions manuelles rattachées à une banque, avant même que l'opération ne
soit constatée sur le relevé bancaire, avec une traçabilité complète de chaque régularisation.

### Ce que le système apporte

- **Anticipation des deux sens de trésorerie** : contrairement aux autres écritures manuelles
  ponctuelles (opérations erronées, restitutions), une même provision peut représenter aussi bien un
  encaissement qu'un décaissement à venir, selon le sens choisi à la création.
- **Suivi centralisé** : toutes les provisions suivies dans un écran unique, avec un état (prévue,
  réglée) toujours à jour, filtrable par banque, motif, sens et libellé.
- **Recherche par période** : chacune des trois dates d'une provision (émission, opération, valeur)
  peut être filtrée indépendamment sur une plage précise.
- **Vue des encours par banque, par sens** : montant total non encore régularisé consultable
  instantanément, décaissements et encaissements présentés séparément.
- **Rapprochement bancaire intégré** : la régularisation rattache directement la provision au
  rapprochement du mois, avec calcul automatique de la TVA associée (déductible pour un
  décaissement, collectée pour un encaissement), sans ressaisie.
- **Protection contre les erreurs** : une provision déjà régularisée ne peut plus être supprimée.
- **Traçabilité complète** : toute création, modification ou suppression est auditée (auteur,
  horodatage).
- **Export PDF à la demande** : la liste des provisions peut être exportée en PDF en un clic, filtres appliqués — un document prêt à partager ou archiver sans ressaisie ; les encours par banque s'exportent depuis le Récapitulatif des États des encours.

## Chapitre 29 — Restitution

### Ce que ce module résout

G-Banque centralise le suivi des sommes que les banques doivent reverser à Staport (restitutions),
avec un état à jour en permanence et une traçabilité complète de chaque régularisation.

### Ce que le système apporte

- **Suivi centralisé** : toutes les restitutions suivies dans un écran unique, avec un état
  (prévue, réglée) toujours à jour, filtrable par banque, statut et libellé.
- **Recherche par période** : chacune des trois dates d'une restitution (émission, opération,
  valeur) peut être filtrée indépendamment sur une plage précise.
- **Vue des encours par banque** : montant total non encore encaissé consultable instantanément.
- **Rapprochement bancaire intégré** : la régularisation rattache directement la restitution au
  rapprochement du mois, avec calcul automatique de la TVA associée, sans ressaisie.
- **Traçabilité complète** : toute création, modification ou suppression est auditée (auteur,
  horodatage).
- **Export PDF à la demande** : la liste des restitutions peut être exportée en PDF en un clic, filtres appliqués — un document prêt à partager ou archiver sans ressaisie ; les encours par banque s'exportent depuis le Récapitulatif des États des encours.

---

L'ensemble de ces opérations, standards comme anomalies, converge chaque mois vers le
rapprochement bancaire, où G-Banque garantit la cohérence entre la comptabilité de Staport et la
réalité de chaque compte.

---

## Chapitre 30 — Rapprochement

### Ce que ce module résout

Le rapprochement bancaire mensuel est l'opération qui garantit que la comptabilité de Staport
reflète fidèlement la réalité de chaque compte bancaire. G-Banque structure cette opération pour
chaque banque autour d'un cycle mensuel clair : une période En cours qui accueille les mouvements,
une Clôture provisoire qui fige les chiffres en vue d'un contrôle, et une Clôture définitive qui
verrouille la période de façon permanente.

### Ce que le système apporte

- **Ouverture automatique de chaque période** : dès qu'une banque est créée ou qu'une période est
  clôturée provisoirement, le mois suivant s'ouvre automatiquement avec le bon solde de départ —
  aucun report manuel.
- **Calcul continu et fiable** : solde de fin, total débit, total crédit et TVA due sont toujours
  calculés à partir des mouvements réels de chaque banque suivie, jamais saisis ni approximés.
- **Cohérence garantie entre périodes** : toute correction apportée pendant la Clôture provisoire
  répercute automatiquement le nouveau solde sur la période suivante — aucun décalage de solde ne
  peut se glisser sans être corrigé.
- **Verrouillage définitif fiable** : une fois une période clôturée définitivement (auteur et date
  historisés), plus aucune modification n'est possible — protège l'intégrité des chiffres remis à
  la comptabilité ou à l'administration fiscale.
- **Rapports prêts à l'emploi, filtrables à la demande** : un rapport PDF de chaque période est
  généré en un clic, en version Finance (avec TVA) ou Comptabilité (sans TVA), sans mise en forme
  manuelle — soit sur l'ensemble de la période, soit restreint aux transactions correspondant aux
  filtres appliqués par l'équipe Finance, pour ne partager que l'information pertinente.
- **Recherche par période** : sur l'écran de détail d'un rapprochement, chacune des trois dates
  d'une transaction (émission, opération, valeur) peut être filtrée indépendamment sur une plage
  précise.
- **Couverture** : l'ensemble des banques suivies dans G-Banque, quel que soit le module d'origine
  des mouvements qui leur sont rattachés.

---

En amont de ce rapprochement mensuel, G-Banque offre à l'équipe Finance un point d'entrée unique
pour visualiser et valider l'ensemble des opérations en attente d'une banque, tous types confondus,
avant qu'elles n'alimentent le rapprochement.

---

## Chapitre 31 — États des encours et Récapitulatif

### Ce que ce module résout

Chaque mois, Staport traite des dizaines d'opérations bancaires de nature très différente —
échéances de crédit, chèques, virements, LCN, préfinancement, agios, impôts et taxes, et bien
d'autres. Avant qu'une opération ne devienne une écriture bancaire officielle, elle doit être
vérifiée puis validée contre le relevé réel de la banque. États des encours donne à l'équipe Finance
un point d'entrée unique pour voir, filtrer et valider — une par une ou en lot — toutes les
opérations en attente d'une banque donnée, quel que soit leur type d'origine.

### Ce que le système apporte

- **Un seul écran pour toutes les opérations en attente** : une vingtaine de types d'opérations
  (crédits leasing et CMT, moyens de paiement, préfinancement, factoring, droits constatés,
  décomptes, commissions, agios, prélèvements, frais bancaires, impôts et taxes, restitutions,
  opérations erronées, impayées, provisions) regroupés par type sous forme de cartes, avec pour chacune
  le nombre d'opérations, les totaux débit et crédit et le nombre d'opérations échues. Encaissements
  et décaissements figurent dans le **même** tableau : la lecture de la position du mois se fait
  d'un seul coup d'œil, avec les totaux côte à côte.
- **Une vue d'ensemble, puis un espace de travail concentré sur chaque type** : à l'arrivée, une carte
  par type d'opération (nombre, débit, crédit, répartition, échues) ; un clic ouvre le type, sur toute la
  largeur de l'écran, avec ses propres filtres et ses boutons d'action toujours visibles. Un lien « échues »
  ouvre directement un type limité à ses opérations échues, et un sélecteur permet de passer d'un type à
  l'autre sans repasser par les cartes.
- **Un écran qui retient où l'on en était** : après avoir créé ou corrigé une opération, l'utilisateur
  retrouve l'écran exactement comme il l'a quitté — même type, mêmes filtres, même page — et la ligne
  modifiée est mise en évidence. Plus de refiltrage à chaque aller-retour : un gain de temps direct sur
  les mois chargés, et moins de risques d'erreur d'attention. La banque est aussi présélectionnée à la
  création.
- **Des filtres à deux niveaux, sans surcharge** : deux filtres globaux seulement (sens et type de
  transaction) pour la vue d'ensemble, et des filtres propres à chaque type (libellé, numéro de pièce,
  montant, période, motif) pour aller droit à l'opération cherchée. L'ajout d'une opération se fait par
  type, depuis l'écran de ce type.
- **Les mêmes gestes pour tous les types** : valider une opération, valider un lot, créer une
  opération, corriger une opération en attente — la démarche et les champs sont identiques quel que
  soit le type, ce qui réduit le temps de prise en main, les erreurs de saisie et la dépendance à une
  personne en particulier. Une nouvelle famille d'opérations s'intègre ensuite sans rien changer au
  parcours des utilisateurs.
- **Recherche transversale** : un seul champ « Libellé / Observation » (qui couvre aussi émetteur et
  organisme), le numéro de pièce, le montant, le motif, la période et le sens permettent de retrouver
  une opération parmi toutes les autres en quelques secondes ; un lien vers l'écran tel qu'il est
  affiché peut être partagé avec un collègue.
- **Alerte sur les échéances** : les opérations arrivées à échéance sont signalées directement sur
  chaque carte et dans les indicateurs globaux, pour qu'aucune échéance ne passe inaperçue.
- **Corrections maîtrisées après validation** : une transaction validée peut être corrigée ou annulée
  tant que le mois n'est pas définitivement clôturé ; l'opération d'origine reste toujours cohérente
  avec la transaction, sans ressaisie. Le retour « en cours » manuel d'une opération rapprochée (réservé à l'administrateur système)
  supprime proprement sa transaction : plus aucune opération « en cours » ne reste liée à une écriture bancaire.
- **Prévisionnel et réalisé jamais confondus** : chaque écran indique explicitement s'il présente
  des opérations *attendues* (pas encore passées en banque) ou *réalisées* (déjà passées sur le
  compte). Les totaux de chaque nature sont affichés là où ils sont produits, et nulle part
  ailleurs — ce qui évite de lire un montant prévisionnel comme s'il était acquis.
- **Validation en lot fiable** : plusieurs opérations similaires validées en une seule action, avec
  un contrôle tout-ou-rien qui élimine tout risque de validation partielle et toute erreur de
  saisie répétée.
- **Cohérence garantie avec le rapprochement** : chaque validation alimente automatiquement la
  bonne période de rapprochement de la banque concernée, sans ressaisie ni erreur d'affectation.
- **Rapports prêts à l'emploi** : récapitulatif global, récapitulatif détaillé par catégorie et
  projection du solde de fin de mois, générés en PDF à la demande, pour une banque donnée ou pour
  l'ensemble du groupe ; export PDF des lignes filtrées depuis le Récapitulatif.
- **Visibilité en temps réel** : à tout moment, projection du solde de fin de mois de chaque
  banque, sans attendre la clôture de la période — élimine le risque d'opération oubliée.

---

Enfin, deux fonctions transverses soutiennent l'ensemble de ces modules au quotidien : la
traçabilité complète de chaque action via les Logs, et l'information automatique des équipes via
les Notifications.

---

## Chapitre 32 — Logs

### Ce que ce module résout

G-Banque conserve la trace de chaque création, modification et suppression réalisée sur les données
financières de Staport, tous modules confondus, et met cette traçabilité à disposition dans un écran
de consultation dédié.

### Ce que le système apporte

- **Traçabilité complète** : chaque opération sensible sur une donnée financière est historisée
  avec son auteur et sa date, sans configuration supplémentaire, sur l'ensemble des modules métier
  de G-Banque.
- **Recherche ciblée** : le journal est consultable par utilisateur, type d'opération, notion
  métier et période.
- **Détail avant/après** : pour toute modification, les valeurs exactes avant et après restent
  consultables, champ par champ — renforce le contrôle interne et la capacité d'audit de Staport.
- **Accès** : réservé à l'administrateur système.

## Chapitre 33 — Notifications

### Ce que ce module résout

G-Banque tient chaque utilisateur informé, en temps réel et sans action de sa part, des échéances
importantes qui approchent (contrats de leasing, crédits moyen terme, lignes fonctionnelles), et
permet à l'administration de diffuser une information générale à toute l'équipe en un clic.

### Ce que le système apporte

- **Alerte automatique des échéances** : chaque semaine, les échéances de leasing, crédit moyen
  terme et lignes fonctionnelles approchant de leur expiration génèrent automatiquement une
  notification, sans surveillance manuelle — réduit le risque d'oubli sur ces échéances.
- **Diffusion d'information générale** : l'administration peut informer instantanément tous les
  utilisateurs connectés d'un message important, avec un historique complet (auteur, date) des
  diffusions.
- **Visibilité immédiate** : un compteur de notifications non lues, visible en permanence, garantit
  qu'aucune alerte importante ne passe inaperçue.
- **Accès** : consultation ouverte à tous les utilisateurs authentifiés ; déclenchement automatique
  sur Leasing, Crédit Moyen Terme et Lignes Fonctionnelles ; déclenchement manuel réservé à
  l'administrateur système.
