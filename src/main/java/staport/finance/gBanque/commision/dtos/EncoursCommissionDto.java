package staport.finance.gBanque.commision.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursCommissionDto(

        BigDecimal totalDesEncours,

        Page<CommissionResponseDto> page

) {}
