package staport.finance.gBanque.lcn.dtos;

import staport.finance.gBanque.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LcnResponseDTO {

    private String numero;
    private Long id;
    private OffsetDateTime dateEmission;
    private String beneficiaire;
    private OffsetDateTime dateEcheance;
    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;
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
    public OffsetDateTime getDateEmission() { return dateEmission; }
    public void setDateEmission(OffsetDateTime dateEmission) { this.dateEmission = dateEmission; }
    public String getBeneficiaire() { return beneficiaire; }
    public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }
    public OffsetDateTime getDateEcheance() { return dateEcheance; }
    public void setDateEcheance(OffsetDateTime dateEcheance) { this.dateEcheance = dateEcheance; }
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

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }
    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }
    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }
    public void setDateValeur(OffsetDateTime dateValeur) {
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