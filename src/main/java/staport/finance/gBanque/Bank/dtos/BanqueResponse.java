package staport.finance.gBanque.Bank.dtos;

import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable detailed response for Bank management.
 * Records automatically handle getters, toString, and final fields.
 */
public record BanqueResponse(
        Long id,
        String logoBase64,
        String nom,
        String numeroDeCompte,
        Devise devise,
        String address,
        String agence,
        BigDecimal soldeActuel,
        BigDecimal soldeDepart,
        LocalDate dateCreation,
        LocalDate dateModification
) {}