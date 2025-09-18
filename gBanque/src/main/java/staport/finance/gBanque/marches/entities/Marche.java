package staport.finance.gBanque.marches.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;

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

    // New fields
    @Column(name = "date_debut_des_travaux", nullable = false)
    private OffsetDateTime dateDebutDesTravaux;

    @Column(name = "date_d_achievement_des_travaux", nullable = false)
    private OffsetDateTime dateDAchievementDesTravaux;

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFonctionnelleSpecifique> lignesFonctionnellesSpecifiques = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prefinancement> prefinancements = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DroitConstate> droitConstates = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Decompte> decomptes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private OffsetDateTime modifiedAt;

    // Default constructor for JPA
    protected Marche() {
    }

    public Marche(String numero, String nom, BigDecimal montant, String maitreDouvrage, OffsetDateTime dateDebutDesTravaux, OffsetDateTime dateDAchievementDesTravaux) {
        this.numero = numero;
        this.nom = nom;
        this.montant = montant;
        this.maitreDouvrage = maitreDouvrage;
        this.dateDebutDesTravaux = dateDebutDesTravaux;
        this.dateDAchievementDesTravaux = dateDAchievementDesTravaux;
    }

    // --- Getters and Setters ---

    public Long getId() {
        return id;
    }

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

    public OffsetDateTime getDateDebutDesTravaux() {
        return dateDebutDesTravaux;
    }

    public void setDateDebutDesTravaux(OffsetDateTime dateDebutDesTravaux) {
        this.dateDebutDesTravaux = dateDebutDesTravaux;
    }

    public OffsetDateTime getDateDAchievementDesTravaux() {
        return dateDAchievementDesTravaux;
    }

    public void setDateDAchievementDesTravaux(OffsetDateTime dateDAchievementDesTravaux) {
        this.dateDAchievementDesTravaux = dateDAchievementDesTravaux;
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
    public List<Prefinancement> getPrefinancements() {
        return prefinancements;
    }

    public void setPrefinancements(List<Prefinancement> prefinancements) {
        this.prefinancements = prefinancements;
    }

    public void addPrefinancement(Prefinancement prefinancement) {
        this.prefinancements.add(prefinancement);
        prefinancement.setMarche(this);
    }

    public void removePrefinancement(Prefinancement prefinancement) {
        this.prefinancements.remove(prefinancement);
        prefinancement.setMarche(null);
    }

    public List<DroitConstate> getDroitConstates() {
        return droitConstates;
    }

    public void setDroitConstates(List<DroitConstate> droitConstates) {
        this.droitConstates = droitConstates;
    }

    public void addDroitConstate(DroitConstate dc) {
        this.droitConstates.add(dc);
        dc.setMarche(this);
    }

    public void removeDroitConstate(DroitConstate dc) {
        this.droitConstates.remove(dc);
        dc.setMarche(null);
    }

    public List<Decompte> getDecomptes() {
        return decomptes;
    }

    public void setDecomptes(List<Decompte> decomptes) {
        this.decomptes = decomptes;
    }

    public void addDecompte(Decompte decompte){
        this.decomptes.add(decompte);
    }

    public void removeDecompte(Decompte decompte){
        this.decomptes.remove(decompte);
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
                ", dateDebutDesTravaux=" + dateDebutDesTravaux +
                ", dateDAchievementDesTravaux=" + dateDAchievementDesTravaux +
                ", createdAt=" + createdAt +
                ", modifiedAt=" + modifiedAt +
                '}';
    }
}