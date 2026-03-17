package staport.finance.gBanque.moyensDePaiements.virements.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;



public class VirementResponseDto {

    private Long id;
    private String numero;
    private LocalDate dateEmission ;
    private LocalDate dateOperation ;
    private LocalDate dateValeur ;
    private String libelle;
    private String observation;
    private String beneficiaire;
    private BigDecimal montant;
    private String banqueEmettriceNom;
    private Long banqueEmettriceId;
    private String banqueBeneficiaire;
    private Long banqueBeneficiaireId;
    private String emetteur;
    private String ribBeneficiaire;
    private boolean active;
    private Type type;


    public VirementResponseDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
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

    public Long getBanqueEmettriceId() {
        return banqueEmettriceId;
    }

    public void setBanqueEmettriceId(Long banqueEmettriceId) {
        this.banqueEmettriceId = banqueEmettriceId;
    }

    public Long getBanqueBeneficiaireId() {
        return banqueBeneficiaireId;
    }

    public void setBanqueBeneficiaireId(Long banqueBeneficiaireId) {
        this.banqueBeneficiaireId = banqueBeneficiaireId;
    }
}