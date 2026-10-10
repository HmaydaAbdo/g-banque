package staport.finance.gBanque.transactions.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entité représentant un mouvement bancaire élémentaire (débit ou crédit) rattaché à un
 * {@link Rapprochement}. Écrite directement par les {@code XxxValidationService} des modules
 * source (leasing, moyens de paiement, agios, etc. — voir {@code architecture.md}, Règle 1).
 */
@Entity
@Audited
@Table(
    name = "transaction",
    indexes = {
        @Index(name = "idx_transaction_banque_id",         columnList = "banque_id"),
        @Index(name = "idx_transaction_rapprochement_id",  columnList = "rapprochement_id"),
        @Index(name = "idx_transaction_source",            columnList = "transaction_source"),
        @Index(name = "idx_transaction_date_operations",   columnList = "date_operations"),
        @Index(name = "idx_transaction_date_emission",     columnList = "date_emission")
    }
)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate dateEmission;
    private LocalDate dateValeur;
    private LocalDate dateOperations;

    @Column(precision = 19, scale = 2)
    private BigDecimal debit=BigDecimal.ZERO;

    @Column(precision = 19, scale = 2)
    private BigDecimal credit=BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String libelle;
    @Column(nullable = false)
    private String observation;

    private BigDecimal tvaDeductible=BigDecimal.ZERO;
    private BigDecimal tvaCollectee=BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private TransactionSource transactionSource;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id")
    private Banque banqueDePrelevement;

    /**
     * {@code Rapprochement} est désormais {@code @Audited} (sweep Rapprochement) — aucune exclusion
     * Envers nécessaire sur cette relation.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rapprochement_id", nullable = false)
    private Rapprochement rapprochement;



    // New fields for tracking creation and update timestamps
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    private Long referent;

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


    public Long getReferent() {
        return referent;
    }

    public void setReferent(Long referent) {
        this.referent = referent;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Transaction that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateValeur, that.dateValeur) && Objects.equals(dateOperations, that.dateOperations) && Objects.equals(debit, that.debit) && Objects.equals(credit, that.credit) && Objects.equals(libelle, that.libelle) && Objects.equals(observation, that.observation) && Objects.equals(tvaDeductible, that.tvaDeductible) && Objects.equals(tvaCollectee, that.tvaCollectee) && transactionSource == that.transactionSource && Objects.equals(banqueDePrelevement, that.banqueDePrelevement) && Objects.equals(rapprochement, that.rapprochement)  && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt) && Objects.equals(referent, that.referent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dateEmission, dateValeur, dateOperations, debit, credit, libelle, observation, tvaDeductible, tvaCollectee, transactionSource, banqueDePrelevement, rapprochement, createdAt, updatedAt, referent);
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
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", referent=" + referent +
                '}';
    }
}