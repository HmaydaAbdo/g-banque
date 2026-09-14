package staport.finance.gBanque.restitution.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour une restitution.
 */
public record RestitutionResponseDto(

        Long id,
        String libelle,
        String observation,
        BigDecimal montant,
        Long banqueId,
        String banqueNom,
        LocalDate dateEmission,
        LocalDate dateValeur,
        LocalDate dateOperation,
        Status status

) {}
