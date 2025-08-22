package staport.finance.gBanque.Bank.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Cheque;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Lcn;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.MiseADisposition;
import staport.finance.gBanque.lesEtatsDesEncours.core.entities.Virement;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.entities.Transaction;

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


    // One-to-many relationships with cascade and orphan removal for proper lifecycle management.
    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneFonctionnelle> ligneFonctionnelles = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Cheque> cheques = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Lcn> lcns = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Virement> virements = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MiseADisposition> misesADisposition = new ArrayList<>();

    @OneToMany(mappedBy = "banqueDePrelevement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ContratLeasing> contratsLeasing = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CreditMoyenTerme> creditMoyenTermes = new ArrayList<>();

    @OneToMany(mappedBy = "banqueDePrelevement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();


    // --- Constructors ---
    public Banque() {
        // Default constructor required by JPA
    }

    // A more practical constructor for creation
    public Banque(byte[] logoData, String nom, String numeroDeCompte, Devise devise, String address, String agence) {
        this.logoData = logoData;
        this.nom = nom;
        this.numeroDeCompte = numeroDeCompte;
        this.devise = devise;
        this.address = address;
        this.agence = agence;
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

    public List<Cheque> getCheques() {
        return cheques;
    }

    public void setCheques(List<Cheque> cheques) {
        this.cheques = cheques;
    }

    public List<ContratLeasing> getContratsLeasing() {
        return contratsLeasing;
    }

    public void setContratsLeasing(List<ContratLeasing> contratsLeasing) {
        this.contratsLeasing = contratsLeasing;
    }

    public List<CreditMoyenTerme> getCreditMoyenTermes() {
        return creditMoyenTermes;
    }

    public void setCreditMoyenTermes(List<CreditMoyenTerme> creditMoyenTermes) {
        this.creditMoyenTermes = creditMoyenTermes;
    }

    // Getters and Setters for the Lcn list
    public List<Lcn> getLcns() {
        return lcns;
    }
    public void setLcns(List<Lcn> lcns) {
        this.lcns = lcns;
    }

    // Getters and Setters for the Virement list
    public List<Virement> getVirements() {
        return virements;
    }
    public void setVirements(List<Virement> virements) {
        this.virements = virements;
    }

    // Getters and Setters for the MiseADisposition list
    public List<MiseADisposition> getMisesADisposition() {
        return misesADisposition;
    }
    public void setMisesADisposition(List<MiseADisposition> misesADisposition) {
        this.misesADisposition = misesADisposition;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // --- Helper methods to manage the bidirectional relationship ---
    public void addLigneFonctionnelle(LigneFonctionnelle ligneFonctionnelle) {
        ligneFonctionnelles.add(ligneFonctionnelle);
        ligneFonctionnelle.setBanque(this);
    }

    public void removeLigneFonctionnelle(LigneFonctionnelle ligneFonctionnelle) {
        ligneFonctionnelles.remove(ligneFonctionnelle);
        ligneFonctionnelle.setBanque(null);
    }

    public void addCheque(Cheque cheque) {
        cheques.add(cheque);
        cheque.setBanque(this);
    }

    public void removeCheque(Cheque cheque) {
        cheques.remove(cheque);
        cheque.setBanque(null);
    }

    public void addLcn(Lcn lcn) {
        lcns.add(lcn);
        lcn.setBanque(this);
    }

    public void removeLcn(Lcn lcn) {
        lcns.remove(lcn);
        lcn.setBanque(null);
    }

    public void addVirement(Virement virement) {
        virements.add(virement);
        virement.setBanque(this);
    }

    public void removeVirement(Virement virement) {
        virements.remove(virement);
        virement.setBanque(null);
    }

    public void addMiseADisposition(MiseADisposition miseADisposition) {
        misesADisposition.add(miseADisposition);
        miseADisposition.setBanque(this);
    }

    public void removeMiseADisposition(MiseADisposition miseADisposition) {
        misesADisposition.remove(miseADisposition);
        miseADisposition.setBanque(null);
    }

    // You would add similar helper methods for ContratLeasing, CreditMoyenTerme, and Transaction

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