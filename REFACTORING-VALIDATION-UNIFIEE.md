# Refactoring — Écran de validation unifié (encaissement/décaissement)

> Fichier de suivi de ce chantier. Complète `TODO.md` (qui reste le suivi général du projet) —
> celui-ci est dédié au refactoring de l'écran de validation et au chantier repo unique / GitHub
> qui le précède. Mis à jour au fil des sessions, dans le même esprit que `TODO.md`.

---

## Objectif métier

Aujourd'hui, chaque source (Virement, Chèque, LCN, Agios, …) a son propre écran de validation,
dupliqué en décaissement et encaissement quand les deux sens existent (~30 écrans Angular, 22
endpoints REST unitaires + `POST /rapprochement/process/lot`).

Cible discutée le 12/09/2026 : un écran de récapitulatif unique pour la validation, quel que soit
le type et le sens de l'opération. Le formulaire d'édition (TVA, dates, libellé…) et l'action de
validation se déclenchent via l'`id` de la source sélectionnée dans le récap ; deux boutons
contextuels (Encaisser / Décaisser) apparaissent seulement quand le sens est réellement un choix
utilisateur (voir `sensPolicy` par source ci-dessous — pour la majorité des sources le sens est
fixe ou porté par la donnée, pas un choix).

**Contrainte non négociable :** l'application est en production. Le modèle de données
(`Transaction`, tables des 20+ sources) ne doit pas être modifié, sauf nécessité identifiée et
validée explicitement avec HMAYDA avant toute migration. Objectif : ramener la base à un état
stable *après* tout le refactoring, pas la faire évoluer en continu pendant.

---

## Chantier préalable : repo unique + migration GitLab → GitHub

Décidé le 14/09/2026 : ce chantier passe **avant** le début du refactoring applicatif.

### État actuel (constaté)

Trois dépôts git imbriqués, indépendants :
- `G-Banque/` (racine) — `docs/`, `conventions/`, `sql/`, `infra/`, `docker-compose.yml`,
  `CLAUDE.md`, `TODO.md`. Initialisé le 07/09/2026 (voir `TODO.md`).
- `G-Banque/Backend/` — historique complet depuis l'origine du projet.
- `G-Banque/Frontend/` — historique complet depuis l'origine du projet.

`G-Banque/.gitignore` ignore actuellement `Backend/` et `Frontend/` pour éviter que git ne les
traite comme des gitlinks illisibles — cette règle disparaît une fois la fusion faite.

### Décisions arbitrées

- **Historique conservé intégralement** (pas de squash) — via `git subtree`, sans dépendance
  externe (`git-filter-repo` non nécessaire). Chaque dépôt source est ajouté comme remote local
  (chemin de fichier) au dépôt racine, puis fusionné avec `git subtree add --prefix=<Backend|
  Frontend>`. Aucun commit des dépôts source n'est perdu ; ils restent consultables dans
  `git log` du dépôt unifié.
- Le dépôt racine (`G-Banque/`) sert de **base** de la fusion (déjà positionné au bon endroit sur
  le disque, contient déjà `docs/conventions/sql/infra`).
- Séquencement : fusion + migration GitHub **avant** de commencer le travail sur une source, pour
  ne pas cumuler deux chantiers risqués sur la même branche.
- Mode d'exécution pour cette session : le pont shell distant (`device_bash`) est indisponible
  (panne connue liée à une mise à jour Windows du 08/09/2026, suivie par Anthropic). HMAYDA exécute
  les commandes que je fournis et me communique le résultat.

### Étapes (à cocher au fur et à mesure)

- [ ] Sauvegarde complète de `G-Banque/` (copie/zip hors du dossier) avant toute opération
- [ ] Inspection en lecture seule des 3 dépôts (remotes, branche par défaut, `git status`,
      changements non commités éventuels) — nécessaire avant d'écrire les commandes de fusion
- [ ] Confirmation du dépôt GitHub cible (nom, compte/organisation, vide ou déjà créé)
- [ ] Fusion `Backend/` dans le dépôt racine via `git subtree`
- [ ] Fusion `Frontend/` dans le dépôt racine via `git subtree`
- [ ] Nouveau `.gitignore` consolidé (suppression des règles "dépôts imbriqués", fusion des
      règles Backend/Frontend/racine, vérification qu'aucun secret n'est désormais suivi)
