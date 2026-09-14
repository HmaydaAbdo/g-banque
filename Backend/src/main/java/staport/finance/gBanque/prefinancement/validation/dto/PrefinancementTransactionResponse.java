package staport.finance.gBanque.prefinancement.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de réponse pour la validation (rapprochement) d'un préfinancement.
 *
 * <p>Déplacé depuis {@code validation.dtos.prefinancements} vers le module
 * {@code prefinancement} — conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code prefinancementId} (distinct de {@code id}, qui identifie la transaction créée) porte
 * l'ID du préfinancement source — nécessaire pour {@code @AuditableAction(idAccessor =
 * "getPrefinancementId")} sur {@code PrefinancementValidationService}, l'entité auditée étant le
 * préfinancement, pas la transaction.
 */
public class PrefinancementTransactionResponse {

    private Long id;
    private Long prefinancementId;
    private String banqueName;
    private String marcheNumero;
    private String libelle;
    private String observation;
    private LocalDate dateEmission ;
    private LocalDate dateValeur ;
    private LocalDate dateOperations ;
    private BigDecimal debit;
    private BigDecimal credit;
    private final TransactionSource transactionSource = TransactionSource.PREFINANCEMENT;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrefinancementId() {
        return prefinancementId;
    }

    public void setPrefinancementId(Long prefinancementId) {
        this.prefinancementId = prefinancementId;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public LocalDate getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(LocalDate dateOperations) {
        this.dateOperations = dateOperations;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
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

    public String getMarcheNumero() {
        return marcheNumero;
    }

    public void setMarcheNumero(String marcheNumero) {
        this.marcheNumero = marcheNumero;
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }
}
