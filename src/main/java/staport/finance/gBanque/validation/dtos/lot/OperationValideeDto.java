package staport.finance.gBanque.validation.dtos.lot;

import java.math.BigDecimal;

/**
 * Résumé d'une opération validée dans le cadre d'une validation en lot.
 * Volontairement léger et générique : les 17 DTOs de réponse unitaires existants
 * (VirementTransactionResponseDto, ChequeTransactionResponseDto, ...) ne sont pas modifiés ;
 * {@code BatchValidationService} extrait ces champs depuis chacun d'eux au moment de l'appel.
 */
public class OperationValideeDto {

    private final Long idOperation;
    private final Long idTransaction;
    private final String libelle;
    private final BigDecimal debit;
    private final BigDecimal credit;

    public OperationValideeDto(Long idOperation, Long idTransaction, String libelle, BigDecimal debit, BigDecimal credit) {
        this.idOperation = idOperation;
        this.idTransaction = idTransaction;
        this.libelle = libelle;
        this.debit = debit;
        this.credit = credit;
    }

    public Long getIdOperation() {
        return idOperation;
    }

    public Long getIdTransaction() {
        return idTransaction;
    }

    public String getLibelle() {
        return libelle;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }
}
