package staport.finance.gBanque.factoring.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Suivi agrégé de factoring d'une ligne fonctionnelle (générale ou spécifique) : totaux
 * débit/crédit/reste et page détaillée des mouvements.
 */
public record AgregatedSuivieFactoringLigneResponseDto(
        String banqueName,
        String marcheName,
        String marcheNumero,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal reste,
        Page<SuivieFactoringLineResponseDto> page
) {
}
