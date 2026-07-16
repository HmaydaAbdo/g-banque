package staport.finance.gBanque.fraisBancaires.dtos;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée : page de résultats + total des encours.
 */
public record EncoursFraisBancaireDto(

        BigDecimal totalDesEncours,

        Page<FraisBancaireResponseDto> page

) {}
