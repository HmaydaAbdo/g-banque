package staport.finance.gBanque.moyensDePaiements.lcn.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LcnResponseDTO {

    private String numero;
    private Long id;
    private LocalDate dateEmission ;
    private String beneficiaire;
    private LocalDate dateEcheance ;
    private LocalDate dateOperation ;
    private LocalDate dateValeur ;
    private String libelle;
    private String observation;
    private BigDecimal montant;
    private boolean active;
    private String banqueName;
    private Long banqueId;
    private Type type;
    private String emetteur;
    private NatureLcn natureLcn;


    public LcnResponseDTO() {}



    // Getters & Setters
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public LocalDate getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(LocalDate dateEcheance) { this.dateEcheance = dateEcheance; }
    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getBanqueName() {
        return banqueName;
    }
    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }
    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }
    public LocalDate getDateValeur() {
        return dateValeur;
    }
    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }
    public String getLibelle() {
        return libelle;
    }
    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
    public String getObservation() {
        return observation;
    }
    public void setObservation(String observation) {
        this.observation = observation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public NatureLcn getNatureLcn() {
        return natureLcn;
    }
    public void setNatureLcn(NatureLcn natureLcn) {
        this.natureLcn = natureLcn;
    }
}