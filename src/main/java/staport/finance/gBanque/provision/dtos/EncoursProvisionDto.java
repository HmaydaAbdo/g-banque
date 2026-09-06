package staport.finance.gBanque.provision.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursProvisionDto(

        BigDecimal totalDesEncours,

        Page<ProvisionResponseDto> page

) {}
