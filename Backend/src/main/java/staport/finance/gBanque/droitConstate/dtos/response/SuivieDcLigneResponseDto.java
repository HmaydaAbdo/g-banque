package staport.finance.gBanque.droitConstate.dtos.response;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne du suivi de droit constaté d'une ligne fonctionnelle (générale ou spécifique).
 */
public record SuivieDcLigneResponseDto(
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
