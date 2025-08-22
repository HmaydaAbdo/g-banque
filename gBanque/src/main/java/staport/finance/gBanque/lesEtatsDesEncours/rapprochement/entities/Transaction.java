package staport.finance.gBanque.lesEtatsDesEncours.rapprochement.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private OffsetDateTime dateEmission;
    private OffsetDateTime dateValeur;
    private OffsetDateTime dateOperations;

    @Column(columnDefinition = "LONGTEXT")
    private String libelle;

    private String observation;
    private Double tvaDeductible;
    private Double tvaCollectee;

    @Enumerated(EnumType.STRING)
    private TransactionSource transactionSource;

    @Column(precision = 19, scale = 2)
    private BigDecimal debit;

    @Column(precision = 19, scale = 2)
    private BigDecimal credit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id")
    private Banque banqueDePrelevement;

    @Column(columnDefinition = "TEXT")
    private String traces;

    // New fields for tracking creation and update timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

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

    public Double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(Double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public Double getTvaCollectee() {
        return tvaCollectee;
    }

    public void setTvaCollectee(Double tvaCollectee) {
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

    // Getters for the new timestamp fields
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
