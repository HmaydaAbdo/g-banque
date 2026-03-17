package staport.finance.gBanque.validation.dtos.virementInternationaux;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class VirementInternationalTransactionResponseDto {

    private Long id;
    private String banqueName;
    private String libelle;
    private String observation;
    private LocalDate dateEmission ;
    private LocalDate dateValeur ;
    private LocalDate dateOperations ;
    private BigDecimal tvaDeductible;
    private BigDecimal tvaCollectee;
    private BigDecimal debit;
    private BigDecimal credit;
    private final TransactionSource transactionSource = TransactionSource.VIREMENT_INTERNATIONAL;

    // Default constructor
    public VirementInternationalTransactionResponseDto() {
    }

    // All-args constructor
    public VirementInternationalTransactionResponseDto(Long id, String banqueName,
                                                       String libelle, String observation, LocalDate dateEmission,
                                                       LocalDate dateValeur, LocalDate dateOperations,
                                                       BigDecimal tvaDeductible, BigDecimal tvaCollectee,
                                                       BigDecimal debit, BigDecimal credit) {
        this.id = id;
        this.banqueName = banqueName;
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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBanqueName() { return banqueName; }
    public void setBanqueName(String banqueName) { this.banqueName = banqueName; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public String getObservation() { return observation; }
    public void setObservation(String observation) { this.observation = observation; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public LocalDate getDateValeur() { return dateValeur; }
    public void setDateValeur(LocalDate dateValeur) { this.dateValeur = dateValeur; }

    public LocalDate getDateOperations() { return dateOperations; }
    public void setDateOperations(LocalDate dateOperations) { this.dateOperations = dateOperations; }

    public BigDecimal getTvaDeductible() { return tvaDeductible; }
    public void setTvaDeductible(BigDecimal tvaDeductible) { this.tvaDeductible = tvaDeductible; }

    public BigDecimal getTvaCollectee() { return tvaCollectee; }
    public void setTvaCollectee(BigDecimal tvaCollectee) { this.tvaCollectee = tvaCollectee; }

    public BigDecimal getDebit() { return debit; }
    public void setDebit(BigDecimal debit) { this.debit = debit; }

    public BigDecimal getCredit() { return credit; }
    public void setCredit(BigDecimal credit) { this.credit = credit; }

    public TransactionSource getTransactionSource() { return transactionSource; }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirementInternationalTransactionResponseDto)) return false;
        VirementInternationalTransactionResponseDto that = (VirementInternationalTransactionResponseDto) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(banqueName, that.banqueName) &&
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
        return Objects.hash(id, banqueName, libelle, observation, dateEmission,
                dateValeur, dateOperations, tvaDeductible, tvaCollectee, debit, credit, transactionSource);
    }

    // toString
    @Override
    public String toString() {
        return "VirementInternationalTransactionResponseDto{" +
                "id=" + id +
                ", banqueName='" + banqueName + '\'' +
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