package staport.finance.gBanque.Bank.dtos.response;

import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;

/**
 * Réponse agrégée pour le tableau de bord Finance (accueil).
 */
public record BankAccueilResponse(
        Long       banqueId,
        String     logoBase64,
        String     nom,
        BigDecimal soldeActuel,
        BigDecimal soldeDebut,
        BigDecimal plafondFC,
        Devise     devise,
        long       lcnsEchusCount,
        long       virementsEchusCount
) {}
