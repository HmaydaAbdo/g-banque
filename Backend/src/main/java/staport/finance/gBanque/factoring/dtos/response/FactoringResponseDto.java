package staport.finance.gBanque.factoring.dtos.response;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Représentation exposée au client d'un factoring.
 */
public record FactoringResponseDto(
        Long id,
        Long marcheId,
        String marcheName,
        String marcheNumero,
        Long banqueId,
        String banqueName,
        LocalDate dateEmission,
        BigDecimal montant,
        String observation,
        Type type,
        LocalDate dateOperation,
        LocalDate dateValeur,
        Status status,
        String libelle
) {
}
