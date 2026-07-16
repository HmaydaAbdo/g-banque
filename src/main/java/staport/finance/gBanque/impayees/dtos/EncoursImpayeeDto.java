package staport.finance.gBanque.impayees.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursImpayeeDto(

        BigDecimal totalDesEncours,

        Page<ImpayeeResponseDto> page

) {}
