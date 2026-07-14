package staport.finance.gBanque.impot_taxes.dtos.request;

import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des impôts/taxes d'une banque (banqueId fourni
 * séparément par le path variable — mirroring {@code EncoursDecompteSearchDto}).
 */
public record EncoursImpotEtTaxeSearchDto(

        String libelle,

        Motif motif,

        BigDecimal montant

) {}
