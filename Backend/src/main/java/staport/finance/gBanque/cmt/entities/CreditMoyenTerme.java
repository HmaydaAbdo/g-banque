package staport.finance.gBanque.cmt.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Audited
@Table(
        name = "credit_moyen_terme",
        indexes = {
                @Index(name = "idx_credit_moyen_terme_banque_id", columnList = "banque_id"),
                @Index(name = "idx_credit_moyen_terme_completed", columnList = "completed"),
                @Index(name = "idx_credit_moyen_terme_date_debut", columnList = "date_debut")
        }
)
public class CreditMoyenTerme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Une banque de prélèvement est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @NotBlank(message = "designation est obligatoire")
    @Column(nullable = false)
    private String designation;

    @NotNull(message = "nominal est obligatoire")
    @DecimalMin(value = "0.01", message = "nominal doit être supérieur à zéro")
    @Column(nullable = false)
    private BigDecimal nominal;

    @NotNull(message = "La periodicite est obligatoire")
    @Column(nullable = false)
    private Integer periodicite;

    @NotNull(message = "La duree est obligatoire")
    @Column(nullable = false)
    private Integer duree;

    @NotNull(message = "L échéance est obligatoire")
    @Column(nullable = false)
    private BigDecimal echeance;

    @NotNull(message = "le taux est obligatoire")
    @DecimalMin(value = "0.01", message = "le taux doit être supérieur à zéro")
    @Column(nullable = false)
    private BigDecimal taux;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut ;

    @OneToMany(mappedBy = "creditMoyenTerme", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EcheanceCMT> echeanceCMTS = new ArrayList<>();

    // New field to indicate if the credit is completed
    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // Constructeur par défaut (obligatoire pour JPA)
    public CreditMoyenTerme() {}

    // Getters and Setters for all fields
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public BigDecimal getNominal() { return nominal; }
    public void setNominal(BigDecimal nominal) { this.nominal = nominal; }
    public Integer getPeriodicite() { return periodicite; }
    public void setPeriodicite(Integer periodicite) { this.periodicite = periodicite; }
    public Integer getDuree() { return duree; }
    public void setDuree(Integer duree) { this.duree = duree; }
    public BigDecimal getEcheance() { return echeance; }
    public void setEcheance(BigDecimal echeance) { this.echeance = echeance; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
    public List<EcheanceCMT> getEcheanceCMTS() { return echeanceCMTS; }
    public void setEcheanceCMTS(List<EcheanceCMT> echeanceCMTS) { this.echeanceCMTS = echeanceCMTS; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    // JPA callbacks for audit fields
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CreditMoyenTerme that = (CreditMoyenTerme) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}