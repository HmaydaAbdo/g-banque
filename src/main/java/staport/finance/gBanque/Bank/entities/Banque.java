package staport.finance.gBanque.Bank.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Bank.enums.Devise;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.restitution.entities.Restitution;
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

    @Column(name = "solde_depart", nullable = false)
    private BigDecimal soldeDepart = BigDecimal.ZERO;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // --- Relations ---
    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<LigneFonctionnelle> ligneFonctionnelles = new ArrayList<>();

    @OneToMany(mappedBy = "banqueDePrelevement", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ContratLeasing> contratsLeasing = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<CreditMoyenTerme> creditMoyenTermes = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Cheque> cheques = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Lcn> lcns = new ArrayList<>();

    @OneToMany(mappedBy = "banqueEmettrice", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Virement> virementsEmis = new ArrayList<>();

    @OneToMany(mappedBy = "banqueBeneficiare", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Virement> virementsRecus = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<VirementInternational> virementInternationals = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<MiseADisposition> misesADisposition = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Marche> marches = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Commission> commissions = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<FraisBancaire> fraisBancaires = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Agios> agios = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Prelevement> prelevements = new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<ImpotEtTaxe> impotEtTaxes=new ArrayList<>();

    @OneToMany(mappedBy = "banque",cascade = {CascadeType.ALL},fetch = FetchType.LAZY,orphanRemoval = true)
    private List<Restitution> restitutions=new ArrayList<>();

    @OneToMany(mappedBy = "banqueDePrelevement",cascade = {CascadeType.ALL},fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Rapprochement> rapprochements = new ArrayList<>();

    @OneToMany(mappedBy = "banqueEncaissement", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Decompte> decomptes = new ArrayList<>();

    @OneToMany(mappedBy = "banque", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Impayee> impayees = new ArrayList<>();

    // --- Constructors ---
    public Banque() {}

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

    public BigDecimal getSoldeDepart() {
        return soldeDepart;
    }
    public void setSoldeDepart(BigDecimal soldeDepart) {
        this.soldeDepart = soldeDepart;
    }

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

    public List<Virement> getVirementsEmis() { return virementsEmis; }
    public void setVirementsEmis(List<Virement> virementsEmis) { this.virementsEmis = virementsEmis; }

    public List<Virement> getVirementsRecus() { return virementsRecus; }
    public void setVirementsRecus(List<Virement> virementsRecus) { this.virementsRecus = virementsRecus; }

    public List<VirementInternational> getVirementInternationals() {
        return virementInternationals;
    }

    public void setVirementInternationals(List<VirementInternational> virementInternationals) {
        this.virementInternationals = virementInternationals;
    }

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

    public void addVirementEmis(Virement v) { virementsEmis.add(v); v.setBanqueEmettrice(this); }
    public void removeVirementEmis(Virement v) { virementsEmis.remove(v); v.setBanqueEmettrice(null); }

    public void addVirementRecu(Virement v) { virementsRecus.add(v); v.setBanqueBeneficiare(this); }
    public void removeVirementRecu(Virement v) { virementsRecus.remove(v); v.setBanqueBeneficiare(null); }

    public void addMiseADisposition(MiseADisposition m) { misesADisposition.add(m); m.setBanque(this); }
    public void removeMiseADisposition(MiseADisposition m) { misesADisposition.remove(m); m.setBanque(null); }

    public void addContratLeasing(ContratLeasing c) { contratsLeasing.add(c); c.setBanqueDePrelevement(this); }
    public void removeContratLeasing(ContratLeasing c) { contratsLeasing.remove(c); c.setBanqueDePrelevement(null); }

    public void addCreditMoyenTerme(CreditMoyenTerme c) { creditMoyenTermes.add(c); c.setBanque(this); }
    public void removeCreditMoyenTerme(CreditMoyenTerme c) { creditMoyenTermes.remove(c); c.setBanque(null); }

    public List<Marche> getMarches() {
        return marches;
    }
    public void setMarches(List<Marche> marches) {
        this.marches = marches;
    }


    public List<Decompte> getDecomptes() {
        return decomptes;
    }

    public void setDecomptes(List<Decompte> decomptes) {
        this.decomptes = decomptes;
    }

    // Helper methods
    public void addDecompte(Decompte decompte) {
        decomptes.add(decompte);
        decompte.setBanqueEncaissement(this);
    }

    public void removeDecompte(Decompte decompte) {
        decomptes.remove(decompte);
        decompte.setBanqueEncaissement(null);
    }

    public void addMarche(Marche marche) {
        marches.add(marche);
        marche.setBanque(this);
    }
    public void removeMarche(Marche marche) {
        marches.remove(marche);
        marche.setBanque(null);
    }

    public void addTransaction(Transaction t) { transactions.add(t); t.setBanqueDePrelevement(this); }
    public void removeTransaction(Transaction t) { transactions.remove(t); t.setBanqueDePrelevement(null); }

    public List<Commission> getCommissions() {
        return commissions;
    }
    public void setCommissions(List<Commission> commissions) {
        this.commissions = commissions;
    }

    public List<Restitution> getRestitutions(){return  restitutions;}
    public void setRestitutions(List<Restitution> restitutions){ this.restitutions=restitutions;}

    public void addRestitution(Restitution restitution){
        this.restitutions.add(restitution);
    }
    public void removeRestitution(Restitution restitution){
        this.restitutions.remove(restitution);
    }

    public void addCommission(Commission commission){
        this.commissions.add(commission);
    }
    public void removeCommission(Commission commission){
        this.commissions.remove(commission);
    }

    public List<FraisBancaire> getFraisBancaires() {
        return fraisBancaires;
    }
    public void setFraisBancaires(List<FraisBancaire> fraisBancaires) {
        this.fraisBancaires = fraisBancaires;
    }

    public void addFraisBancaire(FraisBancaire fraisBancaire){
        this.fraisBancaires.add(fraisBancaire);
    }
    public void removeFraisBancaire(FraisBancaire fraisBancaire){
        this.fraisBancaires.remove(fraisBancaire);
    }

    public List<Agios> getAgios() {
        return agios;
    }
    public void setAgios(List<Agios> agios) {
        this.agios = agios;
    }

    public void addAgios(Agios agios){
        this.agios.add(agios);
    }
    public void removeAgios(Agios agios){
        this.agios.remove(agios);
    }

    public void addImpayee(Impayee i) {
        impayees.add(i);
        i.setBanque(this);
    }
    public void removeImpayee(Impayee i) {
        impayees.remove(i);
        i.setBanque(null);
    }

    public List<Impayee> getImpayees() {
        return impayees;
    }
    public void setImpayees(List<Impayee> impayees) {
        this.impayees = impayees;
    }

    public List<Prelevement> getPrelevements() {
        return prelevements;
    }
    public void setPrelevements(List<Prelevement> prelevements) {
        this.prelevements = prelevements;
    }

    public void addPrelevement(Prelevement prelevement){
        this.prelevements.add(prelevement);
    }
    public void removePrelevement(Prelevement prelevement){
        this.prelevements.remove(prelevement);
    }

    public List<ImpotEtTaxe> getImpotEtTaxes(){ return this.impotEtTaxes;}
    public void setImpotEtTaxes(List<ImpotEtTaxe> impotEtTaxes){this.impotEtTaxes=impotEtTaxes;}

    public void addImpotEtTaxe(ImpotEtTaxe impotEtTaxe){this.impotEtTaxes.add(impotEtTaxe);}
    public void removeImpotEtTaxe(ImpotEtTaxe impotEtTaxe){this.impotEtTaxes.remove(impotEtTaxe);}

    public List<Rapprochement> getRapprochements() {
        return rapprochements;
    }
    public void setRapprochements(List<Rapprochement> rapprochements) {
        this.rapprochements = rapprochements;
    }

    // --- Helper methods for bidirectional relationships (Rapprochement) ---
    public void addRapprochement(Rapprochement rapprochement) {
        rapprochements.add(rapprochement);
        rapprochement.setBanque(this);
    }
    public void removeRapprochement(Rapprochement rapprochement) {
        rapprochements.remove(rapprochement);
        rapprochement.setBanque(null);
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
                ", address='" + address + '\'' +
                ", agence='" + agence + '\'' +
                ", soldeActuel=" + soldeActuel +
                ", soldeDepart=" + soldeDepart +
                '}';
    }
}
