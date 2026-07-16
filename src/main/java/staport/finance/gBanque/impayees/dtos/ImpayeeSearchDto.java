package staport.finance.gBanque.impayees.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran de recherche des impayées (endpoint {@code /search}).
 */
public record ImpayeeSearchDto(

        Long banqueId,
        String libelle,
        TransactionSource motif,
        Type type,
        BigDecimal montant,
        Status status

) {}
