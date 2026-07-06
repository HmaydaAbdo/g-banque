package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

import java.math.BigDecimal;

/**
 * Représentation allégée d'une sous-ligne, imbriquée dans {@link LigneFonctionnelleResponse}
 * de la ligne mère.
 */
public record SousLigneResponseDto(
        Long id,
        BigDecimal plafond,
        Long natureLigneFonctionnelleId,
        String natureLigneFonctionnelleLabel,
        boolean actif
) {
}
