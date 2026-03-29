package staport.finance.gBanque.Bank.dtos;

import staport.finance.gBanque.Bank.enums.Devise;
import java.math.BigDecimal;

/**
 * Clean, immutable response DTO for the Bank Home screen.
 */
public record BankAcceuilResponse(
        Long bankId,
        String logoBase64,
        String name,
        BigDecimal soldeActuelle,
        BigDecimal soldeDepart,
        BigDecimal plafondFC,
        Devise devise
) {}