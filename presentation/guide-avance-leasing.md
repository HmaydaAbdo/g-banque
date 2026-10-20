# Contrats de leasing avec avance — guide rapide

*Pour l'équipe Finance*

## L'essentiel en 30 secondes

Certains contrats de leasing commencent par une **avance**. Dans G-Banque :

- l'avance est la **première ligne** du contrat (échéance n°1), à la **date de début** ;
- les **mensualités** commencent **un mois plus tard** ;
- l'avance est **comprise dans le prix d'achat** et dans la **durée** du contrat ;
- elle est **réglée par un chèque, une LCN ou un virement** que vous avez déjà saisi : vous le choisissez dans le contrat, et **son montant devient l'avance** ;
- il n'y a qu'**une seule transaction** : celle du chèque / de la LCN / du virement ;
- l'avance n'est **jamais comptée deux fois** dans les encours.

---

## Exemple

Contrat de **36 mois**, prix d'achat **600 000 MAD**, avance de **50 000 MAD**.

| Mois | Ligne du tableau d'amortissement |
|---|---|
| 1 (date de début) | **Échéance n°1 : avance 50 000 MAD** |
| 2 à 36 | Échéances n°2 à n°36 : **35 mensualités** |
| 37 | Dernière échéance : valeur résiduelle (1 % de 600 000 = 6 000 MAD) |

- **Dans le prix d'achat** : l'avance ne s'ajoute pas aux 600 000 MAD, elle en fait partie.
- **Dans la durée** : les 36 mois comptent l'avance. Les phases de mensualités couvrent donc **35 mois**.

---

## Étape 1 — Saisir le règlement de l'avance

Créez le **chèque, la LCN ou le virement** (décaissement) dans son module habituel. Il peut être **déjà réglé** ou **encore en cours**.

## Étape 2 — Créer le contrat

1. **Leasing → Contrats → Nouveau contrat**.
2. À l'étape **Détails généraux**, section **Avance (facultative)** :
   - choisissez le **type** (chèque, LCN, virement) ;
   - choisissez le moyen de paiement par son **numéro** dans la liste (elle se charge au fil du défilement ; la zone de recherche en haut filtre sur le numéro).
3. Le moyen retenu s'affiche avec son montant : **c'est l'avance**. *(« Retirer l'avance » annule le choix.)*
4. À l'étape **Phases**, les durées doivent totaliser **la durée du contrat moins 1 mois**.
5. Vérifiez au **Récapitulatif**, puis **Soumettre**.

**Règles à retenir**
- L'avance doit être **inférieure au prix d'achat**.
- Un contrat avec avance dure **au moins 2 mois**.
- Il n'y a **pas de commission** sur l'avance.
- La liste ne propose que des **décaissements non annulés** qui ne règlent pas déjà l'avance d'un autre contrat.

---

## Ce qui se passe ensuite — automatiquement

| Situation du chèque / de la LCN / du virement | Avance |
|---|---|
| **Réglé** | **Payée** |
| **En cours** | **Prévue** |
| **Annulé** | Redevient une échéance **à payer** dans les encours de leasing |

- Vous validez **le moyen de paiement** dans le rapprochement, **pas** l'avance.
- Son **libellé** et son **observation** indiquent : *« Avance réglée via … — échéance n°1 du contrat N° … »*.
- Dans les **encours** et la **trésorerie prévisionnelle**, seul le **moyen de paiement** apparaît.
- Sur le **Tableau d'amortissement**, une carte **Avance** rappelle le montant, la date et le moyen de paiement.

---

## Changer de moyen de paiement

- Tant que le moyen de paiement **n'est pas réglé** : **modifiez le contrat** et choisissez un autre moyen à l'étape *Détails généraux*. L'ancien retrouve ses textes d'origine.
- Une fois **réglé** : l'avance est payée, le contrat n'est plus modifiable (comme pour toute échéance payée).
- **Supprimer le contrat** retire la mention d'avance du moyen de paiement.
- Les **contrats existants** ne sont pas modifiés : l'avance concerne les nouveaux contrats.

## Messages que vous pouvez rencontrer

| Message | Que faire |
|---|---|
| « L'avance … doit être strictement inférieure au montant d'achat TTC » | Choisir un moyen de paiement d'un montant plus petit |
| « La durée totale des phases … ne correspond pas à la durée à couvrir » | Ajuster les phases : durée du contrat − 1 mois |
| « Seul un décaissement peut régler l'avance » | Choisir un décaissement, pas un encaissement |
| « Ce moyen de paiement règle déjà l'avance d'un contrat » | Choisir un autre règlement |
| « Ce moyen de paiement est annulé » | Choisir un moyen de paiement non annulé |
| « L'avance du contrat est déjà payée … » | Aucune action : le moyen de paiement est réglé |
