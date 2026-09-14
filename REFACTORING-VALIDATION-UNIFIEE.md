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

## Chantier préalable : repo unique + migration GitLab → GitHub — ✅ TERMINÉ (14/09/2026)

Décidé le 14/09/2026 : ce chantier passe **avant** le début du refactoring applicatif. Terminé le
même jour.

### Résultat

- **Dépôt unifié** : https://github.com/HmaydaAbdo/g-banque (privé), branche `main`.
- **Historique intégralement conservé** via `git subtree` — 444 commits Backend + 291 commits
  Frontend fusionnés sous les préfixes `Backend/` et `Frontend/`, rattachés à l'historique
  préexistant du dépôt racine (docs/conventions/sql/infra, initialisé le 07/09/2026). Aucun
  squash, aucune réécriture de hash — `git log` du dépôt unifié remonte jusqu'au tout premier
  commit de chaque projet.
- **Aucun secret** (`.env`, `env.dev`) trouvé dans l'historique des 3 dépôts avant push (vérifié
  via `git rev-list --objects --all`).
- **Bug corrigé** : `graphify-out/` était suivi par erreur dans Backend et Frontend (règle
  `.gitignore` `src/graphify-out/` qui ne matchait jamais le dossier, réellement à la racine de
  chaque projet). 848 fichiers retirés du suivi (historique existant non réécrit, juste arrêt du
  suivi). Remplacé par une règle unique `/graphify-out/` à la racine du dépôt unifié — un seul
  graphe pour tout le monorepo, à régénérer via `graphify update .` depuis la racine, jamais
  commité.
- **`.gitignore` des 3 emplacements consolidé** : suppression de la règle "dépôts imbriqués"
  (obsolète, Backend/ et Frontend/ sont maintenant de vrais sous-dossiers suivis).
- **Vérification post-fusion réussie** : `mvnw.cmd compile` (Backend) et `npm ci && ng build`
  (Frontend) fonctionnent tous les deux sans erreur depuis le dépôt fusionné — seuls des warnings
  préexistants sans rapport (budgets de taille de bundle, vulnérabilités npm déjà présentes avant
  la fusion, dépréciations de sous-dépendances).

### Étapes (toutes cochées)

- [x] Sauvegarde complète de `G-Banque/` avant toute opération (`G-Banque-BACKUP-20260914`)
- [x] Inspection en lecture seule des 3 dépôts (remotes, branche par défaut `main` partout,
      aucun changement non commité perdu — Backend et Frontend étaient propres et à jour)
- [x] Confirmation du dépôt GitHub cible (`HmaydaAbdo/g-banque`, privé, créé via l'UI web)
- [x] Fusion `Backend/` dans le dépôt racine via `git subtree` (444 commits)
- [x] Fusion `Frontend/` dans le dépôt racine via `git subtree` (291 commits)
- [x] Retrait de `graphify-out/` (suivi par erreur) + nouveau `.gitignore` consolidé
- [x] Vérification qu'aucun secret n'a jamais été commité dans l'historique des 3 dépôts
- [x] Création du dépôt GitHub et push de `main` (historique complet, 25423 objets)
- [x] Vérification post-fusion : build Backend (`mvn`) et Frontend (`ng build`) réussis depuis
      le dépôt unifié
- [ ] Retrait des anciens dépôts GitLab / anciens `.git` locaux — **volontairement pas encore
      fait**, gardés en sécurité (`Backend-old-standalone-repo/`, `Frontend-old-standalone-repo/`
      à côté de `G-Banque/`, + backup complet `G-Banque-BACKUP-20260914`). À faire seulement
      quand HMAYDA confirme que tout est stable après un usage réel du dépôt fusionné.

### Reste en suspens (mineur, non bloquant)

- Les branches `feat/recap-provisions-ui` (Backend et Frontend) et `claude/admiring-murdock`
  (Frontend, utilisée dans un git worktree local) n'ont pas été recréées dans le dépôt unifié —
  seule `main` a été fusionnée. Leur historique reste présent dans les objets git locaux (remotes
  `backend-local`/`frontend-local`) et dans les backups, donc rien n'est perdu ; à recréer comme
  vraies branches du dépôt unifié si besoin plus tard.
- Remotes locaux `backend-local`/`frontend-local` (pointant vers les anciens dossiers) encore
  présents dans le dépôt racine — à nettoyer (`git remote remove`) une fois les anciens dépôts
  définitivement retirés.

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

**14/09/2026.** Chantier repo unique + GitHub mené de bout en bout : backup, fusion `git subtree`
de Backend (444 commits) et Frontend (291 commits) dans le dépôt racine, vérification absence de
secrets dans l'historique, correction du bug `graphify-out` (848 fichiers suivis par erreur,
gitignore corrigé), push vers `github.com/HmaydaAbdo/g-banque` (privé), build Backend + Frontend
vérifiés depuis le dépôt fusionné. `device_bash` indisponible tout du long (panne connue depuis le
08/09) — chaque commande donnée puis exécutée manuellement par HMAYDA, résultat revérifié avant la
suivante. Prochaine étape : choisir la source pilote pour le refactoring applicatif.

**12/09/2026.** Première discussion de l'idée : écran de récap unique pour la validation, sens
porté par la ligne plutôt que par un choix utilisateur générique. Risques identifiés : RBAC
(la route n'est plus la frontière de permission), `docs/guide-utilisateur/finance.md` à réécrire
sur la partie validation, distinction entre "unifier la validation" et "unifier le CRUD des 24
entités sources" (hors-scope ici).
