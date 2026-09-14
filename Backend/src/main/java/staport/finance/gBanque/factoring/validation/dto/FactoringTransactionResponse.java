package staport.finance.gBanque.factoring.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un factoring.
 *
 * <p>Déplacé depuis {@code validation.dtos.factoring} vers le module {@code factoring} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code factoringId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID du factoring source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "getFactoringId")} sur {@code FactoringValidationService}, l'entité auditée étant le factoring,
 * pas la transaction.
 */
public class FactoringTransactionResponse {

    private Long id;
    private Long factoringId;
    private String banqueName;
    private String libelle;
    private String observation;
    private LocalDate dateEmission;
    private LocalDate dateValeur;
    private LocalDate dateOperations;
    private BigDecimal debit;
    private BigDecimal credit;
    private final TransactionSource transactionSource = TransactionSource.FACTORING;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFactoringId() {
        return factoringId;
    }

    public void setFactoringId(Long factoringId) {
        this.factoringId = factoringId;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public LocalDate getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(LocalDate dateOperations) {
        this.dateOperations = dateOperations;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }
}
