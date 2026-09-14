package staport.finance.gBanque.marches.exceptions;

/**
 * Exception métier spécifique au module Marches.
 *
 * <p>Levée lors de violations de règles métier propres aux marchés, par exemple :
 * <ul>
 *   <li>Date d'achèvement des travaux antérieure à la date de début</li>
 *   <li>Suppression définitive d'un marché ayant des données financières rattachées
 *       (décomptes, préfinancements, droits constatés, factorings, lignes spécifiques)</li>
 * </ul>
 *
 * <p>Interceptée par {@link MarcheExceptionHandler} → HTTP 422 Unprocessable Entity.
 */
public class MarcheBusinessException extends RuntimeException {

    public MarcheBusinessException(String message) {
        super(message);
    }

    public MarcheBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
