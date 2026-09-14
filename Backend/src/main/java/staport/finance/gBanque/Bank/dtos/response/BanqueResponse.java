package staport.finance.gBanque.Bank.dtos.response;

import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Réponse détaillée du module Bank.
 * {@code soldeActuel} est calculé en temps réel — il n'est pas stocké en base.
 */
public record BanqueResponse(
        Long id,
        String logoBase64,
        String nom,
        String numeroDeCompte,
        Devise devise,
        String adresse,
        String agence,
        BigDecimal soldeActuel,
        LocalDate dateCreation,
        LocalDate dateModification
) {}
