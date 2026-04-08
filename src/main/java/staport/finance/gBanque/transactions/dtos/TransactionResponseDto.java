package staport.finance.gBanque.transactions.dtos;

import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class TransactionResponseDto {

    private Long id;
    private LocalDate dateEmission;
    private LocalDate dateValeur;
    private LocalDate dateOperations;
    private String observation;
    private BigDecimal tvaDeductible;
    private BigDecimal tvaCollectee;
    private String libelle;
    private TransactionSource transactionSource;
    private BigDecimal debit;
    private BigDecimal credit;
    private String traces;
    private Long banqueDePrelevementId;
    private String banqueDePrelevementNom;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private StatutRapprochement statutRapprochement;
    private long referent;

    // Constructors, Getters, and Setters
    public TransactionResponseDto() {
    }

    public TransactionResponseDto(Long id, LocalDate dateEmission, LocalDate dateValeur, LocalDate dateOperations, String observation, BigDecimal tvaDeductible, BigDecimal tvaCollectee, String libelle, TransactionSource transactionSource, BigDecimal debit, BigDecimal credit, String traces, Long banqueDePrelevementId, String banqueDePrelevementNom, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperations = dateOperations;
        this.observation = observation;
        this.tvaDeductible = tvaDeductible;
        this.tvaCollectee = tvaCollectee;
        this.libelle = libelle;
        this.transactionSource = transactionSource;
        this.debit = debit;
        this.credit = credit;
        this.traces = traces;
        this.banqueDePrelevementId = banqueDePrelevementId;
        this.banqueDePrelevementNom = banqueDePrelevementNom;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
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

    public String getTraces() {
        return traces;
    }

    public void setTraces(String traces) {
        this.traces = traces;
    }

    public Long getBanqueDePrelevementId() {
        return banqueDePrelevementId;
    }

    public void setBanqueDePrelevementId(Long banqueDePrelevementId) {
        this.banqueDePrelevementId = banqueDePrelevementId;
    }

    public String getBanqueDePrelevementNom() {
        return banqueDePrelevementNom;
    }

    public void setBanqueDePrelevementNom(String banqueDePrelevementNom) {
        this.banqueDePrelevementNom = banqueDePrelevementNom;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public StatutRapprochement getStatutRapprochement() {
        return statutRapprochement;
    }

    public void setStatutRapprochement(StatutRapprochement statutRapprochement) {
        this.statutRapprochement = statutRapprochement;
    }

    public long getReferent() {
        return referent;
    }

    public void setReferent(long referent) {
        this.referent = referent;
    }

    @Override
    public String toString() {
        return "TransactionResponseDto{" +
                "id=" + id +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperations=" + dateOperations +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                ", tvaCollectee=" + tvaCollectee +
                ", libelle='" + libelle + '\'' +
                ", transactionSource=" + transactionSource +
                ", debit=" + debit +
                ", credit=" + credit +
                ", traces='" + traces + '\'' +
                ", banqueDePrelevementId=" + banqueDePrelevementId +
                ", banqueDePrelevementNom='" + banqueDePrelevementNom + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", statutRapprochement=" + statutRapprochement +
                ", referent=" + referent +
                '}';
    }
}
