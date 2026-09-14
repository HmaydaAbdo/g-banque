package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de réponse pour la validation (rapprochement) d'une mise à disposition.
 *
 * <p>Déplacé depuis {@code validation.dtos.miseAdispo} vers le module {@code miseADisposition} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>{@code transactionSource} et {@code miseADispositionNumber} sont renseignés par
 * {@code MiseAdispositionTransactionMapper} depuis respectivement {@code Transaction.getTransactionSource()}
 * et {@code MiseADisposition.getNumero()} (corrigé le 11/07/2026, décision utilisateur explicite —
 * voir TODO.md).
 */
public class MiseAdispositionTransactionResponseDto {

    private Long id;
    /** ID de la mise à disposition source — distinct de {@code id} (celui de la transaction créée).
     * Nécessaire pour {@code @AuditableAction(idAccessor = "getMiseADispositionId")} sur
     * {@code MiseADispositionValidationService}. */
    private Long miseADispositionId;
    private String banqueName;
    private String miseADispositionNumber;
    private String libelle;
    private String observation;
    private LocalDate dateEmission;
    private LocalDate dateValeur;
    private LocalDate dateOperations;
    private BigDecimal tvaDeductible;
    private BigDecimal tvaCollectee;
    private BigDecimal debit;
    private BigDecimal credit;
    private TransactionSource transactionSource;

    public MiseAdispositionTransactionResponseDto() {
    }

    public MiseAdispositionTransactionResponseDto(Long id, String banqueName, String miseADispositionNumber, String libelle,
                                                  String observation, LocalDate dateEmission, LocalDate dateValeur,
                                                  LocalDate dateOperations, BigDecimal tvaDeductible, BigDecimal tvaCollectee,
                                                  BigDecimal debit, BigDecimal credit) {
        this.id = id;
        this.banqueName = banqueName;
        this.miseADispositionNumber = miseADispositionNumber;
        this.libelle = libelle;
        this.observation = observation;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperations = dateOperations;
        this.tvaDeductible = tvaDeductible;
        this.tvaCollectee = tvaCollectee;
        this.debit = debit;
        this.credit = credit;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMiseADispositionId() {
        return miseADispositionId;
    }

    public void setMiseADispositionId(Long miseADispositionId) {
        this.miseADispositionId = miseADispositionId;
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public String getMiseADispositionNumber() {
        return miseADispositionNumber;
    }

    public void setMiseADispositionNumber(String miseADispositionNumber) {
        this.miseADispositionNumber = miseADispositionNumber;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
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

    public BigDecimal getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(BigDecimal tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public BigDecimal getTvaCollectee() {
        return tvaCollectee;
    }

    public void setTvaCollectee(BigDecimal tvaCollectee) {
        this.tvaCollectee = tvaCollectee;
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

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(TransactionSource transactionSource) {
        this.transactionSource = transactionSource;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiseAdispositionTransactionResponseDto that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(banqueName, that.banqueName) &&
                Objects.equals(miseADispositionNumber, that.miseADispositionNumber) &&
                Objects.equals(libelle, that.libelle) &&
                Objects.equals(observation, that.observation) &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperations, that.dateOperations) &&
                Objects.equals(tvaDeductible, that.tvaDeductible) &&
                Objects.equals(tvaCollectee, that.tvaCollectee) &&
                Objects.equals(debit, that.debit) &&
                Objects.equals(credit, that.credit) &&
                transactionSource == that.transactionSource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, banqueName, miseADispositionNumber, libelle, observation, dateEmission, dateValeur,
                dateOperations, tvaDeductible, tvaCollectee, debit, credit, transactionSource);
    }

    @Override
    public String toString() {
        return "MiseAdispositionTransactionResponseDto{" +
                "id=" + id +
                ", banqueName='" + banqueName + '\'' +
                ", miseADispositionNumber='" + miseADispositionNumber + '\'' +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperations=" + dateOperations +
                ", tvaDeductible=" + tvaDeductible +
                ", tvaCollectee=" + tvaCollectee +
                ", debit=" + debit +
                ", credit=" + credit +
                ", transactionSource=" + transactionSource +
                '}';
    }
}
