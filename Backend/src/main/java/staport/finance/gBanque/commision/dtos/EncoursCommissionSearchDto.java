package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des commissions d'une banque (banqueId fourni
 * séparément par le path variable — mirroring {@code EncoursImpotEtTaxeSearchDto}).
 */
public record EncoursCommissionSearchDto(

        String libelle,
        TransactionSource operation,
        BigDecimal montant

) {}
