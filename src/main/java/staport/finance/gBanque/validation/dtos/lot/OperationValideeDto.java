package staport.finance.gBanque.validation.dtos.lot;

import java.math.BigDecimal;

/**
 * Résumé d'une opération validée dans le cadre d'une validation en lot.
 * Volontairement léger et générique : les 17 DTOs de réponse unitaires existants
 * (VirementTransactionResponseDto, ChequeTransactionResponseDto, ...) ne sont pas modifiés ;
 * {@code BatchValidationService} extrait ces champs depuis chacun d'eux au moment de l'appel.
 */
public record OperationValideeDto(
        Long idOperation,
        Long idTransaction,
        String libelle,
        BigDecimal debit,
        BigDecimal credit
) {}
