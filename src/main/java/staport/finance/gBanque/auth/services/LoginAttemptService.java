package staport.finance.gBanque.auth.services;

import org.springframework.stereotype.Service;
import staport.finance.gBanque.exception.customExceptions.TooManyAttemptsException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Verrouillage par compte après échecs d'authentification répétés sur
 * {@code /auth/login} — protection brute-force minimaliste, en mémoire.
 *
 * <p>Suffisant pour une instance unique de l'application (cas actuel). En cas
 * de scaling horizontal multi-instance, ce compteur devrait être déplacé vers
 * un store partagé (Redis...) pour rester efficace entre toutes les instances.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private static final class Attempts {
        private int failureCount = 0;
        private Instant lockedUntil = null;
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByUsername = new ConcurrentHashMap<>();

    /**
     * @throws  TooManyAttemptsException si le compte est actuellement verrouillé
     */
    public void checkNotLocked(String username) {
        Attempts attempts = attemptsByUsername.get(normalize(username));
        if (attempts == null || attempts.lockedUntil == null) {
            return;
        }
        synchronized (attempts) {
            if (attempts.lockedUntil == null) {
                return;
            }
            if (Instant.now().isBefore(attempts.lockedUntil)) {
                throw new TooManyAttemptsException(
                        "Compte temporairement verrouillé suite à plusieurs échecs de connexion. Réessayez dans quelques minutes.");
            }
            // Verrouillage expiré → on repart de zéro pour ce compte.
            attemptsByUsername.remove(normalize(username));
        }
    }

    public void recordFailure(String username) {
        Attempts attempts = attemptsByUsername.computeIfAbsent(normalize(username), k -> new Attempts());
        synchronized (attempts) {
            attempts.failureCount++;
            if (attempts.failureCount >= MAX_ATTEMPTS) {
                attempts.lockedUntil = Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES);
            }
        }
    }

    public void recordSuccess(String username) {
        attemptsByUsername.remove(normalize(username));
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }
}
