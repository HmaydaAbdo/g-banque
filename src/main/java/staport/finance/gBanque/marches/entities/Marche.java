package staport.finance.gBanque.marches.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entité représentant un marché (contrat BTP) rattaché à une banque.
 *
 * <p>{@code prefinancements}/{@code droitConstates}/{@code factorings}/{@code decomptes} sont
 * des {@code @OneToMany(mappedBy = "marche")} <b>sans table de jointure</b> vers des entités qui
 * ne sont pas encore {@code @Audited} (modules non sweepés). Sur ce type de relation, Envers ne
 * peut pas reconstruire l'historique de la collection via une table de jointure inexistante — il
 * faut donc exclure complètement le champ avec {@code @NotAudited} (pas
 * {@code @Audited(targetAuditMode = NOT_AUDITED)}, réservé aux {@code @ManyToOne}/{@code @ManyToMany}/
 * relations avec table de jointure), sinon échec au démarrage Envers
 * ("audited relation ... to a not audited entity"). Cf. mémoire "Envers — NOT_AUDITED vs
 * @NotAudited". {@code banque} et {@code lignesFonctionnellesSpecifiques} pointent vers des
 * entités déjà auditées et n'ont besoin d'aucune annotation particulière.
 */
@Entity
@Audited
@Table(name = "marches", indexes = {
        @Index(name = "idx_marche_actif", columnList = "actif"),
        @Index(name = "idx_marche_banque_id", columnList = "banque_id")
})
@NamedEntityGraph(
        name = "Marche.avecBanque",
        attributeNodes = @NamedAttributeNode("banque")
)
public class Marche  {

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
    @Column(name = "date_debut_des_travaux")
    private LocalDate dateDebutDesTravaux ;

    @Column(name = "date_d_achievement_des_travaux")
    private LocalDate dateDAchievementDesTravaux ;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    /**
     * Archivage du marché — n'a aucun effet rétroactif sur l'historique déjà généré, mais
     * exclut définitivement le marché de tout calcul futur (agios, encours des lignes
     * fonctionnelles) dès qu'il est mis à {@code false}. Cf. {@code MarcheService},
     * {@code PrefinancementAgiosCalculateur}/{@code DroitConstateAgiosCalculateur}/
     * {@code FactoringAgiosCalculateur}, {@code EncoursCalculatorService}.
     */
    @Column(name = "actif", nullable = false)
    private boolean actif = true;

    @ManyToMany(mappedBy = "marches", fetch = FetchType.LAZY)
    private List<LigneFonctionnelle> lignesFonctionnelles = new ArrayList<>();


    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFonctionnelleSpecifique> lignesFonctionnellesSpecifiques = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<Prefinancement> prefinancements = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<DroitConstate> droitConstates = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<Factoring> factorings = new ArrayList<>();

    @OneToMany(mappedBy = "marche", cascade = CascadeType.ALL, orphanRemoval = true)
    @NotAudited
    private List<Decompte> decomptes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at", nullable = false)
    private LocalDate modifiedAt ;

    public Marche() {
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

    public LocalDate getDateDebutDesTravaux() {
        return dateDebutDesTravaux;
    }

    public void setDateDebutDesTravaux(LocalDate dateDebutDesTravaux) {
        this.dateDebutDesTravaux = dateDebutDesTravaux;
    }

    public LocalDate getDateDAchievementDesTravaux() {
        return dateDAchievementDesTravaux;
    }

    public void setDateDAchievementDesTravaux(LocalDate dateDAchievementDesTravaux) {
        this.dateDAchievementDesTravaux = dateDAchievementDesTravaux;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public List<LigneFonctionnelle> getLignesFonctionnelles() {
        return lignesFonctionnelles;
    }

    public void setLignesFonctionnelles(List<LigneFonctionnelle> lignesFonctionnelles) {
        this.lignesFonctionnelles = lignesFonctionnelles;
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

    public List<Factoring> getFactorings() {
        return factorings;
    }

    public void setFactorings(List<Factoring> factorings) {
        this.factorings = factorings;
    }

    public void addFactoring(Factoring factoring){
        this.factorings.add(factoring);
    }

    public void removeFactoring(Factoring factoring){
        this.factorings.remove(factoring);
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public LocalDate getModifiedAt() {
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
                ", banque=" + banque +
                ", actif=" + actif +
                ", lignesFonctionnelles=" + lignesFonctionnelles +
                ", lignesFonctionnellesSpecifiques=" + lignesFonctionnellesSpecifiques +
                ", prefinancements=" + prefinancements +
                ", droitConstates=" + droitConstates +
                ", factorings=" + factorings +
                ", decomptes=" + decomptes +
                ", createdAt=" + createdAt +
                ", modifiedAt=" + modifiedAt +
                '}';
    }
}