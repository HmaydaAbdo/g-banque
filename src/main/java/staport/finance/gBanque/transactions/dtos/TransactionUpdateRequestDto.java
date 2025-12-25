package staport.finance.gBanque.transactions.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class TransactionUpdateRequestDto {

    @NotNull(message = "date d emission est obligatoire")
    private OffsetDateTime dateEmission;

    @NotNull(message = "date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "date operation est obligatoire")
    private OffsetDateTime dateOperations;

    @NotNull(message = "libelle est obligatoire")
    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    @NotNull(message = "observation est obligatoire")
    @NotBlank(message = "observation est obligatoire")
    private String observation;

    @NotNull(message = "tvaDeductible est obligatoire")
    private BigDecimal tvaDeductible;

    @NotNull(message = "tvaCollectee est obligatoire")
    private BigDecimal tvaCollectee;

    @NotNull(message = "source de transaction est obligatoire")
    private TransactionSource transactionSource;

    @NotNull(message = "debit est obligatoire")
    private BigDecimal debit;

    @NotNull(message = "credit est obligatoire")
    private BigDecimal credit;

    @NotNull(message = "id de la banque est obligatoire")
    private Long banqueId;

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public OffsetDateTime getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(OffsetDateTime dateOperations) {
        this.dateOperations = dateOperations;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
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

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(TransactionSource transactionSource) {
        this.transactionSource = transactionSource;
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

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }
}