- [ ] Vérification qu'aucun secret (`.env`, `env.dev`, mots de passe) n'a jamais été commité dans
      l'historique de l'un des 3 dépôts (sinon : purge avant push public/privé sur GitHub)
- [ ] Création du dépôt GitHub et push de toutes les branches/tags du dépôt unifié
- [ ] Vérification post-fusion : build Backend (`mvn`) et Frontend (`ng build`) fonctionnent
      depuis le dépôt unifié fusionné, avant de retirer les 2 anciens dépôts locaux
- [ ] Retrait des anciens dépôts GitLab / anciens `.git` locaux (seulement une fois le nouveau
      dépôt validé stable)

---

## Chantier applicatif : validation unifiée

### Décisions arbitrées

*(à compléter au fur et à mesure des sessions, comme pour la TVA dans `TODO.md`)*

### Source pilote

*(à choisir avec HMAYDA — critère à discuter : une source simple à sens fixe d'abord, pour poser
le patron avant d'attaquer les sources à sens porté par la ligne comme Préfinancement/Droit
constaté/Factoring)*

### Tableau des 24 sources — statut

| Source | Sens | Politique de sens | Endpoint(s) actuel(s) | Écran(s) actuel(s) | Statut |
|---|---|---|---|---|---|
| Virement | D/E | choix utilisateur | `process/{decaissement,encaissement}/virement` | 2 | Non commencé |
| Chèque | D/E | choix utilisateur | `process/{decaissement,encaissement}/cheque` | 2 | Non commencé |
| LCN | D/E | choix utilisateur | `process/{decaissement,encaissement}/lcn` | 2 | Non commencé |
| Mise à disposition | D/E | choix utilisateur | `process/{decaissement,encaissement}/mise-a-disposition` | 2 | Non commencé |
| Virement international | D/E | choix utilisateur | `process/{decaissement,encaissement}/virement-international` | 2 | Non commencé |
| Préfinancement | D/E | porté par la ligne | `process/prefinancement` (D) / `process/prefinancement` (E, à vérifier) | 2 | Non commencé |
| Droit constaté | D/E | porté par la ligne | `process/droit-constate` | 2 | Non commencé |
| Factoring | D/E | porté par la ligne | `process/factoring` | 2 | Non commencé |
| Provision | D/E | porté par l'enregistrement | `process/provision` | 2 | Non commencé |
| Impayé | D/E | porté par l'enregistrement | `process/impayee` | 2 | Non commencé |
| CMT (échéance) | D | fixe | `process/echeance-cmt` | 1 | Non commencé |
| Leasing (échéance) | D | fixe | `process/echeance-leasing` | 1 | Non commencé |
| Agios | D | fixe | `process/agios` | 1 | Non commencé |
| Commission | D | fixe | `process/commission` | 1 | Non commencé |
| Frais bancaire | D | fixe | `process/frais-bancaire` | 1 | Non commencé |
| Impôt / Taxe | D | fixe | `process/impot-taxe` | 1 | Non commencé |
| Prélèvement | D | fixe | `process/prelevement` | 1 | Non commencé |
| Opération erronée | D | fixe | `process/operation-errone` | 1 | Non commencé |
| Décomptes | E | fixe | `process/decompte` | 1 | Non commencé |
| Restitution | E | fixe | `process/restitution` | 1 | Non commencé |

*Tableau à vérifier/compléter avec HMAYDA (ex. sens réel de Préfinancement/DroitConstaté/
Factoring côté backend, colonnes/filtres par source) avant de s'en servir comme base du
descripteur générique.*

---

## Journal de session

**14/09/2026.** Décisions : repo unique + GitHub avant refactoring applicatif ; historique
conservé (`git subtree`) ; mode d'exécution "HMAYDA exécute les commandes fournies" tant que
`device_bash` est indisponible. Création de ce fichier de suivi.

**12/09/2026.** Première discussion de l'idée : écran de récap unique pour la validation, sens
porté par la ligne plutôt que par un choix utilisateur générique. Risques identifiés : RBAC
(la route n'est plus la frontière de permission), `docs/guide-utilisateur/finance.md` à réécrire
sur la partie validation, distinction entre "unifier la validation" et "unifier le CRUD des 24
entités sources" (hors-scope ici).
