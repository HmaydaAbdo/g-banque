package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import java.math.BigDecimal;

/**
 * Agrégats financiers d'un contrat de leasing, calculés sur ses échéances.
 *
 * @param endettement total TTC de toutes les échéances du contrat
 * @param dejaPaye    total TTC des échéances au statut PAYE
 */
public record MontantsContratLeasing(BigDecimal endettement, BigDecimal dejaPaye) {

    /** Montant restant à payer : endettement moins le déjà payé. */
    public BigDecimal resteAPayer() {
        return endettement.subtract(dejaPaye);
    }
}
