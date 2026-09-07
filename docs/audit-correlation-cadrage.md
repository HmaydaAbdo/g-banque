# Corrélation des effets en cascade dans le journal d'audit — note de cadrage

> Document de décision. Rien n'est implémenté à ce jour.
> Objectif : permettre de répondre à « qu'est-ce que **cette** action a provoqué ailleurs ? »

---

## 1. Le problème

Une action métier unique déclenche aujourd'hui plusieurs écritures d'audit indépendantes.

Exemple : la validation d'une transaction écrit une entrée pour la `Transaction`, une pour le
`Rapprochement` mis à jour, une pour chaque solde recalculé en cascade. Ces entrées sont
correctes et complètes, mais **rien ne dit qu'elles découlent de la même action**.

Dans l'écran Logs, elles apparaissent comme des lignes séparées, à la même seconde, sur des
notions différentes. C'est à l'administrateur de reconstituer mentalement le lien — ce qui
suppose qu'il connaisse déjà les règles de cascade du module concerné.

Conséquence concrète : on peut répondre à « qui a modifié cette transaction ? », mais pas à
« quels soldes cette modification a-t-elle déplacés ? ». C'est précisément la question posée
lors d'un contrôle sur un écart de solde.

---

## 2. Ce qui existe déjà et n'est pas à refaire

- Les cascades **sont** journalisées dès lors que le service appelé porte `@AuditableAction`.
  Il ne s'agit pas d'ajouter des traces manquantes, mais de relier des traces existantes.
- `AuditRevisionEntity` attache déjà un `username` à chaque révision Envers.
- `GenericAuditAspect` écrit après commit via `TransactionSynchronization.afterCommit`, donc
  toutes les écritures d'audit d'une même transaction métier passent par le même point.

Ce dernier point est important : le mécanisme de propagation existe déjà, il n'y a pas
d'architecture à inventer, seulement une information à transporter.

---

## 3. Options

### Option A — Identifiant de corrélation sur `Log`

Générer un identifiant à l'entrée de l'action métier (premier `@AuditableAction` de la pile),
le stocker dans un `ThreadLocal` sur le modèle de `BatchAuditContext` qui existe déjà, et
l'inscrire sur chaque ligne `Log` écrite dans la même transaction.

- **Portée** : `Log` (+1 colonne), `GenericAuditAspect`, `AuditLogWriter`, écran Logs.
- **Base** : `ALTER TABLE log ADD COLUMN correlation_id ...` + index. Pas de reprise de
  l'historique — les lignes antérieures restent sans corrélation, ce qui est acceptable.
- **Écran** : un lien « voir les autres effets de cette action » depuis une entrée, ou un
  regroupement visuel des lignes partageant le même identifiant.
- **Risque** : faible. Le `ThreadLocal` doit être nettoyé en `finally` pour ne pas fuiter entre
  requêtes sur un pool de threads — même précaution que `BatchAuditContext`.
- **Limite** : ne relie que ce qui se produit dans la même transaction. Un effet différé
  (scheduler, batch nocturne) n'est pas rattaché.

### Option B — Action racine nommée

Même mécanisme, mais on stocke en plus le libellé de l'action d'origine (« Validation de
transaction ») sur chaque ligne dérivée.

- **Avantage** : lisible sans navigation — chaque ligne porte son contexte.
- **Inconvénient** : information dupliquée sur chaque ligne, et il faut nommer chaque action.
- **Portée** : identique à A, plus un attribut sur `@AuditableAction`.

### Option C — Ne rien relier, documenter les cascades

Laisser le journal en l'état et décrire les règles de cascade dans `docs/modules/`, pour que
l'administrateur sache quoi chercher.

- **Avantage** : coût nul.
- **Inconvénient** : reporte la charge sur le lecteur, et la documentation se désynchronise du
  code. Ne répond pas à la question posée lors d'un contrôle.

---

## 4. Recommandation

**Option A.** Elle résout le problème posé pour un coût contenu, réutilise un mécanisme déjà
présent dans le projet (`BatchAuditContext`), et n'engage pas de convention de nommage à
maintenir sur 182 points d'annotation. L'option B pourra être ajoutée par-dessus plus tard si
la lecture par corrélation seule s'avère insuffisante — l'inverse ne serait pas vrai.

---

## 5. Questions à trancher avant implémentation

1. **Périmètre transactionnel** : se limite-t-on à la transaction, ou veut-on aussi rattacher
   les effets différés (schedulers, génération de charges fixes) à l'action qui les a rendus
   nécessaires ? Le second cas est nettement plus complexe et mérite d'être écarté ou traité à
   part.
2. **Restitution dans l'écran** : lien depuis une entrée vers ses effets, ou regroupement
   direct des lignes corrélées dans la liste ? Le second change la lecture de l'écran pour
   toutes les entrées, y compris celles sans cascade.
3. **Historique** : confirme-t-on qu'aucune reprise des lignes existantes n'est attendue ?
4. **Suppression** : si une entrée corrélée est supprimée manuellement, le groupe devient
   incomplet sans que ça se voie. Ce point rejoint la question de l'immuabilité du journal et
   plaide pour la traiter d'abord.
