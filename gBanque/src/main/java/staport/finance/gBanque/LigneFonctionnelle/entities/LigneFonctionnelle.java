package staport.finance.gBanque.LigneFonctionnelle.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table( name = "ligne_fonctionnelle")
public class LigneFonctionnelle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nature_ligne_id", nullable = false)
    private NatureLigneFonctionnelle natureLigneFonctionnelle;

    @Column(nullable = false)
    private BigDecimal plafond;

    @Column(nullable = false)
    private BigDecimal taux;

    @Column(name = "date_depart", nullable = false)
    private OffsetDateTime dateDepart;

    @Column(name = "date_fin", nullable = false)
    private OffsetDateTime dateFin;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    @Column(nullable = false)
    private boolean actif = true;

    // --- Constructors ---
    public LigneFonctionnelle() {
        // Default constructor required by JPA
    }


    public LigneFonctionnelle(Long id, Banque banque, NatureLigneFonctionnelle natureLigneFonctionnelle, BigDecimal plafond, BigDecimal taux, OffsetDateTime dateDepart, OffsetDateTime dateFin, OffsetDateTime dateCreation, OffsetDateTime dateModification, boolean actif) {
        this.id = id;
        this.banque = banque;
        this.natureLigneFonctionnelle = natureLigneFonctionnelle;
        this.plafond = plafond;
        this.taux = taux;
        this.dateDepart = dateDepart;
        this.dateFin = dateFin;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.actif = actif; // Set actif
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

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

    public OffsetDateTime getDateDepart() { return dateDepart; }
    public void setDateDepart(OffsetDateTime dateDepart) { this.dateDepart = dateDepart; }

    public OffsetDateTime getDateFin() { return dateFin; }
    public void setDateFin(OffsetDateTime dateFin) { this.dateFin = dateFin; }

    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }


    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }


    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
        if (!this.actif) { // Ensure default is true if not set
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
        LigneFonctionnelle that = (LigneFonctionnelle) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LigneFonctionnelle{" +
                "id=" + id +
                ", banque=" + (banque != null ? banque.getNom() : "null") +
                ", natureLigneFonctionnelle=" + (natureLigneFonctionnelle != null ? natureLigneFonctionnelle.getLabel() : "null") +
                ", plafond=" + plafond +
                ", taux=" + taux +
                ", dateDepart=" + dateDepart +
                ", dateFin=" + dateFin +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                '}';
    }
}