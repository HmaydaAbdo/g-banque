# TODO — G-Banque

Rien en attente.

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
