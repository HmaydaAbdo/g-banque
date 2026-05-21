package staport.finance.gBanque.moyensDePaiements.virementInternational.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

public class VirementInternationalResponseDto {

    private Long id;
    private Long banqueId;
    private String banqueNom;
    private String beneficiaire;
    private String emetteur;
    private BigDecimal montant;
    private LocalDate dateEmission ;
    private LocalDate dateOperation ;
    private LocalDate dateValeur ;
    private String libelle;
    private String observation;
    private StatutMoyenPaiement statut;
    private Type type;

    public VirementInternationalResponseDto() {
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

    public String getBanqueNom() {
        return banqueNom;
    }

    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
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

    public StatutMoyenPaiement getStatut() { return statut; }

    public void setStatut(StatutMoyenPaiement statut) { this.statut = statut; }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirementInternationalResponseDto{" +
                "id=" + id +
                ", banqueId=" + banqueId +
                ", banqueNom='" + banqueNom + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", dateEmission=" + dateEmission +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", statut=" + statut +
                ", type=" + type +
                '}';
    }
}