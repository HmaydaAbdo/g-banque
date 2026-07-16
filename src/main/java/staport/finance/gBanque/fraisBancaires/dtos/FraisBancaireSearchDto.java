package staport.finance.gBanque.fraisBancaires.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran de recherche des frais bancaires (endpoint {@code /search}).
 */
public record FraisBancaireSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status

) {}
