package staport.finance.gBanque.leasing.echeancesLeasing.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * DTO utilisé pour requêter les encours d'échéances de leasing.
 * Contient les critères de recherche pour identifier la banque et la période de calcul.
 */
public record EncoursLeasingRequestDTO(
        @NotNull(message = "L'identifiant de la banque est obligatoire.") Long banqueId,
        Long organismeLeasingId,
        String numeroContrat,
        BigDecimal montant
) {}
