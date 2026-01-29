package staport.finance.gBanque.moyensDePaiements.virementInternational.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class VirementInternationalSearchDto {

    private Long banqueId;
    private String beneficiaire;
    private String emetteur;
    private BigDecimal montant;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateOperation;
    private OffsetDateTime dateValeur;
    private String libelle;
    private String observation;
    private Boolean active;
    private Type type;

    public VirementInternationalSearchDto() {
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirementInternationalSearchDto{" +
                "banqueId=" + banqueId +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", dateEmission=" + dateEmission +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", active=" + active +
                ", type=" + type +
                '}';
    }
}