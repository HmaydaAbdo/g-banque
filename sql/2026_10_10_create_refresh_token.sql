-- =============================================================================
-- Table refresh_token — sessions par cookies (10/10/2026)
-- =============================================================================
--
-- Mappée par staport.finance.gBanque.auth.entities.RefreshToken.
--
-- À exécuter sur la base de production AVANT de déployer la version qui
-- introduit les cookies de session : le profil prod est en ddl-auto=validate,
-- le backend refuse de démarrer si la table manque. En dev (ddl-auto=update),
-- Hibernate la crée seul ; ce script reste valable (idempotent).
--
-- Table neuve : aucune donnée existante impactée, aucune migration de données.
-- Une ligne par jeton émis (connexion puis un renouvellement toutes les ~15 min
-- par utilisateur actif) ; purge quotidienne des sessions expirées par le
-- backend (RefreshTokenService.purgeExpired, 03:15).
-- =============================================================================

CREATE TABLE IF NOT EXISTS refresh_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    token_hash  VARCHAR(64)  NOT NULL,               -- SHA-256 hexadécimal ; le jeton en clair n'est jamais stocké
    family_id   VARCHAR(36)  NOT NULL,               -- identifiant de la session (UUID), commun aux jetons d'une connexion
    username    VARCHAR(50)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,               -- échéance absolue de la session (connexion + JWT_REFRESH_TOKEN_HOURS)
    rotated_at  DATETIME(6)  NULL,                   -- jeton échangé contre un nouveau
    revoked_at  DATETIME(6)  NULL,                   -- session terminée (déconnexion, réutilisation détectée)
    PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    INDEX idx_refresh_token_family (family_id),
    INDEX idx_refresh_token_expires_at (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Vérification
-- SHOW CREATE TABLE refresh_token;
