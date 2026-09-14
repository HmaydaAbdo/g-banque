package staport.finance.gBanque.cmt.echeances.dtos;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record EncoursCMTRequestDTO(
        @NotNull(message = "L'identifiant de la banque est obligatoire.")
        Long banqueId,
        String designation,
        BigDecimal montant
) {
}
