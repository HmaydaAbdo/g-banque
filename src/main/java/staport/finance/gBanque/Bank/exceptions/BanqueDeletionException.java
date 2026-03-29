package staport.finance.gBanque.Bank.exceptions;

/**
 * Thrown when a banque cannot be deleted because one or more business guards
 * are not satisfied:
 * <ul>
 *   <li>Active lignes fonctionnelles exist for the banque</li>
 *   <li>Pending (unconfirmed) transactions exist</li>
 *   <li>The computed balance is not zero</li>
 * </ul>
 */
public class BanqueDeletionException extends RuntimeException {

    public BanqueDeletionException(String message) {
        super(message);
    }
}
