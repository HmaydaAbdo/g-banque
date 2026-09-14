package staport.finance.gBanque.factoring.dtos.response;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne du suivi de factoring d'une ligne fonctionnelle (générale ou spécifique).
 */
public record SuivieFactoringLineResponseDto(
        Long id,
        LocalDate dateEmission,
        LocalDate dateOperation,
        LocalDate dateValeur,
        String libelle,
        BigDecimal debit,
        BigDecimal credit,
        String observation,
        Status status,
        Type type,
        String banqueName,
        String marcheName,
        String marcheNumero
) {
}
