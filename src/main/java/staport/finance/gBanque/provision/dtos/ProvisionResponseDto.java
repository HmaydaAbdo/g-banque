package staport.finance.gBanque.provision.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour une provision.
 */
public record ProvisionResponseDto(

        Long id,
        String libelle,
        String observation,
        BigDecimal montant,
        Long banqueId,
        String banqueNom,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        Status status,
        TransactionSource motif,
        SensOperation sens

) {}
