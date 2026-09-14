package staport.finance.gBanque.decomptes.dtos.response;

import org.springframework.data.domain.Page;

import java.math.BigDecimal;

/**
 * Réponse agrégée pour l'écran d'encours des décomptes : page de résultats + total.
 */
public record EncoursDecompteDto(

        BigDecimal totalDesEncours,

        Page<DecompteResponseDto> page

) {}
