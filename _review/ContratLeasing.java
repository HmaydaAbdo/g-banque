package staport.finance.gBanque.leasing.contratLeasing.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité représentant un contrat de leasing.
 */
@Audited
@Entity
@Table(name = "contrat_leasing", indexes = {
        @Index(name = "idx_contrat_leasing_banque_id",     columnList = "banque_id"),
        @Index(name = "idx_contrat_leasing_contrat_cadre", columnList = "contrat_cadre_id"),
        @Index(name = "idx_contrat_leasing_type_contrat",  columnList = "type_contrat"),
        @Index(name = "idx_contrat_leasing_completed",     columnList = "completed")
})
public class ContratLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le numéro de contrat est obligatoire")
    @Column(name = "numero_contrat", nullable = false, unique = true)
    private String numeroContrat;

    @NotNull(message = "Une banque de prélèvement est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banqueDePrelevement;

    /**
     * Contrat cadre actif au moment de la signature.
     * Porte les conditions commerciales (plafond, période de validité) de l'organisme.
     */
    @NotNull(message = "Un contrat cadre est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_cadre_id", nullable = false)
    private ContratCadre contratCadre;

    @NotBlank(message = "L'immobilisation est obligatoire")
    @Column(nullable = false)
    private String immobilisation;

    @NotNull(message = "Le montant d'achat est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant d'achat doit être supérieur à zéro")
    @Column(name = "montant_achat", nullable = false)
    private BigDecimal montantAchatTTC;

    @NotNull(message = "La date de début est obligatoire")
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut ;

    @NotNull(message = "La période en mois est obligatoire")
    @Column(name = "periode_mois", nullable = false)
    private Integer periodeEnMois;

    @NotNull(message = "Le type de contrat de leasing est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false)
    private TypeContratLeasing typeContrat;

    @OneToMany(mappedBy = "contratLeasing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("numeroPhase ASC")
    private List<ContratLeasingPhase> phases = new ArrayList<>();

    @Column(name = "valeur_residuelle")
    private BigDecimal valeurResiduelle;

    @OneToMany(mappedBy = "contratLeasing", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EcheanceLeasing> echeances = new ArrayList<>();

    @Column(nullable = false)
    private boolean completed = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Constructeurs ---
    public ContratLeasing() { /* Constructeur par défaut requis par JPA */ }

    // --- Getters et Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNumeroContrat() { return numeroContrat; }
    public void setNumeroContrat(String numeroContrat) { this.numeroContrat = numeroContrat; }
    public BigDecimal getMontantAchatTTC() { return montantAchatTTC; }
    public void setMontantAchatTTC(BigDecimal montantAchatTTC) { this.montantAchatTTC = montantAchatTTC; }
    public String getImmobilisation() { return immobilisation; }
    public void setImmobilisation(String immobilisation) { this.immobilisation = immobilisation; }
    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }
    public Integer getPeriodeEnMois() { return periodeEnMois; }
    public void setPeriodeEnMois(Integer periodeEnMois) { this.periodeEnMois = periodeEnMois; }
    public BigDecimal getValeurResiduelle() { return valeurResiduelle; }
    public void setValeurResiduelle(BigDecimal valeurResiduelle) { this.valeurResiduelle = valeurResiduelle; }
    public TypeContratLeasing getTypeContrat() { return typeContrat; }
    public void setTypeContrat(TypeContratLeasing typeContrat) { this.typeContrat = typeContrat; }
    public Banque getBanqueDePrelevement() { return banqueDePrelevement; }
    public void setBanqueDePrelevement(Banque banqueDePrelevement) { this.banqueDePrelevement = banqueDePrelevement; }
    public ContratCadre getContratCadre() { return contratCadre; }
    public void setContratCadre(ContratCadre contratCadre) { this.contratCadre = contratCadre; }
    public List<ContratLeasingPhase> getPhases() { return phases; }
    public void setPhases(List<ContratLeasingPhase> phases) { this.phases = phases; }

    public List<EcheanceLeasing> getEcheances() {
        return echeances;
    }

    public void setEcheances(List<EcheanceLeasing> echeances) {
        this.echeances = echeances;
    }

    public void addPhase(ContratLeasingPhase phase) {
        this.phases.add(phase);
        phase.setContratLeasing(this);
    }
    public void removePhase(ContratLeasingPhase phase) {
        this.phases.remove(phase);
        phase.setContratLeasing(null);
    }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    // --- Callbacks du cycle de vie JPA ---
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

    // --- equals() et hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContratLeasing that = (ContratLeasing) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "ContratLeasing{" +
                "id=" + id +
                ", numeroContrat='" + numeroContrat + '\'' +
                ", montantAchatTTC=" + montantAchatTTC +
                ", immobilisation='" + immobilisation + '\'' +
                ", dateDebut=" + dateDebut +
                ", typeContrat=" + typeContrat +
                ", completed=" + completed +
                '}';
    }
}