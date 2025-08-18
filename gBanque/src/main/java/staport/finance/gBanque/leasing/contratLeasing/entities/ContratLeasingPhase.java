package staport.finance.gBanque.leasing.contratLeasing.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Entité représentant une phase spécifique d'un contrat de leasing.
 * Utilisée pour les contrats PROGRESSIF ou DEGRESSIF où le montant du loyer HT varie sur différentes périodes.
 */
@Entity
@Table(name = "contrat_leasing_phase")
public class ContratLeasingPhase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relation ManyToOne vers ContratLeasing : plusieurs phases peuvent appartenir à un même contrat.
    @NotNull(message = "Le contrat de leasing est obligatoire pour une phase")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_leasing_id", nullable = false)
    private ContratLeasing contratLeasing;

    @NotNull(message = "Le numéro de phase est obligatoire")
    @Min(value = 1, message = "Le numéro de phase doit être supérieur ou égal à 1")
    @Column(name = "numero_phase", nullable = false)
    private Integer numeroPhase; // Ordre de la phase (ex: 1, 2, 3...)

    @NotNull(message = "La durée de la phase en mois est obligatoire")
    @Min(value = 1, message = "La durée de la phase doit être d'au moins 1 mois")
    @Column(name = "duree_mois", nullable = false)
    private Integer dureeMois; // Durée de cette phase en mois

    @NotNull(message = "Le montant de loyer TTC pour cette phase est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant de loyer TTC doit être supérieur à zéro")
    @Column(name = "mensualite_ttc", nullable = false)
    // Montant HT du loyer applicable à chaque échéance durant cette phase.
    private BigDecimal mensualiteTTC;

    // Champs d'audit
    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Constructeurs ---
    public ContratLeasingPhase() {}

    public ContratLeasingPhase(ContratLeasing contratLeasing, Integer numeroPhase, Integer dureeMois, BigDecimal mensualiteTTC) {
        this.contratLeasing = contratLeasing;
        this.numeroPhase = numeroPhase;
        this.dureeMois = dureeMois;
        this.mensualiteTTC = mensualiteTTC;
    }

    // --- Getters et Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    // ... (autres getters et setters) ...
    public ContratLeasing getContratLeasing() { return contratLeasing; }
    public void setContratLeasing(ContratLeasing contratLeasing) { this.contratLeasing = contratLeasing; }

    public Integer getNumeroPhase() { return numeroPhase; }
    public void setNumeroPhase(Integer numeroPhase) { this.numeroPhase = numeroPhase; }

    public Integer getDureeMois() { return dureeMois; }
    public void setDureeMois(Integer dureeMois) { this.dureeMois = dureeMois; }

    public BigDecimal getMensualiteTTC() {
        return mensualiteTTC;
    }
    public void setMensualiteTTC(BigDecimal mensualiteTTC) {
        this.mensualiteTTC = mensualiteTTC;
    }

    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

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
        ContratLeasingPhase that = (ContratLeasingPhase) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "ContratLeasingPhase{" +
                "id=" + id +
                ", numeroPhase=" + numeroPhase +
                ", dureeMois=" + dureeMois +
                ", mensualité=" + mensualiteTTC +
                '}';
    }
}