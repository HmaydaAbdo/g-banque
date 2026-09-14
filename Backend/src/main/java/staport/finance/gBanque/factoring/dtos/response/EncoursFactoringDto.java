package staport.finance.gBanque.factoring.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Encours (déblocage ou remboursement) de factoring d'une banque : total et page détaillée.
 */
public record EncoursFactoringDto(
        BigDecimal totalDesEncours,
        Page<FactoringResponseDto> page
) {
}
