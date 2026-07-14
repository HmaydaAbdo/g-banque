package staport.finance.gBanque.impot_taxes.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursImpotEtTaxeDto(

        BigDecimal totalDesEncours,

        Page<ImpotEtTaxeResponseDto> page

) {}
