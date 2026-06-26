package staport.finance.gBanque.leasing.echeancesLeasing.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entité représentant une échéance individuelle d'un contrat de leasing.
 * Contient les montants calculés pour un paiement spécifique.
 */
@Audited
@Entity
@Table(name = "echeance_leasing", indexes = {
        @Index(name = "idx_echeance_leasing_contrat_id", columnList = "contrat_leasing_id"),
        @Index(name = "idx_echeance_leasing_statut_paiement", columnList = "statut_paiement"),
        @Index(name = "idx_echeance_leasing_date_echeance", columnList = "date_echeance")
})
public class EcheanceLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation ManyToOne : Chaque échéance est liée à un contrat d e leasing.
    @NotNull(message = "Le contrat de leasing est obligatoire")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_leasing_id", nullable = false)
    private ContratLeasing contratLeasing;

    @NotNull(message = "Le numéro d'échéance est obligatoire")
    @Column(name = "numero_echeance", nullable = false)
    private Integer numeroEcheance; // Ordre de l'échéance dans le contrat (ex: 1, 2, 3...)

    @NotNull(message = "La date d'échéance est obligatoire")
    @Column(name = "date_echeance", nullable = false)
    private LocalDate dateEcheance ; // Date à laquelle l'échéance doit être payée

    private LocalDate dateOperation ;
    private LocalDate dateValeur ;

    @NotNull(message = "Le montant TTC de l'échéance est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant TTC de l'échéance doit être supérieur à zéro")
    @Column(name = "montant_ttc", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantTTC;

    @NotNull(message = "Le statut de paiement est obligatoire")
    @Enumerated(EnumType.STRING)
    @Column(name = "statut_paiement", nullable = false)
    private StatutEcheanceLeasing statutPaiement; // État de l'échéance (prévue, payée, en retard, etc.)

    private String observation;
    private String libelle;

    // Champs d'audit
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;


    // --- Constructeurs ---
    public EcheanceLeasing() {}

    public EcheanceLeasing(ContratLeasing contratLeasing, Integer numeroEcheance, LocalDate dateEcheance, BigDecimal montantTTC,
                           StatutEcheanceLeasing statutPaiement) {
        this.contratLeasing = contratLeasing;
        this.numeroEcheance = numeroEcheance;
        this.dateEcheance = dateEcheance;
        this.montantTTC = montantTTC;
        this.statutPaiement = statutPaiement;
    }

    public boolean isPaid(){
        return this.statutPaiement==StatutEcheanceLeasing.PAYE;
    }
    // --- Getters et Setters ---
    public Long getId() { return id; }
    public ContratLeasing getContratLeasing() { return contratLeasing; }
    public void setContratLeasing(ContratLeasing contratLeasing) { this.contratLeasing = contratLeasing; }
    public Integer getNumeroEcheance() { return numeroEcheance; }
    public void setNumeroEcheance(Integer numeroEcheance) { this.numeroEcheance = numeroEcheance; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public BigDecimal getMontantTTC() {
        return montantTTC;
    }
    public void setMontantTTC(BigDecimal montantTTC) {
        this.montantTTC = montantTTC;
    }
    public LocalDate getDateOperation() {
        return dateOperation;
    }
    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }
    public LocalDate getDateValeur() {
        return dateValeur;
    }
    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }
    public StatutEcheanceLeasing getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutEcheanceLeasing statutPaiement) { this.statutPaiement = statutPaiement; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

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
        EcheanceLeasing that = (EcheanceLeasing) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "EcheanceLeasing{" +
                "id=" + id +
                ", statutPaiement=" + statutPaiement +
                ", montantTTC=" + montantTTC +
                ", dateEcheance=" + dateEcheance +
                ", numeroEcheance=" + numeroEcheance +
                '}';
    }
}