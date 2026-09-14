package staport.finance.gBanque.operationErrone.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursOperationErroneDto(

        BigDecimal totalDesEncours,

        Page<OperationErroneResponseDto> page

) {}
