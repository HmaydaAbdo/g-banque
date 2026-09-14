package staport.finance.gBanque.restitution.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursRestitution(

        BigDecimal totalDesEncours,

        Page<RestitutionResponseDto> page

) {}
