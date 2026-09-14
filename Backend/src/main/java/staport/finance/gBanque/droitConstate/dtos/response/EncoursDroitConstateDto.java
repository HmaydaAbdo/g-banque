package staport.finance.gBanque.droitConstate.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Encours (déblocage ou remboursement) de droits constatés d'une banque : total et page
 * détaillée.
 */
public record EncoursDroitConstateDto(
        BigDecimal totalDesEncours,
        Page<DroitConstateResponseDto> page
) {
}
