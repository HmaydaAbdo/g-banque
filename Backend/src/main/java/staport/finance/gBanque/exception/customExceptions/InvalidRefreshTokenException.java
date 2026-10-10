package staport.finance.gBanque.exception.customExceptions;

/**
 * Levée quand le jeton de rafraîchissement est absent, inconnu, expiré, révoqué ou réutilisé :
 * la session est terminée et l'utilisateur doit se reconnecter (HTTP 401).
 *
 * @see staport.finance.gBanque.auth.services.RefreshTokenService
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
