package staport.finance.gBanque.Agios.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse de l'endpoint encours agios : total des montants + page d'agios PREVU.
 */
public record EncoursAgiosDto(

        BigDecimal totalDesEncours,
        Page<AgiosResponseDto> page

) {}
