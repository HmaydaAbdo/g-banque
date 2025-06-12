package staport.finance.gBanque.Bank.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "banque")
public class Banque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(name = "logo_data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] logoData;

    @Column(nullable = false)
    private String nom;

    @Column(name = "numero_compte", nullable = false, unique = true, length = 24)
    private String numeroDeCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Devise devise;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String agence;

    @Column(name = "solde_actuel", nullable = false)
    private BigDecimal soldeActuel;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;


    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFonctionnelle> ligneFonctionnelles = new ArrayList<>();

    // --- Constructors ---
    public Banque() {
        // Default constructor required by JPA
    }

    public Banque(Long id, byte[] logoData, String nom, String numeroDeCompte, Devise devise, String address, String agence, BigDecimal soldeActuel, OffsetDateTime dateCreation, OffsetDateTime dateModification) {
        this.id = id;
        this.logoData = logoData;
        this.nom = nom;
        this.numeroDeCompte = numeroDeCompte;
        this.devise = devise;
        this.address = address;
        this.agence = agence;
        this.soldeActuel = soldeActuel;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public byte[] getLogoData() { return logoData; }
    public void setLogoData(byte[] logoData) { this.logoData = logoData; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getNumeroDeCompte() { return numeroDeCompte; }
    public void setNumeroDeCompte(String numeroDeCompte) { this.numeroDeCompte = numeroDeCompte; }

    public Devise getDevise() { return devise; }
    public void setDevise(Devise devise) { this.devise = devise; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAgence() { return agence; }
    public void setAgence(String agence) { this.agence = agence; }

    public BigDecimal getSoldeActuel() { return soldeActuel; }
    public void setSoldeActuel(BigDecimal soldeActuel) { this.soldeActuel = soldeActuel; }


    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    // --- Getters and Setters for the LigneFonctionnelle list ---
    public List<LigneFonctionnelle> getLigneFonctionnelles() {
        return ligneFonctionnelles;
    }

    public void setLigneFonctionnelles(List<LigneFonctionnelle> ligneFonctionnelles) {
        this.ligneFonctionnelles = ligneFonctionnelles;
    }

    // --- Helper methods to manage the bidirectional relationship (CRITICAL for orphanRemoval) ---
    public void addLigneFonctionnelle(LigneFonctionnelle ligneFonctionnelle) {
        ligneFonctionnelles.add(ligneFonctionnelle);
        ligneFonctionnelle.setBanque(this); // Set the inverse side
    }

    public void removeLigneFonctionnelle(LigneFonctionnelle ligneFonctionnelle) {
        ligneFonctionnelles.remove(ligneFonctionnelle);
        ligneFonctionnelle.setBanque(null); // Unset the inverse side
    }

    // --- JPA Lifecycle Callbacks ---
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
        if (this.soldeActuel == null) {
            this.soldeActuel = BigDecimal.ZERO;
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
        Banque banque = (Banque) o;
        return Objects.equals(id, banque.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- toString() ---
    @Override
    public String toString() {
        return "Banque{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", numeroDeCompte='" + numeroDeCompte + '\'' +
                ", devise=" + devise +
                '}';
    }
}