package staport.finance.gBanque.virements.dtos;

import staport.finance.gBanque.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;



public class VirementResponseDto {

    private String numero;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;
    private String libelle;
    private String observation;
    private String beneficiaire;
    private BigDecimal montant;
    private String banqueEmettriceNom;
    private String banqueBeneficiaire;
    private String emetteur;
    private String ribBeneficiaire;
    private boolean active;
    private Type type;


    public VirementResponseDto() {
    }



    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Type getType() {return type;}
    public void setType(Type type) { this.type = type;}

    public String getBanqueBeneficiaire() {
        return banqueBeneficiaire;
    }

    public void setBanqueBeneficiaire(String banqueBeneficiaire) {
        this.banqueBeneficiaire = banqueBeneficiaire;
    }

    public String getRibBeneficiaire() {
        return ribBeneficiaire;
    }

    public void setRibBeneficiaire(String ribBeneficiaire) {
        this.ribBeneficiaire = ribBeneficiaire;
    }

    public String getBanqueEmettriceNom() {
        return banqueEmettriceNom;
    }

    public void setBanqueEmettriceNom(String banqueEmettriceNom) {
        this.banqueEmettriceNom = banqueEmettriceNom;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
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


}