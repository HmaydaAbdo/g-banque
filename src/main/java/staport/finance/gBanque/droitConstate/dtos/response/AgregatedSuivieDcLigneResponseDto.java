package staport.finance.gBanque.droitConstate.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Suivi agrégé de droit constaté d'une ligne fonctionnelle (générale ou spécifique) : totaux
 * débit/crédit/reste et page détaillée des mouvements.
 */
public record AgregatedSuivieDcLigneResponseDto(
        String banqueName,
        String marcheName,
        String marcheNumero,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal reste,
        Page<SuivieDcLigneResponseDto> page
) {
}
