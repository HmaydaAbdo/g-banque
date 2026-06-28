package staport.finance.gBanque.leasing.lesOrganismsLeasing.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Audited
@Entity
@Table(name = "organisme_leasing", indexes = {
        @Index(name = "idx_organisme_leasing_nom", columnList = "nom")
})
public class OrganismeLeasing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String nom;

    @OneToMany(mappedBy = "organisme", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ContratCadre> contratsCadres = new ArrayList<>();

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Constructeurs ---
    public OrganismeLeasing() {}

    // --- Getters / Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public List<ContratCadre> getContratsCadres() { return contratsCadres; }
    public void setContratsCadres(List<ContratCadre> contratsCadres) { this.contratsCadres = contratsCadres; }

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
        return "OrganismeLeasing{id=" + id + ", nom='" + nom + "'}";
    }
}