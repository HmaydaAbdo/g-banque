package staport.finance.gBanque.droitConstate.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un droit constaté.
 *
 * <p>Déplacé depuis {@code validation.dtos.droitConstate} vers le module {@code droitConstate} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code droitConstateId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID du droit constaté source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "getDroitConstateId")} sur {@code DroitConstateValidationService}, l'entité auditée étant le
 * droit constaté, pas la transaction.
 */
public class DroitConstateTransactionResponse {

    private Long id;
    private Long droitConstateId;
    private String banqueName;
    private String libelle;
    private String observation;
    private LocalDate dateEmission;
    private LocalDate dateValeur;
    private LocalDate dateOperations;
    private BigDecimal debit;
    private BigDecimal credit;
    private final TransactionSource transactionSource = TransactionSource.DROIT_CONSTATE;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDroitConstateId() {
        return droitConstateId;
    }

    public void setDroitConstateId(Long droitConstateId) {
        this.droitConstateId = droitConstateId;
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
