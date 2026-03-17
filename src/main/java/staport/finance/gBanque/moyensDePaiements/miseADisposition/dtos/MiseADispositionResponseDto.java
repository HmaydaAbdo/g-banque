package staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

public class MiseADispositionResponseDto {

    private Long id;
    private Long banqueId;
    private String emetteur;
    private Type type;
    private String numero;
    private LocalDate dateEmission ;
    private LocalDate dateOperation ;
    private LocalDate dateValeur ;
    private String libelle;
    private String observation;
    private String beneficiaire;
    private BigDecimal montant;
    private String banqueName;
    private String cinBeneficiaire;
    private boolean active;


    // Getters and Setters
    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }


    public String getCinBeneficiaire() {
        return cinBeneficiaire;
    }

    public void setCinBeneficiaire(String cinBeneficiaire) {
        this.cinBeneficiaire = cinBeneficiaire;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

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

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}