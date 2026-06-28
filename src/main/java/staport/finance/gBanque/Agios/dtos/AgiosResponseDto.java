package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse exposé par l'API Agios.
 */
public record AgiosResponseDto(

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
