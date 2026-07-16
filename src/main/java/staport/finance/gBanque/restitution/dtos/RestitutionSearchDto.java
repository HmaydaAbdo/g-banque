package staport.finance.gBanque.restitution.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran de recherche des restitutions (endpoint {@code /search}).
 */
public record RestitutionSearchDto(

        Long banqueId,
        String libelle,
        BigDecimal montant,
        Status status

) {}
