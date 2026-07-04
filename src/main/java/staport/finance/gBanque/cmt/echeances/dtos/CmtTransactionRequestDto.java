package staport.finance.gBanque.cmt.echeances.dtos;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'une échéance CMT.
 * Crée la transaction bancaire associée et marque l'échéance comme PAYE.
 */
public record CmtTransactionRequestDto(

        @NotNull(message = "L'ID du crédit moyen terme est obligatoire")
        Long creditMoyenTermeId,

        @NotNull(message = "L'ID de l'échéance est obligatoire")
        Long echeanceId,

        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        String observation,
        Double tvaDeductible,
        String libelle,

        @NotNull(message = "rapprochementId est obligatoire")
        Long rapprochementId

) {}
