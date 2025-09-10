package staport.finance.gBanque.marches.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "marches")
public class Marche implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", unique = true, nullable = false, length = 50)
    private String numero;

    @Column(name = "nom", nullable = false, length = 255)
    private String nom;

    @Column(name = "montant", precision = 19, scale = 2, nullable = false)
    private BigDecimal montant;

    @Column(name = "maitre_douvrage", nullable = false, length = 255)
    private String maitreDouvrage;

    @Column(name = "delai", nullable = false)
    private OffsetDateTime delai;

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFonctionnelleSpecifique> lignesFonctionnellesSpecifiques = new ArrayList<>();


    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private OffsetDateTime modifiedAt;

    // Default constructor for JPA
    protected Marche() {
    }

    public Marche(String numero, String nom, BigDecimal montant, String maitreDouvrage, OffsetDateTime delai) {
        this.numero = numero;
        this.nom = nom;
        this.montant = montant;
        this.maitreDouvrage = maitreDouvrage;
        this.delai = delai;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

    // No setter for 'id' as it's auto-generated

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getMaitreDouvrage() {
        return maitreDouvrage;
    }

    public void setMaitreDouvrage(String maitreDouvrage) {
        this.maitreDouvrage = maitreDouvrage;
    }

    public OffsetDateTime getDelai() {
        return delai;
    }

    public void setDelai(OffsetDateTime delai) {
        this.delai = delai;
    }


    public List<LigneFonctionnelleSpecifique> getLignesFonctionnellesSpecifiques() {
        return lignesFonctionnellesSpecifiques;
    }

    public void setLignesFonctionnellesSpecifiques(List<LigneFonctionnelleSpecifique> lignesFonctionnellesSpecifiques) {
        this.lignesFonctionnellesSpecifiques = lignesFonctionnellesSpecifiques;
    }

    public void addLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifique ligne) {
        this.lignesFonctionnellesSpecifiques.add(ligne);
        ligne.setMarche(this);
    }

    public void removeLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifique ligne) {
        this.lignesFonctionnellesSpecifiques.remove(ligne);
        ligne.setMarche(null);
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getModifiedAt() {
        return modifiedAt;
    }

    // --- Object Methods ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Marche marche = (Marche) o;
        return Objects.equals(id, marche.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Marche{" +
                "id=" + id +
                ", numero='" + numero + '\'' +
                ", nom='" + nom + '\'' +
                ", montant=" + montant +
                ", maitreDouvrage='" + maitreDouvrage + '\'' +
                ", delai=" + delai +
                ", createdAt=" + createdAt +
                ", modifiedAt=" + modifiedAt +
                '}';
    }
}