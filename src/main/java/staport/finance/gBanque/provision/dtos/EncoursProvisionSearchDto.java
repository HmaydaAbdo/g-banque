package staport.finance.gBanque.provision.dtos;

import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

/**
 * Critères de recherche pour l'écran d'encours des provisions d'une banque (banqueId fourni
 * séparément par le path variable — mirroring {@code EncoursOperationErroneSearchDto}). Le
 * {@code sens} permet de filtrer l'écran encours décaissement / encaissement dédié côté frontend
 * (module à double sens, comme Virement/Chèque/LCN).
 */
public record EncoursProvisionSearchDto(

        String libelle,
        BigDecimal montant,
        TransactionSource motif,
        SensOperation sens

) {}
