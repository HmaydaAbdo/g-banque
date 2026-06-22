package staport.finance.gBanque.exception.customExceptions;

/**
 * Levée quand un compte est temporairement verrouillé suite à des échecs
 * d'authentification répétés (protection brute-force sur /auth/login).
 *
 * @see staport.finance.gBanque.auth.services.LoginAttemptService
 */
public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException(String message) {
        super(message);
    }
}
