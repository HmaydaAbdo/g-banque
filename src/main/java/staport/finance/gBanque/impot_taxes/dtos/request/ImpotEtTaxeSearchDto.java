package staport.finance.gBanque.impot_taxes.dtos.request;

import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

/**
 * Critères de recherche pour la liste générale des impôts/taxes. Tous les champs sont optionnels.
 */
public record ImpotEtTaxeSearchDto(

        Long banqueId,

        String libelle,

        Motif motif,

        BigDecimal montant,

        Status status

) {}
