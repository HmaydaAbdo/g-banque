package staport.finance.gBanque.prefinancement.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Suivi agrégé de préfinancement d'une ligne fonctionnelle (générale ou spécifique) : totaux
 * débit/crédit/reste et page détaillée des mouvements.
 */
public record AgregatedSuiviePrefiLigneResponseDto(
        String banqueName,
        String marcheName,
        String marcheNumero,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal reste,
        Page<SuiviePrefiLigneResponseDto> page
) {
}
