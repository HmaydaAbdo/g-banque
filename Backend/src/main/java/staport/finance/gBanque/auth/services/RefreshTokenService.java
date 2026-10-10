package staport.finance.gBanque.auth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.auth.entities.RefreshToken;
import staport.finance.gBanque.auth.repositories.RefreshTokenRepository;
import staport.finance.gBanque.exception.customExceptions.InvalidRefreshTokenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Cycle de vie des jetons de rafraîchissement : émission à la connexion, rotation à chaque
 * rafraîchissement, révocation à la déconnexion, détection de réutilisation.
 *
 * <p><b>Réutilisation.</b> Un jeton déjà échangé qui revient signifie qu'il a pu être volé :
 * toute la session (famille) est révoquée. Exception : un jeton échangé il y a moins de
 * {@link #ROTATION_GRACE} — cas de deux onglets qui rafraîchissent en même temps. Les cookies
 * étant partagés par les onglets, le second dispose déjà des nouveaux jetons ; on lui répond
 * {@link RefreshOutcome.AlreadyRotated} sans rien révoquer.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    static final Duration ROTATION_GRACE = Duration.ofSeconds(30);
    private static final int TOKEN_BYTES = 32;
    private static final String SESSION_EXPIRED = "Votre session a expiré, veuillez vous reconnecter.";

    private final RefreshTokenRepository refreshTokenRepository;
    private final long refreshTokenHours;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh-token-hours}") long refreshTokenHours) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHours = refreshTokenHours;
    }

    /** Jeton en clair (à poser dans le cookie) et son échéance absolue. */
    public record IssuedRefreshToken(String rawToken, Instant expiresAt) {
    }

    /** Résultat d'un rafraîchissement. */
    public sealed interface RefreshOutcome {
        /** Jeton valide : un nouveau jeton a été émis pour {@code username}. */
        record Rotated(String username, IssuedRefreshToken newToken) implements RefreshOutcome {
        }

        /** Jeton échangé à l'instant par une autre requête de la même session : rien à émettre. */
        record AlreadyRotated() implements RefreshOutcome {
        }
    }

    /** Ouvre une nouvelle session (nouvelle famille) d'une durée de {@code jwt.refresh-token-hours}. */
    @Transactional
    public IssuedRefreshToken issueForNewSession(String username) {
        Instant now = Instant.now();
        Instant sessionExpiresAt = now.plus(refreshTokenHours, ChronoUnit.HOURS);
        return issue(username, UUID.randomUUID().toString(), now, sessionExpiresAt);
    }

    /**
     * Échange un jeton valide contre un nouveau, dans la même session et avec la même échéance.
     *
     * @throws InvalidRefreshTokenException si le jeton est absent, inconnu, expiré, révoqué ou réutilisé
     */
    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public RefreshOutcome rotate(String rawToken) {
        RefreshToken current = findOrThrow(rawToken);
        Instant now = Instant.now();

        if (current.getRevokedAt() != null) {
            throw new InvalidRefreshTokenException(SESSION_EXPIRED);
        }
        if (!now.isBefore(current.getExpiresAt())) {
            throw new InvalidRefreshTokenException(SESSION_EXPIRED);
        }
        if (current.getRotatedAt() != null) {
            if (current.getRotatedAt().plus(ROTATION_GRACE).isAfter(now)) {
                return new RefreshOutcome.AlreadyRotated();
            }
            int revoked = refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            log.warn("Réutilisation d'un jeton de rafraîchissement déjà échangé : session révoquée username={} jetonsRévoqués={}",
                    current.getUsername(), revoked);
            throw new InvalidRefreshTokenException(SESSION_EXPIRED);
        }

        current.markRotated(now);
        IssuedRefreshToken next = issue(current.getUsername(), current.getFamilyId(), now, current.getExpiresAt());
        return new RefreshOutcome.Rotated(current.getUsername(), next);
    }

    /** Termine la session du jeton présenté (déconnexion). Sans effet si le jeton est absent ou inconnu. */
    @Transactional
    public void revokeSession(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            refreshTokenRepository.revokeFamily(token.getFamilyId(), Instant.now());
            log.info("Déconnexion : session révoquée username={}", token.getUsername());
        });
    }

    /** Purge quotidienne des jetons dont la session est terminée depuis plus d'un jour. */
    @Scheduled(cron = "${auth.refresh-token.cleanup-cron}")
    @Transactional
    public void purgeExpired() {
        int deleted = refreshTokenRepository.deleteExpiredBefore(Instant.now().minus(1, ChronoUnit.DAYS));
        log.info("Purge des jetons de rafraîchissement expirés : {} supprimé(s)", deleted);
    }

    private IssuedRefreshToken issue(String username, String familyId, Instant now, Instant sessionExpiresAt) {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshToken(hash(rawToken), familyId, username, now, sessionExpiresAt));
        return new IssuedRefreshToken(rawToken, sessionExpiresAt);
    }

    private RefreshToken findOrThrow(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException(SESSION_EXPIRED);
        }
        return refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidRefreshTokenException(SESSION_EXPIRED));
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
