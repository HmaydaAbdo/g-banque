package staport.finance.gBanque.prefinancement.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Encours (déblocage ou remboursement) de préfinancement d'une banque : total et page détaillée.
 */
public record EncoursPrefinancementDto(
        BigDecimal totalDesEncours,
        Page<PrefinancementResponseDTO> page
) {
}
