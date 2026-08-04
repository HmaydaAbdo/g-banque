package staport.finance.gBanque.impayees.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de réponse pour une impayée.
 */
public record ImpayeeResponseDto(

        Long id,
        String libelle,
        String observation,
        BigDecimal montant,
        Long banqueId,
        String banqueNom,
        Type type,
        TransactionSource motif,
        String numero,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        Status status,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification

) {}
