package staport.finance.gBanque.operationErrone.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des opérations erronées d'une banque (banqueId
 * fourni séparément par le path variable — mirroring {@code EncoursImpotEtTaxeSearchDto}).
 */
public record EncoursOperationErroneSearchDto(

        String libelle,
        BigDecimal montant,
        TransactionSource motif

) {}
