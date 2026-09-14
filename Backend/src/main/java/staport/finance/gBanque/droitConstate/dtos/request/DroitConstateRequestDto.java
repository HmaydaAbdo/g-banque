package staport.finance.gBanque.droitConstate.dtos.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Données de création/modification d'un droit constaté.
 *
 * <p>{@code banqueId} est obligatoire (contrairement à Préfinancement/Factoring) : pré-rempli
 * depuis {@code marche.banque} côté UI, mais modifiable indépendamment.
 * <ul>
 *   <li>DEBLOCAGE → banque d'encaissement (reçoit les fonds du Trésor)</li>
 *   <li>REMBOURSEMENT → banque de décaissement (effectue le remboursement)</li>
 * </ul>
 */
public record DroitConstateRequestDto(
        @NotNull(message = "Marche ID cannot be null") Long marcheId,
        @NotNull(message = "La banque est obligatoire") Long banqueId,
        @NotNull(message = "Date of emission cannot be null") LocalDate dateEmission,
        @NotNull(message = "Montant cannot be null")
        @DecimalMin(value = "0.01", message = "Montant must be a positive value") BigDecimal montant,
        @NotNull(message = "Type cannot be null") Type type
) {
}
