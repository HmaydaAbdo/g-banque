package staport.finance.gBanque.rapprochement.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity(name = "rapprochement")
public class Rapprochement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(name = "date_rapprochement", nullable = false)
    private YearMonth dateRapprochement;

    private BigDecimal soldeDebut;
    private BigDecimal soldeFin;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal tvaDeductible;
    private BigDecimal tvaCollectee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutRapprochement statut = StatutRapprochement.EN_COURS;

    @OneToMany(mappedBy = "rapprochement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> transactions = new ArrayList<>();

    // --- Champs de suivi (Audit Fields) ---
    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


    // --- Constructeurs ---
    public Rapprochement() {}

    // --- Getters et Setters ---


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }
    public YearMonth getDateRapprochement() { return dateRapprochement; }
    public void setDateRapprochement(YearMonth dateRapprochement) { this.dateRapprochement = dateRapprochement; }


    public BigDecimal getSoldeDebut() { return soldeDebut; }
    public void setSoldeDebut(BigDecimal soldeDebut) { this.soldeDebut = soldeDebut; }

    public BigDecimal getSoldeFin() { return soldeFin; }
    public void setSoldeFin(BigDecimal soldeFin) { this.soldeFin = soldeFin; }

    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }

    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }

    public BigDecimal getTvaDeductible() { return tvaDeductible; }
    public void setTvaDeductible(BigDecimal tvaDeductible) { this.tvaDeductible = tvaDeductible; }

    public BigDecimal getTvaCollectee() { return tvaCollectee; }
    public void setTvaCollectee(BigDecimal tvaCollectee) { this.tvaCollectee = tvaCollectee; }

    public StatutRapprochement getStatut() {
        return statut;
    }

    public void setStatut(StatutRapprochement statut) {
        this.statut = statut;
    }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }
    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
        transaction.setRapprochement(this);
    }
    public void removeTransaction(Transaction transaction) {
        this.transactions.remove(transaction);
        if (transaction != null) {
            transaction.setRapprochement(null);
        }
    }


    // --- equals, hashCode et toString ---


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Rapprochement that = (Rapprochement) o;
        return Objects.equals(id, that.id) && Objects.equals(banque, that.banque) && Objects.equals(dateRapprochement, that.dateRapprochement) && Objects.equals(soldeDebut, that.soldeDebut) && Objects.equals(soldeFin, that.soldeFin) && Objects.equals(totalDebit, that.totalDebit) && Objects.equals(totalCredit, that.totalCredit) && Objects.equals(tvaDeductible, that.tvaDeductible) && Objects.equals(tvaCollectee, that.tvaCollectee) && Objects.equals(transactions, that.transactions) && Objects.equals(createdAt, that.createdAt) && Objects.equals(updatedAt, that.updatedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, banque, dateRapprochement, soldeDebut, soldeFin, totalDebit, totalCredit, tvaDeductible, tvaCollectee, transactions, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Rapprochement{" +
                "id=" + id +
                ", banqueId=" + (banque != null ? banque.getId() : "null") +
                ", dateRapprochement=" + dateRapprochement +
                ", soldeDebut=" + soldeDebut +
                ", soldeFin=" + soldeFin +
                ", totalDebit=" + totalDebit +
                ", totalCredit=" + totalCredit +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}