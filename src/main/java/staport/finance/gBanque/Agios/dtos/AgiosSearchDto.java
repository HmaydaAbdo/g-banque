package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

/**
 * Critères de recherche pour la liste paginée des agios.
 */
public record AgiosSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status

) {}
