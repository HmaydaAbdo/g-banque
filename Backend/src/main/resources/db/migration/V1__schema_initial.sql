-- =============================================================================
-- V1 — Schéma initial de G-Banque (structure de la production au 10/10/2026)
-- =============================================================================
--
-- Généré depuis l'export de la base de production du 10/10/2026 (structure seule, aucune donnée).
-- Sur la base de production existante, cette migration N'EST PAS exécutée : Flyway y pose une
-- « baseline » en version 1 (spring.flyway.baseline-on-migrate). Elle sert à construire une base
-- vierge identique (dev, tests, CI, nouvel environnement).
--
-- Adaptations par rapport à l'export :
--   * compteurs AUTO_INCREMENT retirés (repartent à 1) ;
--   * largeurs d'affichage des entiers retirées (bigint(20) → bigint…, obsolètes en MySQL 8 ; tinyint(1) conservé) ;
--   * tables Spring Batch : script officiel spring-batch-core 5.2 (schema-mysql.sql), noms en
--     MAJUSCULES — l'export (passé par un MySQL Windows) les avait mis en minuscules, ce qui les rend
--     introuvables par Spring Batch sur MySQL Linux (noms de tables sensibles à la casse) ;
--   * vues exclues : migrations répétables R__v_*.sql ;
--   * séquence d'audit Envers initialisée à 1.
--
-- Ne jamais modifier ce fichier une fois appliqué : toute évolution passe par une nouvelle V<n>.
-- =============================================================================

SET NAMES utf8mb4;


-- ── Tables ──────────────────────────────────────────────────────────────────

