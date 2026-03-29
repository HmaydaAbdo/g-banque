package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.exceptions;

/**
 * Thrown when a {@code NatureLigneFonctionnelle} cannot be deleted because it is
 * still referenced by one or more {@code LigneFonctionnelle} records.
 */
public class NatureLFDeletionException extends RuntimeException {

    public NatureLFDeletionException(String message) {
        super(message);
    }
}
