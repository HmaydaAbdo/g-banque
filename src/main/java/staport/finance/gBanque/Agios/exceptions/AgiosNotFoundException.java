package staport.finance.gBanque.Agios.exceptions;

/**
 * Levée lorsqu'un agios est introuvable par son identifiant.
 * Interceptée par {@link AgiosExceptionHandler} → HTTP 404.
 */
public class AgiosNotFoundException extends RuntimeException {

    public AgiosNotFoundException(Long id) {
        super("Agios introuvable — id: " + id);
    }
}
