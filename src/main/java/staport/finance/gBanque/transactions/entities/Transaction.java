package staport.finance.gBanque.transactions.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OffsetDateTime dateEmission;
    private OffsetDateTime dateValeur;
    private OffsetDateTime dateOperations;

    @Column(precision = 19, scale = 2)
    private BigDecimal debit;

    @Column(precision = 19, scale = 2)
    private BigDecimal credit;

    @Column(columnDefinition = "LONGTEXT")
    private String libelle;
    private String observation;

    private BigDecimal tvaDeductible;
    private BigDecimal tvaCollectee;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private TransactionSource transactionSource;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id")
    private Banque banqueDePrelevement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapprochement_id", nullable = true)
    private Rapprochement rapprochement;

    @Column(columnDefinition = "TEXT")
    private String traces;

    // New fields for tracking creation and update timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private Long referent;

    private boolean isConfirmed=false;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

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

    public OffsetDateTime getDateOperations() {
        return dateOperations;
    }

    public void setDateOperations(OffsetDateTime dateOperations) {
        this.dateOperations = dateOperations;
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

    public String getTraces() {
        return traces;
    }

    public void setTraces(String traces) {
        this.traces = traces;
    }

    public Banque getBanqueDePrelevement() {
        return banqueDePrelevement;
    }

    public void setBanqueDePrelevement(Banque banqueDePrelevement) {
        this.banqueDePrelevement = banqueDePrelevement;
    }

    public Rapprochement getRapprochement() {
        return rapprochement;
    }

    public void setRapprochement(Rapprochement rapprochement) {
        this.rapprochement = rapprochement;
    }

    // Getters for the new timestamp fields
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(boolean confirmed) {
        isConfirmed = confirmed;
    }

    public Long getReferent() {
        return referent;
    }

    public void setReferent(Long referent) {
        this.referent = referent;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transaction that)) return false;
        return isConfirmed == that.isConfirmed && Objects.equals(id, that.id) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateValeur, that.dateValeur) && Objects.equals(dateOperations, that.dateOperations) && Objects.equals(debit, that.debit) && Objects.equals(credit, that.credit) && Objects.equals(libelle, that.libelle) && Objects.equals(observation, that.observation) && Objects.equals(tvaDeductible, that.tvaDeductible) && Objects.equals(tvaCollectee, that.tvaCollectee) && transactionSource == that.transactionSource && Objects.equals(banqueDePrelevement, that.banqueDePrelevement) && Objects.equals(rapprochement, that.rapprochement) && Objects.equals(traces, that.traces) && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt) && Objects.equals(referent, that.referent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateEmission, dateValeur, dateOperations, debit, credit, libelle, observation, tvaDeductible, tvaCollectee, transactionSource, banqueDePrelevement, rapprochement, traces, createdAt, updatedAt, referent, isConfirmed);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperations=" + dateOperations +
                ", debit=" + debit +
                ", credit=" + credit +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                ", tvaCollectee=" + tvaCollectee +
                ", transactionSource=" + transactionSource +
                ", banqueDePrelevement=" + banqueDePrelevement +
                ", rapprochement=" + rapprochement +
                ", traces='" + traces + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", referent=" + referent +
                ", isConfirmed=" + isConfirmed +
                '}';
    }
}
