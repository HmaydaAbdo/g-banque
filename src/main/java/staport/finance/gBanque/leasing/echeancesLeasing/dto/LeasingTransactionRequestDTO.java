package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'une échéance de leasing.
 * Crée la transaction bancaire associée et marque l'échéance comme PAYE.
 */
public record LeasingTransactionRequestDTO(

        @NotNull(message = "L'ID du contrat de leasing est obligatoire")
        Long contratLeasingId,

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
