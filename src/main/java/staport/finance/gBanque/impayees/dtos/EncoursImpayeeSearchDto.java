package staport.finance.gBanque.impayees.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des impayées d'une banque (banqueId fourni
 * séparément par le path variable — mirroring {@code EncoursImpotEtTaxeSearchDto}).
 */
public record EncoursImpayeeSearchDto(

        String libelle,
        TransactionSource motif,
        Type type,
        BigDecimal montant

) {}
