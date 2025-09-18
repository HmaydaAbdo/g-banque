package staport.finance.gBanque.Bank.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.transactions.entities.Transaction;

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

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String nom;

    @NotBlank
    @Pattern(regexp = "\\d{24}")
    @Column(name = "numero_compte", nullable = false, unique = true, length = 24)
    private String numeroDeCompte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Devise devise;

    @NotBlank
    @Column(nullable = false)
    private String address;

    @NotBlank
    @Column(nullable = false)
    private String agence;

    @Column(name = "solde_actuel", nullable = false)
    private BigDecimal soldeActuel = BigDecimal.ZERO;


    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;


    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Relations ---
    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<LigneFonctionnelle> ligneFonctionnelles = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LigneFonctionnelleSpecifique> ligneFonctionnellesSpecifiques = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Cheque> cheques = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Lcn> lcns = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Virement> virements = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<MiseADisposition> misesADisposition = new ArrayList<>();

    @OneToMany(mappedBy = "banqueDePrelevement", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ContratLeasing> contratsLeasing = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CreditMoyenTerme> creditMoyenTermes = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Decompte> decomptes = new ArrayList<>();

    @OneToMany(mappedBy = "banqueDePrelevement",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();


    // --- Constructors ---
    public Banque() {}

    public Banque(byte[] logoData, String nom, String numeroDeCompte, Devise devise, String address, String agence) {
        this.logoData = logoData;
        this.nom = nom;
        this.numeroDeCompte = numeroDeCompte;
        this.devise = devise;
        this.address = address;
        this.agence = agence;
        this.soldeActuel = BigDecimal.ZERO;
    }

    // --- Getters / Setters ---
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
    public OffsetDateTime getDateModification() { return dateModification; }

    public List<LigneFonctionnelle> getLigneFonctionnelles() { return ligneFonctionnelles; }
    public void setLigneFonctionnelles(List<LigneFonctionnelle> ligneFonctionnelles) { this.ligneFonctionnelles = ligneFonctionnelles; }

    public List<Cheque> getCheques() { return cheques; }
    public void setCheques(List<Cheque> cheques) { this.cheques = cheques; }

    public List<ContratLeasing> getContratsLeasing() { return contratsLeasing; }
    public void setContratsLeasing(List<ContratLeasing> contratsLeasing) { this.contratsLeasing = contratsLeasing; }

    public List<CreditMoyenTerme> getCreditMoyenTermes() { return creditMoyenTermes; }
    public void setCreditMoyenTermes(List<CreditMoyenTerme> creditMoyenTermes) { this.creditMoyenTermes = creditMoyenTermes; }

    public List<Lcn> getLcns() { return lcns; }
    public void setLcns(List<Lcn> lcns) { this.lcns = lcns; }

    public List<Virement> getVirements() { return virements; }
    public void setVirements(List<Virement> virements) { this.virements = virements; }

    public List<MiseADisposition> getMisesADisposition() { return misesADisposition; }
    public void setMisesADisposition(List<MiseADisposition> misesADisposition) { this.misesADisposition = misesADisposition; }

    public List<Transaction> getTransactions() { return transactions; }
    public void setTransactions(List<Transaction> transactions) { this.transactions = transactions; }

    // --- Helper methods for bidirectional relationships ---
    public void addLigneFonctionnelle(LigneFonctionnelle l) { ligneFonctionnelles.add(l); l.setBanque(this); }
    public void removeLigneFonctionnelle(LigneFonctionnelle l) { ligneFonctionnelles.remove(l); l.setBanque(null); }

    public void addCheque(Cheque c) { cheques.add(c); c.setBanque(this); }
    public void removeCheque(Cheque c) { cheques.remove(c); c.setBanque(null); }

    public void addLcn(Lcn l) { lcns.add(l); l.setBanque(this); }
    public void removeLcn(Lcn l) { lcns.remove(l); l.setBanque(null); }

    public void addVirement(Virement v) { virements.add(v); v.setBanque(this); }
    public void removeVirement(Virement v) { virements.remove(v); v.setBanque(null); }

    public void addMiseADisposition(MiseADisposition m) { misesADisposition.add(m); m.setBanque(this); }
    public void removeMiseADisposition(MiseADisposition m) { misesADisposition.remove(m); m.setBanque(null); }

    public void addContratLeasing(ContratLeasing c) { contratsLeasing.add(c); c.setBanqueDePrelevement(this); }
    public void removeContratLeasing(ContratLeasing c) { contratsLeasing.remove(c); c.setBanqueDePrelevement(null); }

    public void addCreditMoyenTerme(CreditMoyenTerme c) { creditMoyenTermes.add(c); c.setBanque(this); }
    public void removeCreditMoyenTerme(CreditMoyenTerme c) { creditMoyenTermes.remove(c); c.setBanque(null); }

    public void addTransaction(Transaction t) { transactions.add(t); t.setBanqueDePrelevement(this); }
    public void removeTransaction(Transaction t) { transactions.remove(t); t.setBanqueDePrelevement(null); }

    public List<LigneFonctionnelleSpecifique> getLigneFonctionnellesSpecifiques() {
        return ligneFonctionnellesSpecifiques;
    }

    public void setLigneFonctionnellesSpecifiques(List<LigneFonctionnelleSpecifique> ligneFonctionnellesSpecifiques) {
        this.ligneFonctionnellesSpecifiques = ligneFonctionnellesSpecifiques;
    }

    public void addLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifique l) {
        ligneFonctionnellesSpecifiques.add(l);
        l.setBanque(this);
    }

    public void removeLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifique l) {
        ligneFonctionnellesSpecifiques.remove(l);
        l.setBanque(null);
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

    @PrePersist
    protected void onCreate() {
        this.dateCreation = OffsetDateTime.now();
        this.dateModification = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }

    // --- equals / hashCode ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Banque)) return false;
        Banque b = (Banque) o;
        return Objects.equals(numeroDeCompte, b.numeroDeCompte);
    }

    @Override
    public int hashCode() { return Objects.hash(numeroDeCompte); }

    // --- toString ---
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
