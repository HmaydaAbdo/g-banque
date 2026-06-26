package staport.finance.gBanque.leasing.lesOrganismsLeasing.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Audited
@Entity
@Table(name = "organisme_leasing", indexes = {
        @Index(name = "idx_organisme_leasing_nom", columnList = "nom"),
        @Index(name = "idx_organisme_leasing_dates", columnList = "date_debut, date_fin")
})
public class OrganismeLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nom;

    @NotNull
    @Column(name = "plafond", nullable = false)
    private BigDecimal plafond;

    @NotNull
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut ;

    @NotNull
    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin ;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Constructors ---
    public OrganismeLeasing() {}


    public OrganismeLeasing(String nom, BigDecimal plafond, LocalDate dateDebut, LocalDate dateFin) {
        this.nom = nom;
        this.plafond = plafond;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public LocalDate getDateDebut() { return dateDebut; }
    public void setDateDebut(LocalDate dateDebut) { this.dateDebut = dateDebut; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }


    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    // --- JPA Lifecycle Callbacks ---
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

    // --- equals(), hashCode(), toString() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrganismeLeasing that = (OrganismeLeasing) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "OrganismeLeasing{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", plafond=" + plafond +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                '}';
    }
}