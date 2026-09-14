package staport.finance.gBanque.leasing.contratCadre.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Contrat cadre d'un organisme de leasing.
 * Historise les conditions commerciales (plafond, période de validité) d'un organisme.
 * Un seul contrat cadre peut être à l'état {@code ACTIF} par organisme à un instant donné.
 */
@Audited
@Entity
@Table(name = "contrat_cadre", indexes = {
        @Index(name = "idx_contrat_cadre_organisme_id", columnList = "organisme_id"),
        @Index(name = "idx_contrat_cadre_statut",       columnList = "statut")
})
public class ContratCadre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisme_id", nullable = false)
    private OrganismeLeasing organisme;

    @NotNull
    @Column(name = "plafond", nullable = false, precision = 19, scale = 2)
    private BigDecimal plafond;

    @NotNull
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @NotNull
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private StatutContratCadre statut = StatutContratCadre.ACTIF;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Constructeurs ---
    public ContratCadre() {}

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public OrganismeLeasing getOrganisme() { return organisme; }
    public void setOrganisme(OrganismeLeasing organisme) { this.organisme = organisme; }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public StatutContratCadre getStatut() { return statut; }
    public void setStatut(StatutContratCadre statut) { this.statut = statut; }

    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    // --- Callbacks JPA ---
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation    = now;
        this.dateModification = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }

    // --- equals / hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContratCadre that = (ContratCadre) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "ContratCadre{id=" + id +
                ", organisme=" + (organisme != null ? organisme.getId() : null) +
                ", plafond=" + plafond +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", statut=" + statut + '}';
    }
}
