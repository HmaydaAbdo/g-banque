package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.marches.entities.Marche;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "ligne_fonctionnelle_specifique")
public class LigneFonctionnelleSpecifique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nature_ligne_id", nullable = false)
    private NatureLigneFonctionnelle natureLigneFonctionnelle;

    @Column(nullable = false)
    private BigDecimal plafond;

    @Column(nullable = false)
    private BigDecimal taux;

    @Column(name = "date_depart", nullable = false)
    private LocalDate dateDepart ;

    @Column(name = "date_fin", nullable = false)
    private LocalDate dateFin ;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    @Column(nullable = false)
    private boolean actif = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marche_id", nullable = false)
    private Marche marche;

    // --- Constructors ---
    public LigneFonctionnelleSpecifique() {
        // Default constructor required by JPA
    }

    public LigneFonctionnelleSpecifique(Long id , NatureLigneFonctionnelle natureLigneFonctionnelle, BigDecimal plafond, BigDecimal taux, LocalDate dateDepart, LocalDate dateFin, OffsetDateTime dateCreation, OffsetDateTime dateModification, boolean actif, Marche marche) {
        this.id = id;
        this.natureLigneFonctionnelle = natureLigneFonctionnelle;
        this.plafond = plafond;
        this.taux = taux;
        this.dateDepart = dateDepart;
        this.dateFin = dateFin;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.actif = actif;
        this.marche = marche;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public NatureLigneFonctionnelle getNatureLigneFonctionnelle() {
        return natureLigneFonctionnelle;
    }

    public void setNatureLigneFonctionnelle(NatureLigneFonctionnelle natureLigneFonctionnelle) {
        this.natureLigneFonctionnelle = natureLigneFonctionnelle;
    }

    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }

    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }

    public LocalDate getDateDepart() { return dateDepart; }
    public void setDateDepart(LocalDate dateDepart) { this.dateDepart = dateDepart; }

    public LocalDate getDateFin() { return dateFin; }
    public void setDateFin(LocalDate dateFin) { this.dateFin = dateFin; }

    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    public Marche getMarche() {
        return marche;
    }

    public void setMarche(Marche marche) {
        this.marche = marche;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
        if (!this.actif) {
            this.actif = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }


    // --- equals() and hashCode() ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LigneFonctionnelleSpecifique that = (LigneFonctionnelleSpecifique) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "LigneFonctionnelleSpecifique{" +
                "id=" + id +
                ", natureLigneFonctionnelle=" + (natureLigneFonctionnelle != null ? natureLigneFonctionnelle.getLabel() : "null") +
                ", plafond=" + plafond +
                ", taux=" + taux +
                ", dateDepart=" + dateDepart +
                ", dateFin=" + dateFin +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                ", marche=" + (marche != null ? marche.getNom() : "null") +
                '}';
    }
}