CREATE TABLE `agios` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `agios_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `agios_journalier` (
  `id` bigint NOT NULL,
  `agios_ht` decimal(19,2) NOT NULL,
  `agios_ttc` decimal(19,2) NOT NULL,
  `base_calcul` decimal(19,2) NOT NULL,
  `date_calcul` datetime(6) NOT NULL,
  `date_valeur` date NOT NULL,
  `metadata` text DEFAULT NULL,
  `nb_recalculs` int NOT NULL,
  `solde_journalier` decimal(19,2) NOT NULL,
  `taux_applique` decimal(8,4) NOT NULL,
  `tva` decimal(19,2) NOT NULL,
  `type_agios` enum('DROIT_CONSTATE','FACILITE_CAISSE','FACTORING','PREFINANCEMENT') NOT NULL,
  `banque_id` bigint NOT NULL,
  `trimestre_id` bigint DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `agios_trimestriel` (
  `id` bigint NOT NULL,
  `agios_definitif_id` bigint DEFAULT NULL,
  `annee` int NOT NULL,
  `date_cloture` datetime(6) DEFAULT NULL,
  `date_debut` date NOT NULL,
  `date_dernier_calcul` datetime(6) DEFAULT NULL,
  `date_fin` date NOT NULL,
  `nb_jours_calcules` int NOT NULL,
  `nb_jours_total` int NOT NULL,
  `statut` enum('CLOTURE','OUVERT') NOT NULL,
  `total_agios_ht` decimal(19,2) NOT NULL,
  `total_agios_ttc` decimal(19,2) NOT NULL,
  `total_tva` decimal(19,2) NOT NULL,
  `trimestre` int NOT NULL,
  `type_agios` enum('DROIT_CONSTATE','FACILITE_CAISSE','FACTORING','PREFINANCEMENT') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `agios_trimestriel_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `agios_definitif_id` bigint DEFAULT NULL,
  `annee` int DEFAULT NULL,
  `date_cloture` datetime(6) DEFAULT NULL,
  `date_debut` date DEFAULT NULL,
  `date_dernier_calcul` datetime(6) DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `nb_jours_calcules` int DEFAULT NULL,
  `nb_jours_total` int DEFAULT NULL,
  `statut` enum('CLOTURE','OUVERT') DEFAULT NULL,
  `total_agios_ht` decimal(19,2) DEFAULT NULL,
  `total_agios_ttc` decimal(19,2) DEFAULT NULL,
  `total_tva` decimal(19,2) DEFAULT NULL,
  `trimestre` int DEFAULT NULL,
  `type_agios` enum('DROIT_CONSTATE','FACILITE_CAISSE','FACTORING','PREFINANCEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `audit_revision_entity` (
  `id` int NOT NULL,
  `timestamp` bigint NOT NULL,
  `username` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `audit_revision_entity_seq` (
  `next_val` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `banque` (
  `id` bigint NOT NULL,
  `address` varchar(255) NOT NULL,
  `agence` varchar(100) NOT NULL,
  `date_creation` date NOT NULL,
  `date_modification` date NOT NULL,
  `devise` enum('EUR','MAD','USD','GNS') NOT NULL,
  `logo_data` longblob NOT NULL,
  `nom` varchar(100) NOT NULL,
  `numero_compte` varchar(24) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `banque_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `agence` varchar(100) DEFAULT NULL,
  `date_creation` date DEFAULT NULL,
  `date_modification` date DEFAULT NULL,
  `devise` enum('EUR','GNS','MAD','USD') DEFAULT NULL,
  `nom` varchar(100) DEFAULT NULL,
  `numero_compte` varchar(24) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cautions` (
  `id` bigint NOT NULL,
  `actif` tinyint(1) NOT NULL DEFAULT 1,
  `created_at` date NOT NULL,
  `date_de_prise` date DEFAULT NULL,
  `designation` text NOT NULL,
  `montant` decimal(19,2) NOT NULL,
  `numero_caution` varchar(255) DEFAULT NULL,
  `organisme` text NOT NULL,
  `taux` decimal(19,6) NOT NULL,
  `updated_at` date NOT NULL,
  `banque_id` bigint NOT NULL,
  `marche_id` bigint DEFAULT NULL,
  `nature_id` bigint NOT NULL,
  `date_de_main_levee` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cautions_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `actif` tinyint(1) DEFAULT 1,
  `created_at` date DEFAULT NULL,
  `date_de_main_levee` date DEFAULT NULL,
  `date_de_prise` date DEFAULT NULL,
  `designation` text DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `numero_caution` varchar(255) DEFAULT NULL,
  `organisme` text DEFAULT NULL,
  `taux` decimal(19,6) DEFAULT NULL,
  `updated_at` date DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL,
  `nature_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `caution_commission_config` (
  `id` bigint NOT NULL,
  `diviseur` decimal(19,2) NOT NULL,
  `montant_minimum` decimal(19,2) NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `caution_commission_config_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `diviseur` decimal(19,2) DEFAULT NULL,
  `montant_minimum` decimal(19,2) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `charge_fixe_config` (
  `id` bigint NOT NULL,
  `append_periode` bit(1) NOT NULL,
  `jour_emission` int NOT NULL,
  `libelle` varchar(150) NOT NULL,
  `mois_precedent` bit(1) NOT NULL,
  `montant` decimal(19,2) NOT NULL,
  `motif` enum('ADII','AUTRE_ASSURANCE','COMMUNAL','DGI','IR','IS','PENALITE_DE_RETARD','RETENUE_A_LA_SOURCE','TAXE_PROFESSIONNELLE','TVA','TVA_COMMISSION_CAUTION','VIGNETTE') DEFAULT NULL,
  `type` enum('AGIOS','FRAIS_BANCAIRE','IMPOT_TAXE','PRELEVEMENT') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `charge_fixe_config_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `append_periode` bit(1) DEFAULT NULL,
  `jour_emission` int DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `mois_precedent` bit(1) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `motif` enum('ADII','AUTRE_ASSURANCE','COMMUNAL','DGI','IR','IS','PENALITE_DE_RETARD','RETENUE_A_LA_SOURCE','TAXE_PROFESSIONNELLE','TVA','TVA_COMMISSION_CAUTION','VIGNETTE') DEFAULT NULL,
  `type` enum('AGIOS','FRAIS_BANCAIRE','IMPOT_TAXE','PRELEVEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cheques` (
  `id` bigint NOT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `beneficiaire` varchar(255) NOT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) NOT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `banque_id` bigint NOT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') NOT NULL,
  `id_paiement` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cheques_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `beneficiaire` varchar(255) DEFAULT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `id_paiement` varchar(50) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cmt_config` (
  `id` bigint NOT NULL,
  `alerte_echeance_jours_court_terme` int NOT NULL,
  `alerte_echeance_jours_long_terme` int NOT NULL,
  `date_modification` datetime(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `cmt_config_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `alerte_echeance_jours_court_terme` int DEFAULT NULL,
  `alerte_echeance_jours_long_terme` int DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `commission` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `operation` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','Importation','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') NOT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL,
  `caution_id` bigint DEFAULT NULL,
  `impot_et_taxe_id` bigint DEFAULT NULL,
  `virement_id` bigint DEFAULT NULL,
  `type_commission` enum('CLOTURE','MENSUELLE','PRORATA_ENTREE') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `commission_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `operation` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','Importation','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `caution_id` bigint DEFAULT NULL,
  `impot_et_taxe_id` bigint DEFAULT NULL,
  `virement_id` bigint DEFAULT NULL,
  `type_commission` enum('CLOTURE','MENSUELLE','PRORATA_ENTREE') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `commission_config` (
  `id` bigint NOT NULL,
  `commission_condition` enum('DEFAULT','SI_MATCH','SI_NON_MATCH') NOT NULL,
  `montant` decimal(19,4) NOT NULL,
  `organisme_nom` varchar(150) DEFAULT NULL,
  `type` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `commission_config_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `commission_condition` enum('DEFAULT','SI_MATCH','SI_NON_MATCH') DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `organisme_nom` varchar(150) DEFAULT NULL,
  `type` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `contrat_cadre` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_debut` date NOT NULL,
  `date_fin` date NOT NULL,
  `date_modification` datetime(6) NOT NULL,
  `plafond` decimal(19,2) NOT NULL,
  `statut` enum('ACTIF','ARCHIVE','EXPIRE') NOT NULL,
  `organisme_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `contrat_cadre_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_debut` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `plafond` decimal(19,2) DEFAULT NULL,
  `statut` enum('ACTIF','ARCHIVE','EXPIRE') DEFAULT NULL,
  `organisme_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `contrat_leasing` (
  `id` bigint NOT NULL,
  `completed` bit(1) NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_debut` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `immobilisation` varchar(255) NOT NULL,
  `montant_achat` decimal(38,2) NOT NULL,
  `numero_contrat` varchar(255) NOT NULL,
  `periode_mois` int NOT NULL,
  `type_contrat` enum('DEGRESSIF','LINEAIRE','PROGRESSIF') NOT NULL,
  `valeur_residuelle` decimal(38,2) DEFAULT NULL,
  `banque_id` bigint NOT NULL,
  `date_upload_pdf` date DEFAULT NULL,
  `fichier_pdf_contenu` longblob DEFAULT NULL,
  `nom_fichier_pdf` varchar(255) DEFAULT NULL,
  `contrat_cadre_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `contrat_leasing_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `completed` bit(1) DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_debut` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `immobilisation` varchar(255) DEFAULT NULL,
  `montant_achat` decimal(38,2) DEFAULT NULL,
  `numero_contrat` varchar(255) DEFAULT NULL,
  `periode_mois` int DEFAULT NULL,
  `type_contrat` enum('DEGRESSIF','LINEAIRE','PROGRESSIF') DEFAULT NULL,
  `valeur_residuelle` decimal(38,2) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `organisme_leasing_id` bigint DEFAULT NULL,
  `contrat_cadre_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `contrat_leasing_phase` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_modification` datetime(6) NOT NULL,
  `duree_mois` int NOT NULL,
  `mensualite_ttc` decimal(38,2) NOT NULL,
  `numero_phase` int NOT NULL,
  `contrat_leasing_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `contrat_leasing_phase_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `duree_mois` int DEFAULT NULL,
  `mensualite_ttc` decimal(38,2) DEFAULT NULL,
  `numero_phase` int DEFAULT NULL,
  `contrat_leasing_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `credit_moyen_terme` (
  `id` bigint NOT NULL,
  `completed` bit(1) NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_debut` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `designation` varchar(255) NOT NULL,
  `duree` int NOT NULL,
  `echeance` decimal(38,2) NOT NULL,
  `nominal` decimal(38,2) NOT NULL,
  `periodicite` int NOT NULL,
  `taux` decimal(38,2) NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `credit_moyen_terme_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `completed` bit(1) DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_debut` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `designation` varchar(255) DEFAULT NULL,
  `duree` int DEFAULT NULL,
  `echeance` decimal(38,2) DEFAULT NULL,
  `nominal` decimal(38,2) DEFAULT NULL,
  `periodicite` int DEFAULT NULL,
  `taux` decimal(38,2) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `decompte` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `marche_id` bigint DEFAULT NULL,
  `banque_encaissement_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `decompte_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_encaissement_id` bigint DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `department` (
  `id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `department_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `name` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `droit_constate` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(38,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DEBLOCAGE','REMBOURSEMENT') DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_specifique_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `droit_constate_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(38,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DEBLOCAGE','REMBOURSEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_specifique_id` bigint DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `echeance_cmt` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) NOT NULL,
  `numero_echeance` int NOT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut_paiement` enum('PAYE','PREVU') NOT NULL,
  `credit_moyen_terme_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `echeance_cmt_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `numero_echeance` int DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut_paiement` enum('PAYE','PREVU') DEFAULT NULL,
  `credit_moyen_terme_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `echeance_leasing` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant_ttc` decimal(19,2) NOT NULL,
  `numero_echeance` int NOT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut_paiement` enum('PAYE','PREVU') NOT NULL,
  `contrat_leasing_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `echeance_leasing_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant_ttc` decimal(19,2) DEFAULT NULL,
  `numero_echeance` int DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut_paiement` enum('PAYE','PREVU') DEFAULT NULL,
  `contrat_leasing_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `factoring` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(38,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DEBLOCAGE','REMBOURSEMENT') DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_specifique_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `factoring_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(38,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DEBLOCAGE','REMBOURSEMENT') DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_specifique_id` bigint DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `frais_bancaire` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `frais_bancaire_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `impayee` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(500) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `motif` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','INTERETS','LCN','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `numero` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `impayee_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(500) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `motif` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `numero` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `impot_et_taxe` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(500) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `motif` enum('ADII','AUTRE_ASSURANCE','COMMUNAL','DGI','IR','IS','PENALITE_DE_RETARD','RETENUE_A_LA_SOURCE','TAXE_PROFESSIONNELLE','TVA','TVA_COMMISSION_CAUTION','VIGNETTE') NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL,
  `caution_id` bigint DEFAULT NULL,
  `commission_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `impot_et_taxe_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(500) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `motif` enum('ADII','AUTRE_ASSURANCE','COMMUNAL','DGI','IR','IS','PENALITE_DE_RETARD','RETENUE_A_LA_SOURCE','TAXE_PROFESSIONNELLE','TVA','TVA_COMMISSION_CAUTION','VIGNETTE') DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `caution_id` bigint DEFAULT NULL,
  `commission_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `lcns` (
  `id` bigint NOT NULL,
  `beneficiaire` varchar(255) NOT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) NOT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint NOT NULL,
  `nature_lcn` enum('ENCAISSEMENT','ESCOMPTE') DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') NOT NULL,
  `id_paiement` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `lcns_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `beneficiaire` varchar(255) DEFAULT NULL,
  `date_echeance` date DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `id_paiement` varchar(50) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `nature_lcn` enum('ENCAISSEMENT','ESCOMPTE') DEFAULT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `leasing_config` (
  `id` bigint NOT NULL,
  `date_modification` datetime(6) NOT NULL,
  `taux_tva` decimal(6,4) NOT NULL,
  `valeur_residuelle` decimal(6,4) NOT NULL,
  `alerte_echeance_jours_court_terme` int NOT NULL,
  `alerte_echeance_jours_long_terme` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `leasing_config_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `taux_tva` decimal(6,4) DEFAULT NULL,
  `valeur_residuelle` decimal(6,4) DEFAULT NULL,
  `alerte_echeance_jours_court_terme` int DEFAULT NULL,
  `alerte_echeance_jours_long_terme` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle` (
  `id` bigint NOT NULL,
  `actif` bit(1) NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_depart` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `plafond` decimal(38,2) DEFAULT NULL,
  `taux` decimal(38,2) NOT NULL,
  `banque_id` bigint NOT NULL,
  `nature_ligne_id` bigint NOT NULL,
  `ligne_mere_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `actif` bit(1) DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_depart` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `plafond` decimal(38,2) DEFAULT NULL,
  `taux` decimal(38,2) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `ligne_mere_id` bigint DEFAULT NULL,
  `nature_ligne_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_config` (
  `id` bigint NOT NULL,
  `alert_threshold_months` int NOT NULL,
  `date_modification` datetime(6) NOT NULL,
  `warning_threshold_months` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_config_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `alert_threshold_months` int DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `warning_threshold_months` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_marche` (
  `ligne_id` bigint NOT NULL,
  `marche_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_marche_aud` (
  `rev` int NOT NULL,
  `ligne_id` bigint NOT NULL,
  `marche_id` bigint NOT NULL,
  `revtype` tinyint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_specifique` (
  `id` bigint NOT NULL,
  `actif` bit(1) NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_depart` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `date_modification` datetime(6) NOT NULL,
  `plafond` decimal(38,2) NOT NULL,
  `taux` decimal(38,2) NOT NULL,
  `marche_id` bigint NOT NULL,
  `nature_ligne_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `ligne_fonctionnelle_specifique_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `actif` bit(1) DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_depart` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `plafond` decimal(38,2) DEFAULT NULL,
  `taux` decimal(38,2) DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL,
  `nature_ligne_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `log` (
  `id` bigint NOT NULL,
  `entity_id` bigint NOT NULL,
  `entity_name` varchar(255) NOT NULL,
  `operation_type` varchar(255) NOT NULL,
  `timestamp` datetime(6) NOT NULL,
  `user_name` varchar(255) NOT NULL,
  `revision_number` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `log_entry_detail` (
  `id` bigint NOT NULL,
  `field_name` varchar(255) NOT NULL,
  `new_value` text DEFAULT NULL,
  `old_value` text DEFAULT NULL,
  `log_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `marches` (
  `id` bigint NOT NULL,
  `created_at` date NOT NULL,
  `date_d_achievement_des_travaux` date DEFAULT NULL,
  `date_debut_des_travaux` date DEFAULT NULL,
  `maitre_douvrage` varchar(255) NOT NULL,
  `modified_at` date NOT NULL,
  `montant` decimal(19,2) NOT NULL,
  `nom` varchar(255) NOT NULL,
  `numero` varchar(50) NOT NULL,
  `banque_id` bigint NOT NULL,
  `actif` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `marches_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `actif` bit(1) DEFAULT NULL,
  `created_at` date DEFAULT NULL,
  `date_d_achievement_des_travaux` date DEFAULT NULL,
  `date_debut_des_travaux` date DEFAULT NULL,
  `maitre_douvrage` varchar(255) DEFAULT NULL,
  `modified_at` date DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `nom` varchar(255) DEFAULT NULL,
  `numero` varchar(50) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `mises_a_disposition` (
  `id` bigint NOT NULL,
  `beneficiaire` varchar(255) NOT NULL,
  `cin_beneficiaire` varchar(255) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) NOT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint NOT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') NOT NULL,
  `id_paiement` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `mises_a_disposition_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `beneficiaire` varchar(255) DEFAULT NULL,
  `cin_beneficiaire` varchar(255) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `id_paiement` varchar(50) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `nature_ligne_fonctionnelle` (
  `id` bigint NOT NULL,
  `code` varchar(20) NOT NULL,
  `date_creation` date NOT NULL,
  `date_modification` date NOT NULL,
  `description` varchar(255) NOT NULL,
  `label` varchar(255) NOT NULL,
  `portee` enum('GENERALE','GENERALE_ET_SPECIFIQUE','SPECIFIQUE') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `nature_ligne_fonctionnelle_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `code` varchar(20) DEFAULT NULL,
  `date_creation` date DEFAULT NULL,
  `date_modification` date DEFAULT NULL,
  `description` varchar(255) DEFAULT NULL,
  `label` varchar(255) DEFAULT NULL,
  `portee` enum('GENERALE','GENERALE_ET_SPECIFIQUE','SPECIFIQUE') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notifications` (
  `id` bigint NOT NULL,
  `content` text DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `type` enum('ALERT','INFO','SUCCESS','WARNING') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `notifications_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `content` text DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `type` enum('ALERT','INFO','SUCCESS','WARNING') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `operation_errone` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `motif` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `operation_errone_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `motif` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `organisme_leasing` (
  `id` bigint NOT NULL,
  `date_creation` datetime(6) NOT NULL,
  `date_modification` datetime(6) NOT NULL,
  `nom` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `organisme_leasing_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_creation` datetime(6) DEFAULT NULL,
  `date_debut` date DEFAULT NULL,
  `date_fin` date DEFAULT NULL,
  `date_modification` datetime(6) DEFAULT NULL,
  `nom` varchar(255) DEFAULT NULL,
  `plafond` decimal(38,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `prefinancement` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(38,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DEBLOCAGE','REMBOURSEMENT') DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_specifique_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `prefinancement_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(38,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `type` enum('DEBLOCAGE','REMBOURSEMENT') DEFAULT NULL,
  `ligne_fonctionnelle_id` bigint DEFAULT NULL,
  `ligne_fonctionnelle_specifique_id` bigint DEFAULT NULL,
  `marche_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `prelevement` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `prelevement_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `provision` (
  `id` bigint NOT NULL,
  `date_emission` date NOT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) NOT NULL,
  `montant` decimal(19,4) NOT NULL,
  `motif` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') NOT NULL,
  `observation` varchar(500) NOT NULL,
  `sens` enum('DECAISSEMENT','ENCAISSEMENT') NOT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `provision_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `motif` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `sens` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rapprochement` (
  `id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `date_rapprochement` varbinary(255) NOT NULL,
  `solde_debut` decimal(38,2) DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `banque_id` bigint NOT NULL,
  `statut` enum('EN_COURS','CLOTURE_PROVISOIRE','CLOTURE_DEFINITIVE') NOT NULL DEFAULT 'EN_COURS'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `rapprochement_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `date_rapprochement` varbinary(255) DEFAULT NULL,
  `solde_debut` decimal(38,2) DEFAULT NULL,
  `statut` enum('CLOTURE_DEFINITIVE','CLOTURE_PROVISOIRE','EN_COURS') DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `restitution` (
  `id` bigint NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) NOT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') NOT NULL,
  `banque_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `restitution_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(150) DEFAULT NULL,
  `montant` decimal(19,4) DEFAULT NULL,
  `observation` varchar(500) DEFAULT NULL,
  `status` enum('PAYE','PREVU') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `transaction` (
  `id` bigint NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `credit` decimal(19,2) NOT NULL,
  `date_emission` date NOT NULL,
  `date_operations` date NOT NULL,
  `date_valeur` date NOT NULL,
  `debit` decimal(19,2) NOT NULL,
  `libelle` longtext NOT NULL,
  `observation` varchar(255) NOT NULL,
  `transaction_source` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `tva_collectee` decimal(38,2) NOT NULL,
  `tva_deductible` decimal(38,2) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `banque_id` bigint NOT NULL,
  `rapprochement_id` bigint NOT NULL,
  `referent` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `transaction_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `credit` decimal(19,2) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operations` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `debit` decimal(19,2) DEFAULT NULL,
  `libelle` longtext DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `referent` bigint DEFAULT NULL,
  `transaction_source` enum('AGIOS','CAUTION','CHEQUE','CMT','COMMISSION','DECOMPTES','DROIT_CONSTATE','FACTORING','FRAIS_BANCAIRE','IMPAYEE','IMPOT_TAXE','LCN','LCN_DECAISSEMENT','LCN_ENCAISSEMENT','LCN_ESCOMPTE','LEASING','MiseADisposition','OPERATION_ERRONE','PREFINANCEMENT','PRELEVEMENT','PROVISION','RESTITUTION','VIREMENT_INTERNATIONAL','Virement') DEFAULT NULL,
  `tva_collectee` decimal(38,2) DEFAULT NULL,
  `tva_deductible` decimal(38,2) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL,
  `rapprochement_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL,
  `enabled` bit(1) NOT NULL,
  `password` varchar(255) DEFAULT NULL,
  `username` varchar(50) NOT NULL,
  `is_system_admin` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `users_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  `is_system_admin` bit(1) DEFAULT NULL,
  `username` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_department_role` (
  `id` bigint NOT NULL,
  `role_in_dept` enum('DIRECTOR','MEMBER') NOT NULL,
  `department_id` bigint NOT NULL,
  `user_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_department_role_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `role_in_dept` enum('DIRECTOR','MEMBER') DEFAULT NULL,
  `department_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_notification_status` (
  `id` bigint NOT NULL,
  `is_read` bit(1) NOT NULL,
  `notification_id` bigint NOT NULL,
  `user_id` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `user_notification_status_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `is_read` bit(1) DEFAULT NULL,
  `notification_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `virements` (
  `id` bigint NOT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `beneficiaire` varchar(255) NOT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) NOT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `rib_beneficiaire` varchar(255) DEFAULT NULL,
  `banque_emettrice_id` bigint DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_beneficiare_id` bigint DEFAULT NULL,
  `nom_banque_beneficiaire_libre` varchar(255) DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') NOT NULL,
  `id_paiement` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `virements_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `beneficiaire` varchar(255) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `id_paiement` varchar(50) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `nom_banque_beneficiaire_libre` varchar(255) DEFAULT NULL,
  `numero` varchar(255) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `rib_beneficiaire` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_beneficiare_id` bigint DEFAULT NULL,
  `banque_emettrice_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `virements_internationaux` (
  `id` bigint NOT NULL,
  `beneficiaire` varchar(255) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) NOT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') NOT NULL,
  `banque_id` bigint NOT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE `virements_internationaux_aud` (
  `id` bigint NOT NULL,
  `rev` int NOT NULL,
  `revtype` tinyint DEFAULT NULL,
  `beneficiaire` varchar(255) DEFAULT NULL,
  `date_emission` date DEFAULT NULL,
  `date_operation` date DEFAULT NULL,
  `date_valeur` date DEFAULT NULL,
  `emetteur` varchar(255) DEFAULT NULL,
  `libelle` varchar(255) DEFAULT NULL,
  `montant` decimal(19,2) DEFAULT NULL,
  `observation` varchar(255) DEFAULT NULL,
  `statut` enum('ANNULE','ENCOURS','REGLE') DEFAULT NULL,
  `type` enum('DECAISSEMENT','ENCAISSEMENT') DEFAULT NULL,
  `banque_id` bigint DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ── Spring Batch (schema-mysql.sql officiel) ────────────────────────────────

-- Autogenerated: do not edit this file

CREATE TABLE BATCH_JOB_INSTANCE  (
	JOB_INSTANCE_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT ,
	JOB_NAME VARCHAR(100) NOT NULL,
	JOB_KEY VARCHAR(32) NOT NULL,
	constraint JOB_INST_UN unique (JOB_NAME, JOB_KEY)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION  (
	JOB_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT  ,
	JOB_INSTANCE_ID BIGINT NOT NULL,
	CREATE_TIME DATETIME(6) NOT NULL,
	START_TIME DATETIME(6) DEFAULT NULL ,
	END_TIME DATETIME(6) DEFAULT NULL ,
	STATUS VARCHAR(10) ,
	EXIT_CODE VARCHAR(2500) ,
	EXIT_MESSAGE VARCHAR(2500) ,
	LAST_UPDATED DATETIME(6),
	constraint JOB_INST_EXEC_FK foreign key (JOB_INSTANCE_ID)
	references BATCH_JOB_INSTANCE(JOB_INSTANCE_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_PARAMS  (
	JOB_EXECUTION_ID BIGINT NOT NULL ,
	PARAMETER_NAME VARCHAR(100) NOT NULL ,
	PARAMETER_TYPE VARCHAR(100) NOT NULL ,
	PARAMETER_VALUE VARCHAR(2500) ,
	IDENTIFYING CHAR(1) NOT NULL ,
	constraint JOB_EXEC_PARAMS_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION  (
	STEP_EXECUTION_ID BIGINT  NOT NULL PRIMARY KEY ,
	VERSION BIGINT NOT NULL,
	STEP_NAME VARCHAR(100) NOT NULL,
	JOB_EXECUTION_ID BIGINT NOT NULL,
	CREATE_TIME DATETIME(6) NOT NULL,
	START_TIME DATETIME(6) DEFAULT NULL ,
	END_TIME DATETIME(6) DEFAULT NULL ,
	STATUS VARCHAR(10) ,
	COMMIT_COUNT BIGINT ,
	READ_COUNT BIGINT ,
	FILTER_COUNT BIGINT ,
	WRITE_COUNT BIGINT ,
	READ_SKIP_COUNT BIGINT ,
	WRITE_SKIP_COUNT BIGINT ,
	PROCESS_SKIP_COUNT BIGINT ,
	ROLLBACK_COUNT BIGINT ,
	EXIT_CODE VARCHAR(2500) ,
	EXIT_MESSAGE VARCHAR(2500) ,
	LAST_UPDATED DATETIME(6),
	constraint JOB_EXEC_STEP_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_CONTEXT  (
	STEP_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint STEP_EXEC_CTX_FK foreign key (STEP_EXECUTION_ID)
	references BATCH_STEP_EXECUTION(STEP_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_JOB_EXECUTION_CONTEXT  (
	JOB_EXECUTION_ID BIGINT NOT NULL PRIMARY KEY,
	SHORT_CONTEXT VARCHAR(2500) NOT NULL,
	SERIALIZED_CONTEXT TEXT ,
	constraint JOB_EXEC_CTX_FK foreign key (JOB_EXECUTION_ID)
	references BATCH_JOB_EXECUTION(JOB_EXECUTION_ID)
) ENGINE=InnoDB;

CREATE TABLE BATCH_STEP_EXECUTION_SEQ (
	ID BIGINT NOT NULL,
	UNIQUE_KEY CHAR(1) NOT NULL,
	constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_STEP_EXECUTION_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_STEP_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_EXECUTION_SEQ (
	ID BIGINT NOT NULL,
	UNIQUE_KEY CHAR(1) NOT NULL,
	constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_JOB_EXECUTION_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_JOB_EXECUTION_SEQ);

CREATE TABLE BATCH_JOB_SEQ (
	ID BIGINT NOT NULL,
	UNIQUE_KEY CHAR(1) NOT NULL,
	constraint UNIQUE_KEY_UN unique (UNIQUE_KEY)
) ENGINE=InnoDB;

INSERT INTO BATCH_JOB_SEQ (ID, UNIQUE_KEY) select * from (select 0 as ID, '0' as UNIQUE_KEY) as tmp where not exists(select * from BATCH_JOB_SEQ);

-- ── Clés primaires et index ────────────────────────────────────────────────

ALTER TABLE `agios`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK8dp1ltfv7wr5al9sipy4b0107` (`banque_id`),
  ADD KEY `idx_agios_banque_id` (`banque_id`),
  ADD KEY `idx_agios_status` (`status`),
  ADD KEY `idx_agios_date_emission` (`date_emission`);

ALTER TABLE `agios_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `agios_journalier`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK2en0pwll1dsulm2pxdeoqpqqj` (`banque_id`),
  ADD KEY `FK8qjsgqh7e4d3jgearhns9uc4c` (`trimestre_id`),
  ADD KEY `FK7bbq8xylg7nqtokn9418mpyjl` (`marche_id`),
  ADD KEY `idx_agios_jour_banque_id` (`banque_id`),
  ADD KEY `idx_agios_jour_type_agios` (`type_agios`),
  ADD KEY `idx_agios_jour_marche_id` (`marche_id`),
  ADD KEY `idx_agios_jour_trimestre_id` (`trimestre_id`);

ALTER TABLE `agios_trimestriel`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_trim_banque_type` (`trimestre`,`annee`,`banque_id`,`type_agios`),
  ADD KEY `FKcaaqqxmwbl0owg7ik3jik463f` (`banque_id`),
  ADD KEY `idx_agios_trim_banque_id` (`banque_id`),
  ADD KEY `idx_agios_trim_statut` (`statut`),
  ADD KEY `idx_agios_trim_type_agios` (`type_agios`);

ALTER TABLE `agios_trimestriel_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `audit_revision_entity`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `banque`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKcpq92gr9qh03fpotihs7c4o36` (`numero_compte`),
  ADD KEY `idx_banque_numero_compte` (`numero_compte`),
  ADD KEY `idx_banque_nom` (`nom`),
  ADD KEY `idx_banque_devise` (`devise`);

ALTER TABLE `banque_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `cautions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKhyu23nydp8io2sdo93r05c9b3` (`numero_caution`),
  ADD KEY `idx_caution_numero` (`numero_caution`),
  ADD KEY `idx_caution_banque_id` (`banque_id`),
  ADD KEY `idx_caution_nature_id` (`nature_id`),
  ADD KEY `idx_caution_marche_id` (`marche_id`),
  ADD KEY `idx_caution_actif` (`actif`);

ALTER TABLE `cautions_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `caution_commission_config`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKl4e55wle5qily8ef4lcto6ujq` (`banque_id`),
  ADD KEY `idx_caution_commission_config_banque` (`banque_id`);

ALTER TABLE `caution_commission_config_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `charge_fixe_config`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_charge_fixe_config_banque` (`banque_id`);

ALTER TABLE `charge_fixe_config_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `cheques`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKtnisvdwv1jajjvlkxr1hgst00` (`id_paiement`),
  ADD KEY `FKq2rbj2rkjv05tv4kf017gtq4c` (`banque_id`),
  ADD KEY `idx_cheque_numero` (`numero`),
  ADD KEY `idx_cheque_banque_id` (`banque_id`),
  ADD KEY `idx_cheque_statut` (`statut`),
  ADD KEY `idx_cheque_date_emission` (`date_emission`),
  ADD KEY `idx_cheque_type` (`type`);

ALTER TABLE `cheques_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `cmt_config`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `cmt_config_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `commission`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK42aedk4qsl3t0vr0hc0nv04ce` (`impot_et_taxe_id`),
  ADD UNIQUE KEY `UK1o3l2nb0om5ayp77468297hay` (`virement_id`),
  ADD KEY `FKpcarewovrseewylrm6pasqhvi` (`banque_id`),
  ADD KEY `FKfjravd1r62rantee2guiamn7o` (`caution_id`);

ALTER TABLE `commission_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `commission_config`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_commission_config_banque_type` (`banque_id`,`type`),
  ADD KEY `idx_commission_config_condition` (`commission_condition`);

ALTER TABLE `commission_config_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `contrat_cadre`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_contrat_cadre_organisme_id` (`organisme_id`),
  ADD KEY `idx_contrat_cadre_statut` (`statut`);

ALTER TABLE `contrat_cadre_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `contrat_leasing`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKquxa2lcg2h174r3tsgv26vhx9` (`numero_contrat`),
  ADD KEY `FKd4e8mee7l3jbkf45oyrikpiv9` (`banque_id`),
  ADD KEY `idx_contrat_leasing_banque_id` (`banque_id`),
  ADD KEY `idx_contrat_leasing_type_contrat` (`type_contrat`),
  ADD KEY `idx_contrat_leasing_completed` (`completed`),
  ADD KEY `idx_contrat_leasing_contrat_cadre` (`contrat_cadre_id`);

ALTER TABLE `contrat_leasing_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `contrat_leasing_phase`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK13xw8kjqibumbr40kcta8g8bv` (`contrat_leasing_id`),
  ADD KEY `idx_contrat_leasing_phase_contrat_id` (`contrat_leasing_id`);

ALTER TABLE `contrat_leasing_phase_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `credit_moyen_terme`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKb63rivvpq4cymn7y4eb73m3vt` (`banque_id`),
  ADD KEY `idx_credit_moyen_terme_banque_id` (`banque_id`),
  ADD KEY `idx_credit_moyen_terme_completed` (`completed`),
  ADD KEY `idx_credit_moyen_terme_date_debut` (`date_debut`);

ALTER TABLE `credit_moyen_terme_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `decompte`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKb5a1qt9el4g26drbbb198f3d8` (`marche_id`),
  ADD KEY `FKk2nh68eny65q5m6qip5h516ee` (`banque_encaissement_id`),
  ADD KEY `idx_decompte_marche_id` (`marche_id`),
  ADD KEY `idx_decompte_banque_encaissement_id` (`banque_encaissement_id`),
  ADD KEY `idx_decompte_status` (`status`),
  ADD KEY `idx_decompte_date_valeur` (`date_valeur`);

ALTER TABLE `decompte_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `department`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK1t68827l97cwyxo9r1u6t4p7d` (`name`),
  ADD KEY `idx_department_name` (`name`);

ALTER TABLE `department_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `droit_constate`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKjw3k1pq8jnouir3s3a2jtj6le` (`marche_id`),
  ADD KEY `FKpp56khgk9dxbabpganxxft8hk` (`banque_id`),
  ADD KEY `FKopkuan9rishe55yx22lfly5i1` (`ligne_fonctionnelle_id`),
  ADD KEY `FKea8xrqwfie3oyra5v18oyjfxc` (`ligne_fonctionnelle_specifique_id`),
  ADD KEY `idx_droit_constate_marche_id` (`marche_id`),
  ADD KEY `idx_droit_constate_banque_id` (`banque_id`),
  ADD KEY `idx_droit_constate_status` (`status`),
  ADD KEY `idx_droit_constate_type` (`type`),
  ADD KEY `idx_droit_constate_date_valeur` (`date_valeur`);

ALTER TABLE `droit_constate_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `echeance_cmt`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK3j02dbgpdkqg816lyke49181h` (`credit_moyen_terme_id`),
  ADD KEY `idx_echeance_cmt_credit_moyen_terme_id` (`credit_moyen_terme_id`),
  ADD KEY `idx_echeance_cmt_statut_paiement` (`statut_paiement`),
  ADD KEY `idx_echeance_cmt_date_echeance` (`date_echeance`);

ALTER TABLE `echeance_cmt_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `echeance_leasing`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK6lqt5kw7ekfm8wvof6u8y0ev6` (`contrat_leasing_id`),
  ADD KEY `idx_echeance_leasing_contrat_id` (`contrat_leasing_id`),
  ADD KEY `idx_echeance_leasing_statut_paiement` (`statut_paiement`),
  ADD KEY `idx_echeance_leasing_date_echeance` (`date_echeance`);

ALTER TABLE `echeance_leasing_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `factoring`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK1owbcg4rihh8wxybc5hcq579t` (`marche_id`),
  ADD KEY `FKd0rjg0rw9pvlax83l5gqns1kx` (`ligne_fonctionnelle_id`),
  ADD KEY `FK4nf0q1l16ibka9g7wmlwkd1sg` (`ligne_fonctionnelle_specifique_id`),
  ADD KEY `idx_factoring_marche_id` (`marche_id`),
  ADD KEY `idx_factoring_status` (`status`),
  ADD KEY `idx_factoring_type` (`type`),
  ADD KEY `idx_factoring_date_valeur` (`date_valeur`);

ALTER TABLE `factoring_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `frais_bancaire`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK3b0mmyf6mm94h3hubxjru0tel` (`banque_id`);

ALTER TABLE `frais_bancaire_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `impayee`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKeylp2fflkjxwguefd0kvq0adq` (`banque_id`);

ALTER TABLE `impayee_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `impot_et_taxe`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_impot_commission` (`commission_id`),
  ADD KEY `FK8nkpfqmwev9cdjielt19vj9bh` (`banque_id`),
  ADD KEY `FK6822r70ni2xoue7nno1thgo74` (`caution_id`),
  ADD KEY `FKl9stm0or019wgwv9u6ngdv6md` (`commission_id`),
  ADD KEY `idx_impot_et_taxe_banque_id` (`banque_id`),
  ADD KEY `idx_impot_et_taxe_status` (`status`),
  ADD KEY `idx_impot_et_taxe_motif` (`motif`),
  ADD KEY `idx_impot_et_taxe_date_emission` (`date_emission`);

ALTER TABLE `impot_et_taxe_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `lcns`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKe3a3d4e6rvry71q5raqavro8q` (`id_paiement`),
  ADD KEY `FKbgoif52n0bs32q15oncvf1p93` (`banque_id`),
  ADD KEY `FK9ph6p1cyjg4nsc59iucorafw3` (`ligne_fonctionnelle_id`),
  ADD KEY `idx_lcn_numero` (`numero`),
  ADD KEY `idx_lcn_banque_id` (`banque_id`),
  ADD KEY `idx_lcn_statut` (`statut`),
  ADD KEY `idx_lcn_date_echeance` (`date_echeance`),
  ADD KEY `idx_lcn_type` (`type`),
  ADD KEY `idx_lcn_nature_lcn` (`nature_lcn`);

ALTER TABLE `lcns_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `leasing_config`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `leasing_config_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `ligne_fonctionnelle`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKiwfx893ms6t4s0r8p4p212659` (`banque_id`),
  ADD KEY `FK950ndr0000osm83ob87lq47dx` (`nature_ligne_id`),
  ADD KEY `FKem7x3v915h4okb2cf03c9lsh2` (`ligne_mere_id`),
  ADD KEY `idx_lf_banque_id` (`banque_id`),
  ADD KEY `idx_lf_nature_id` (`nature_ligne_id`),
  ADD KEY `idx_lf_actif` (`actif`),
  ADD KEY `idx_lf_date_depart` (`date_depart`),
  ADD KEY `idx_lf_ligne_mere_id` (`ligne_mere_id`);

ALTER TABLE `ligne_fonctionnelle_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `ligne_fonctionnelle_config`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `ligne_fonctionnelle_config_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `ligne_fonctionnelle_marche`
  ADD KEY `FKc7q78fxx5wpwn2owjqkd0pctg` (`marche_id`),
  ADD KEY `FKmehqwt8xrb101jp3kcknmox04` (`ligne_id`);

ALTER TABLE `ligne_fonctionnelle_marche_aud`
  ADD PRIMARY KEY (`ligne_id`,`rev`,`marche_id`),
  ADD KEY `FKanhk1lirn1g1sidf0oo6kbbd3` (`rev`);

ALTER TABLE `ligne_fonctionnelle_specifique`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK57lk2ydnkt1um5o98h1cfri81` (`marche_id`),
  ADD KEY `FKlfodudydwmhtqocgajmo862e9` (`nature_ligne_id`),
  ADD KEY `idx_lfs_nature_id` (`nature_ligne_id`),
  ADD KEY `idx_lfs_marche_id` (`marche_id`),
  ADD KEY `idx_lfs_actif` (`actif`);

ALTER TABLE `ligne_fonctionnelle_specifique_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `log`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `log_entry_detail`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKqyda0kxkktunv8kyalky6cr7k` (`log_id`);

ALTER TABLE `marches`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK6lt7sqmf282e9y0vvjry6wj2u` (`numero`),
  ADD KEY `FKcpi3mv9yhm6gs9b22qxvcedt5` (`banque_id`),
  ADD KEY `idx_marche_actif` (`actif`),
  ADD KEY `idx_marche_banque_id` (`banque_id`);

ALTER TABLE `marches_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `mises_a_disposition`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKgw080va9la2rtwk668g48q48c` (`id_paiement`),
  ADD KEY `FK2mwhxb2rlme2h916yggaga3ku` (`banque_id`),
  ADD KEY `idx_mise_a_dispo_numero` (`numero`),
  ADD KEY `idx_mise_a_dispo_banque_id` (`banque_id`),
  ADD KEY `idx_mise_a_dispo_statut` (`statut`),
  ADD KEY `idx_mise_a_dispo_date_emission` (`date_emission`),
  ADD KEY `idx_mise_a_dispo_type` (`type`);

ALTER TABLE `mises_a_disposition_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `nature_ligne_fonctionnelle`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKsjnhtqaim07sqlmq7cpetwvx0` (`label`),
  ADD KEY `idx_nlf_code` (`code`),
  ADD KEY `idx_nlf_label` (`label`),
  ADD KEY `idx_nlf_portee` (`portee`);

ALTER TABLE `nature_ligne_fonctionnelle_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `notifications`
  ADD PRIMARY KEY (`id`);

ALTER TABLE `notifications_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `operation_errone`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK64gpla2wkwxqxikq0i1l9oita` (`banque_id`);

ALTER TABLE `operation_errone_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `organisme_leasing`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_organisme_leasing_nom` (`nom`);

ALTER TABLE `organisme_leasing_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `prefinancement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK4g5mv670pd9jwp2hnw2phpk4e` (`marche_id`),
  ADD KEY `FKtilv0u1346pt55815uscwjm4s` (`ligne_fonctionnelle_id`),
  ADD KEY `FKo0yv0j8c6jdvg4hl9l5l4s16g` (`ligne_fonctionnelle_specifique_id`),
  ADD KEY `idx_prefinancement_marche_id` (`marche_id`),
  ADD KEY `idx_prefinancement_status` (`status`),
  ADD KEY `idx_prefinancement_type` (`type`),
  ADD KEY `idx_prefinancement_date_valeur` (`date_valeur`);

ALTER TABLE `prefinancement_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `prelevement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKql7rimltc1kmokjmhxpm0spq4` (`banque_id`),
  ADD KEY `idx_prelevement_banque_id` (`banque_id`),
  ADD KEY `idx_prelevement_status` (`status`),
  ADD KEY `idx_prelevement_date_emission` (`date_emission`);

ALTER TABLE `prelevement_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `provision`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKglsjrhgrwtqef5sja7cbb4an9` (`banque_id`);

ALTER TABLE `provision_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `rapprochement`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKspq7j7jtjyesk6b0gsoejwnt1` (`banque_id`),
  ADD KEY `idx_rapprochement_banque_id` (`banque_id`),
  ADD KEY `idx_rapprochement_statut` (`statut`),
  ADD KEY `idx_rapprochement_date` (`date_rapprochement`),
  ADD KEY `idx_rapprochement_banque_statut` (`banque_id`,`statut`);

ALTER TABLE `rapprochement_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `restitution`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK9gwtf8ws5rn2rlxlfac6gpnar` (`banque_id`);

ALTER TABLE `restitution_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `transaction`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK6q4n22db62ovn9vtvdg547quo` (`banque_id`),
  ADD KEY `FK6clh3e3bqnc90kq02lu0j97fw` (`rapprochement_id`),
  ADD KEY `idx_transaction_banque_id` (`banque_id`),
  ADD KEY `idx_transaction_rapprochement_id` (`rapprochement_id`),
  ADD KEY `idx_transaction_source` (`transaction_source`),
  ADD KEY `idx_transaction_date_operations` (`date_operations`),
  ADD KEY `idx_transaction_date_emission` (`date_emission`);

ALTER TABLE `transaction_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKr43af9ap4edm43mmtq01oddj6` (`username`),
  ADD KEY `idx_user_username` (`username`),
  ADD KEY `idx_user_system_admin` (`is_system_admin`);

ALTER TABLE `users_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `user_department_role`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uq_user_dept` (`user_id`,`department_id`),
  ADD KEY `FK1upufonuoyy28tjjom0qd8gt7` (`department_id`),
  ADD KEY `idx_user_department_role_department_id` (`department_id`),
  ADD KEY `idx_user_department_role_user_id` (`user_id`);

ALTER TABLE `user_department_role_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `user_notification_status`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UKiooau267w40m8gtql5vsvww5x` (`notification_id`,`user_id`),
  ADD KEY `FK9wdicp7t7t9gydc73ii1eweg7` (`user_id`);

ALTER TABLE `user_notification_status_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `virements`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `UK8bln0qc1bui967nkl1pc2cgkf` (`id_paiement`),
  ADD KEY `FKchrb8hfylc0grt3iy6xk21icw` (`banque_emettrice_id`),
  ADD KEY `FKtanl65cwb33wvme2wrqrjifh3` (`banque_beneficiare_id`),
  ADD KEY `idx_virement_numero` (`numero`),
  ADD KEY `idx_virement_banque_emettrice_id` (`banque_emettrice_id`),
  ADD KEY `idx_virement_banque_beneficiare_id` (`banque_beneficiare_id`),
  ADD KEY `idx_virement_statut` (`statut`),
  ADD KEY `idx_virement_date_emission` (`date_emission`),
  ADD KEY `idx_virement_type` (`type`);

ALTER TABLE `virements_aud`
  ADD PRIMARY KEY (`rev`,`id`);

ALTER TABLE `virements_internationaux`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FKl8dgtbsabohwidglfuonv0dxb` (`banque_id`),
  ADD KEY `idx_virement_intl_banque_id` (`banque_id`),
  ADD KEY `idx_virement_intl_statut` (`statut`),
  ADD KEY `idx_virement_intl_date_emission` (`date_emission`),
  ADD KEY `idx_virement_intl_type` (`type`);

ALTER TABLE `virements_internationaux_aud`
  ADD PRIMARY KEY (`rev`,`id`);

-- ── Auto-incréments ────────────────────────────────────────────────────────

ALTER TABLE `agios`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `agios_journalier`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `agios_trimestriel`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `banque`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `cautions`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `caution_commission_config`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `charge_fixe_config`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `cheques`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `commission`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `commission_config`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `contrat_cadre`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `contrat_leasing`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `contrat_leasing_phase`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `credit_moyen_terme`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `decompte`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `department`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `droit_constate`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `echeance_cmt`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `echeance_leasing`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `factoring`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `frais_bancaire`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `impayee`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `impot_et_taxe`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `lcns`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `ligne_fonctionnelle`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `ligne_fonctionnelle_specifique`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `log`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `log_entry_detail`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `marches`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `mises_a_disposition`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `nature_ligne_fonctionnelle`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `notifications`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `operation_errone`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `organisme_leasing`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `prefinancement`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `prelevement`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `provision`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `rapprochement`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `restitution`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `transaction`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `users`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `user_department_role`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `user_notification_status`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `virements`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

ALTER TABLE `virements_internationaux`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

-- ── Clés étrangères ────────────────────────────────────────────────────────

ALTER TABLE `agios`
  ADD CONSTRAINT `FK8dp1ltfv7wr5al9sipy4b0107` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `agios_aud`
  ADD CONSTRAINT `FKiqbobvs010iyidv1t0nn02xhy` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `agios_journalier`
  ADD CONSTRAINT `FK2en0pwll1dsulm2pxdeoqpqqj` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`),
  ADD CONSTRAINT `FK7bbq8xylg7nqtokn9418mpyjl` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FK8qjsgqh7e4d3jgearhns9uc4c` FOREIGN KEY (`trimestre_id`) REFERENCES `agios_trimestriel` (`id`);

ALTER TABLE `agios_trimestriel`
  ADD CONSTRAINT `FKcaaqqxmwbl0owg7ik3jik463f` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `agios_trimestriel_aud`
  ADD CONSTRAINT `FKdakssioxo8ngyj0smer8kgxir` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `banque_aud`
  ADD CONSTRAINT `FKp12gnvoorjq0kcm6gosms2h94` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `cautions`
  ADD CONSTRAINT `FK8gbpemsexoepyvwt20tb390ed` FOREIGN KEY (`nature_id`) REFERENCES `nature_ligne_fonctionnelle` (`id`),
  ADD CONSTRAINT `FKasc3f7yqjjmf3enytlodagp2o` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FKfost869htj8594dmi7ae579fl` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `cautions_aud`
  ADD CONSTRAINT `FKlq9oj07bf3unksbn0e2no638o` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `caution_commission_config`
  ADD CONSTRAINT `FKo7w9xlq9cwswyd8a4qpxtugj4` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `caution_commission_config_aud`
  ADD CONSTRAINT `FKmmt02t65ht1a21p7sxyxl5ys5` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `charge_fixe_config`
  ADD CONSTRAINT `FKou66k0unndvretd7hyc6cvmm8` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `charge_fixe_config_aud`
  ADD CONSTRAINT `FK5m5y8rf3wxvlcdfa0hfykhibc` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `cheques`
  ADD CONSTRAINT `FKq2rbj2rkjv05tv4kf017gtq4c` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `cheques_aud`
  ADD CONSTRAINT `FKoce14m6v8t8j4lhxb5j4qv69p` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `cmt_config_aud`
  ADD CONSTRAINT `FKd1x3nddefa9xu6dbf9g26n1jn` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `commission`
  ADD CONSTRAINT `FKbx7ef0cyl07w191ay62ghq9dl` FOREIGN KEY (`impot_et_taxe_id`) REFERENCES `impot_et_taxe` (`id`),
  ADD CONSTRAINT `FKfjravd1r62rantee2guiamn7o` FOREIGN KEY (`caution_id`) REFERENCES `cautions` (`id`),
  ADD CONSTRAINT `FKgh5mdd1rg9hywoyclmakyeweu` FOREIGN KEY (`virement_id`) REFERENCES `virements` (`id`),
  ADD CONSTRAINT `FKpcarewovrseewylrm6pasqhvi` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `commission_aud`
  ADD CONSTRAINT `FKchsssjqw01m245bw7oitw0shx` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `commission_config`
  ADD CONSTRAINT `FKpnye9qu3tbkthy4n7t1mx9wii` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `commission_config_aud`
  ADD CONSTRAINT `FKpor64wqb30n5o6gwnjai3l52i` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `contrat_cadre`
  ADD CONSTRAINT `FK30v1i7gd86xujw7ji6jrktbr` FOREIGN KEY (`organisme_id`) REFERENCES `organisme_leasing` (`id`);

ALTER TABLE `contrat_cadre_aud`
  ADD CONSTRAINT `FKpuqidooy9w5ds3hj3k9bn4q75` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `contrat_leasing`
  ADD CONSTRAINT `FK6yt78nc8qouovmj14hqaoxkjf` FOREIGN KEY (`contrat_cadre_id`) REFERENCES `contrat_cadre` (`id`),
  ADD CONSTRAINT `FKd4e8mee7l3jbkf45oyrikpiv9` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`),
  ADD CONSTRAINT `fk_contrat_leasing_cadre` FOREIGN KEY (`contrat_cadre_id`) REFERENCES `contrat_cadre` (`id`);

ALTER TABLE `contrat_leasing_aud`
  ADD CONSTRAINT `FK5dvcgg945e41ptit9w5npl7gv` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `contrat_leasing_phase`
  ADD CONSTRAINT `FK13xw8kjqibumbr40kcta8g8bv` FOREIGN KEY (`contrat_leasing_id`) REFERENCES `contrat_leasing` (`id`);

ALTER TABLE `contrat_leasing_phase_aud`
  ADD CONSTRAINT `FK32io3hpm4xln9gg2h0yhhbryr` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `credit_moyen_terme`
  ADD CONSTRAINT `FKb63rivvpq4cymn7y4eb73m3vt` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `credit_moyen_terme_aud`
  ADD CONSTRAINT `FKptbfw1b84119kunn21jvrfm1p` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `decompte`
  ADD CONSTRAINT `FKb5a1qt9el4g26drbbb198f3d8` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FKk2nh68eny65q5m6qip5h516ee` FOREIGN KEY (`banque_encaissement_id`) REFERENCES `banque` (`id`);

ALTER TABLE `decompte_aud`
  ADD CONSTRAINT `FKgaf4n8x08ipqnnyjwrus2bk85` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `department_aud`
  ADD CONSTRAINT `FK8ml32j0a8c006cscnx3v0cp0d` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `droit_constate`
  ADD CONSTRAINT `FKea8xrqwfie3oyra5v18oyjfxc` FOREIGN KEY (`ligne_fonctionnelle_specifique_id`) REFERENCES `ligne_fonctionnelle_specifique` (`id`),
  ADD CONSTRAINT `FKjw3k1pq8jnouir3s3a2jtj6le` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FKopkuan9rishe55yx22lfly5i1` FOREIGN KEY (`ligne_fonctionnelle_id`) REFERENCES `ligne_fonctionnelle` (`id`),
  ADD CONSTRAINT `FKpp56khgk9dxbabpganxxft8hk` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `droit_constate_aud`
  ADD CONSTRAINT `FKa9c49bs21najkv7qq89obm5bm` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `echeance_cmt`
  ADD CONSTRAINT `FK3j02dbgpdkqg816lyke49181h` FOREIGN KEY (`credit_moyen_terme_id`) REFERENCES `credit_moyen_terme` (`id`);

ALTER TABLE `echeance_cmt_aud`
  ADD CONSTRAINT `FKpm6238mhprwvpxrosgbk255id` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `echeance_leasing`
  ADD CONSTRAINT `FK6lqt5kw7ekfm8wvof6u8y0ev6` FOREIGN KEY (`contrat_leasing_id`) REFERENCES `contrat_leasing` (`id`);

ALTER TABLE `echeance_leasing_aud`
  ADD CONSTRAINT `FKd23df20onklajehdt9b351qbt` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `factoring`
  ADD CONSTRAINT `FK1owbcg4rihh8wxybc5hcq579t` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FK4nf0q1l16ibka9g7wmlwkd1sg` FOREIGN KEY (`ligne_fonctionnelle_specifique_id`) REFERENCES `ligne_fonctionnelle_specifique` (`id`),
  ADD CONSTRAINT `FKd0rjg0rw9pvlax83l5gqns1kx` FOREIGN KEY (`ligne_fonctionnelle_id`) REFERENCES `ligne_fonctionnelle` (`id`);

ALTER TABLE `factoring_aud`
  ADD CONSTRAINT `FK3i8ktu51xa9jtbaa81ibfi7ln` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `frais_bancaire`
  ADD CONSTRAINT `FK3b0mmyf6mm94h3hubxjru0tel` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `frais_bancaire_aud`
  ADD CONSTRAINT `FKq6ohpxsm7veovwet33c2vo0ha` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `impayee`
  ADD CONSTRAINT `FKeylp2fflkjxwguefd0kvq0adq` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `impayee_aud`
  ADD CONSTRAINT `FKqcutxd0bks32c9gs15l94lu78` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `impot_et_taxe`
  ADD CONSTRAINT `FK6822r70ni2xoue7nno1thgo74` FOREIGN KEY (`caution_id`) REFERENCES `cautions` (`id`),
  ADD CONSTRAINT `FK8nkpfqmwev9cdjielt19vj9bh` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`),
  ADD CONSTRAINT `FKl9stm0or019wgwv9u6ngdv6md` FOREIGN KEY (`commission_id`) REFERENCES `commission` (`id`);

ALTER TABLE `impot_et_taxe_aud`
  ADD CONSTRAINT `FK5ie7fpduejvaij14tuj3id1is` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `lcns`
  ADD CONSTRAINT `FK9ph6p1cyjg4nsc59iucorafw3` FOREIGN KEY (`ligne_fonctionnelle_id`) REFERENCES `ligne_fonctionnelle` (`id`),
  ADD CONSTRAINT `FKbgoif52n0bs32q15oncvf1p93` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `lcns_aud`
  ADD CONSTRAINT `FKsxs04pe2biytucddtdbqxp0ab` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `leasing_config_aud`
  ADD CONSTRAINT `FKf2eh4yndtxiqckeegvxuqhdr4` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `ligne_fonctionnelle`
  ADD CONSTRAINT `FK950ndr0000osm83ob87lq47dx` FOREIGN KEY (`nature_ligne_id`) REFERENCES `nature_ligne_fonctionnelle` (`id`),
  ADD CONSTRAINT `FKem7x3v915h4okb2cf03c9lsh2` FOREIGN KEY (`ligne_mere_id`) REFERENCES `ligne_fonctionnelle` (`id`),
  ADD CONSTRAINT `FKiwfx893ms6t4s0r8p4p212659` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `ligne_fonctionnelle_aud`
  ADD CONSTRAINT `FKay2taoldwdfa3md7yd8sb3dux` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `ligne_fonctionnelle_config_aud`
  ADD CONSTRAINT `FK1cwrh0ngwieewljavxcen8mnw` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `ligne_fonctionnelle_marche`
  ADD CONSTRAINT `FKc7q78fxx5wpwn2owjqkd0pctg` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FKmehqwt8xrb101jp3kcknmox04` FOREIGN KEY (`ligne_id`) REFERENCES `ligne_fonctionnelle` (`id`);

ALTER TABLE `ligne_fonctionnelle_marche_aud`
  ADD CONSTRAINT `FKanhk1lirn1g1sidf0oo6kbbd3` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `ligne_fonctionnelle_specifique`
  ADD CONSTRAINT `FK57lk2ydnkt1um5o98h1cfri81` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FKlfodudydwmhtqocgajmo862e9` FOREIGN KEY (`nature_ligne_id`) REFERENCES `nature_ligne_fonctionnelle` (`id`);

ALTER TABLE `ligne_fonctionnelle_specifique_aud`
  ADD CONSTRAINT `FKfh52vn2vs2el0wp0jrq8jwjs6` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `log_entry_detail`
  ADD CONSTRAINT `FKqyda0kxkktunv8kyalky6cr7k` FOREIGN KEY (`log_id`) REFERENCES `log` (`id`);

ALTER TABLE `marches`
  ADD CONSTRAINT `FKcpi3mv9yhm6gs9b22qxvcedt5` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `marches_aud`
  ADD CONSTRAINT `FKd1246dinnjekoywgu1lglj5b9` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `mises_a_disposition`
  ADD CONSTRAINT `FK2mwhxb2rlme2h916yggaga3ku` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `mises_a_disposition_aud`
  ADD CONSTRAINT `FKd5ow2opukndhy03wjoioh1k63` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `nature_ligne_fonctionnelle_aud`
  ADD CONSTRAINT `FKnm7ly625qwkw31q8axfceqdkc` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `notifications_aud`
  ADD CONSTRAINT `FKq13cdum57uaulds1c0y53jd4j` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `operation_errone`
  ADD CONSTRAINT `FK64gpla2wkwxqxikq0i1l9oita` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `operation_errone_aud`
  ADD CONSTRAINT `FKklis0r030sqaw2u6q54nnyl7y` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `organisme_leasing_aud`
  ADD CONSTRAINT `FKiqt0dfrkr3i27j17gtayqvyve` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `prefinancement`
  ADD CONSTRAINT `FK4g5mv670pd9jwp2hnw2phpk4e` FOREIGN KEY (`marche_id`) REFERENCES `marches` (`id`),
  ADD CONSTRAINT `FKo0yv0j8c6jdvg4hl9l5l4s16g` FOREIGN KEY (`ligne_fonctionnelle_specifique_id`) REFERENCES `ligne_fonctionnelle_specifique` (`id`),
  ADD CONSTRAINT `FKtilv0u1346pt55815uscwjm4s` FOREIGN KEY (`ligne_fonctionnelle_id`) REFERENCES `ligne_fonctionnelle` (`id`);

ALTER TABLE `prefinancement_aud`
  ADD CONSTRAINT `FKdbpnx4wr6h25kwy6iev1pf394` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `prelevement`
  ADD CONSTRAINT `FKql7rimltc1kmokjmhxpm0spq4` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `prelevement_aud`
  ADD CONSTRAINT `FKjj39ifrh2y503ta6rw81b2o9c` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `provision`
  ADD CONSTRAINT `FKglsjrhgrwtqef5sja7cbb4an9` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `provision_aud`
  ADD CONSTRAINT `FKk8898t26efmt6wxah762mbk6g` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `rapprochement`
  ADD CONSTRAINT `FKspq7j7jtjyesk6b0gsoejwnt1` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `rapprochement_aud`
  ADD CONSTRAINT `FKkxue94gcr6i4pkjmogg17top1` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `restitution`
  ADD CONSTRAINT `FK9gwtf8ws5rn2rlxlfac6gpnar` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `restitution_aud`
  ADD CONSTRAINT `FKg6uln2rup9h0cdre9glnpo3kj` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `transaction`
  ADD CONSTRAINT `FK6clh3e3bqnc90kq02lu0j97fw` FOREIGN KEY (`rapprochement_id`) REFERENCES `rapprochement` (`id`),
  ADD CONSTRAINT `FK6q4n22db62ovn9vtvdg547quo` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `transaction_aud`
  ADD CONSTRAINT `FKb0b8njq8b3l4vwvo7mhgc39t0` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `users_aud`
  ADD CONSTRAINT `FKld7cdnhid45yc6535cecshyop` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `user_department_role`
  ADD CONSTRAINT `FK1upufonuoyy28tjjom0qd8gt7` FOREIGN KEY (`department_id`) REFERENCES `department` (`id`),
  ADD CONSTRAINT `FKsq358pb2ydgjrk5gf9ihbes53` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);

ALTER TABLE `user_department_role_aud`
  ADD CONSTRAINT `FK38k1ct5ayos6yfr6vr747vjiy` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `user_notification_status`
  ADD CONSTRAINT `FK9wdicp7t7t9gydc73ii1eweg7` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  ADD CONSTRAINT `FKjbj96u0aof2vnfsh9aepxkyqi` FOREIGN KEY (`notification_id`) REFERENCES `notifications` (`id`);

ALTER TABLE `user_notification_status_aud`
  ADD CONSTRAINT `FKpyy4cvow7te4nqywg60vpmtib` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `virements`
  ADD CONSTRAINT `FKchrb8hfylc0grt3iy6xk21icw` FOREIGN KEY (`banque_emettrice_id`) REFERENCES `banque` (`id`),
  ADD CONSTRAINT `FKtanl65cwb33wvme2wrqrjifh3` FOREIGN KEY (`banque_beneficiare_id`) REFERENCES `banque` (`id`);

ALTER TABLE `virements_aud`
  ADD CONSTRAINT `FK12ocf7aq99n0f92y313a6mjt3` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

ALTER TABLE `virements_internationaux`
  ADD CONSTRAINT `FKl8dgtbsabohwidglfuonv0dxb` FOREIGN KEY (`banque_id`) REFERENCES `banque` (`id`);

ALTER TABLE `virements_internationaux_aud`
  ADD CONSTRAINT `FKgfs5b7jprk8dtdjyc345fqwn7` FOREIGN KEY (`rev`) REFERENCES `audit_revision_entity` (`id`);

-- ── Séquence des révisions d'audit (Hibernate Envers) ──────────────────────

INSERT INTO `audit_revision_entity_seq` (`next_val`) VALUES (1);

