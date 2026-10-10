package staport.finance.gBanque.rapprochement.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.common.YearMonthDateConverter;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Représente la période de rapprochement bancaire mensuel d'une banque (statut EN_COURS →
 * CLOTURE_PROVISOIRE → CLOTURE_DEFINITIVE). {@code banque} (déjà auditée) et {@code transactions}
 * (désormais auditée depuis le sweep Transactions) ne nécessitent aucune exclusion Envers — voir
 * mémoire {@code feedback_envers_audit_relations}.
 */
@Entity(name = "rapprochement")
@Audited
@Table(
        name = "rapprochement",
        indexes = {
                @Index(name = "idx_rapprochement_banque_id",     columnList = "banque_id"),
                @Index(name = "idx_rapprochement_statut",        columnList = "statut"),
                @Index(name = "idx_rapprochement_date",          columnList = "date_rapprochement"),
                @Index(name = "idx_rapprochement_banque_statut", columnList = "banque_id, statut")
        }
)
public class Rapprochement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Convert(converter = YearMonthDateConverter.class)
    @Column(name = "date_rapprochement", nullable = false)
    private YearMonth dateRapprochement;

    private BigDecimal soldeDebut;

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
        if (this == o) return true;
        if (!(o instanceof Rapprochement that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Rapprochement{" +
                "id=" + id +
                ", banqueId=" + (banque != null ? banque.getId() : "null") +
                ", dateRapprochement=" + dateRapprochement +
                ", soldeDebut=" + soldeDebut +
                ", statut=" + statut +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}