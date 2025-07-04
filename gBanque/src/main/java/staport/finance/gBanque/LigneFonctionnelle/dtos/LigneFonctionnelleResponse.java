package staport.finance.gBanque.LigneFonctionnelle.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LigneFonctionnelleResponse {

    private Long id;
    private Long banqueId;
    private String banqueNom;
    private Long natureLigneFonctionnelleId;
    private String natureLigneFonctionnelleLabel;
    private String natureLigneFonctionnelleDescription;
    private BigDecimal plafond;
    private BigDecimal taux;
    private OffsetDateTime dateDepart;
    private OffsetDateTime dateFin;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private boolean actif; // NEW FIELD

    // --- Constructors ---
    public LigneFonctionnelleResponse() {
    }


    public LigneFonctionnelleResponse(Long id, Long banqueId, String banqueNom, Long natureLigneFonctionnelleId, String natureLigneFonctionnelleLabel, String natureLigneFonctionnelleDescription, BigDecimal plafond, BigDecimal taux, OffsetDateTime dateDepart, OffsetDateTime dateFin, OffsetDateTime dateCreation, OffsetDateTime dateModification, boolean actif) {
        this.id = id;
        this.banqueId = banqueId;
        this.banqueNom = banqueNom;
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
        this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel;
        this.natureLigneFonctionnelleDescription = natureLigneFonctionnelleDescription;
        this.plafond = plafond;
        this.taux = taux;
        this.dateDepart = dateDepart;
        this.dateFin = dateFin;
        this.dateCreation = dateCreation;
        this.dateModification = dateModification;
        this.actif = actif;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBanqueId() { return banqueId; }
    public void setBanqueId(Long banqueId) { this.banqueId = banqueId; }
    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }
    public Long getNatureLigneFonctionnelleId() {
        return natureLigneFonctionnelleId;
    }
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) {
        this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;
    }
    public String getNatureLigneFonctionnelleLabel() {
        return natureLigneFonctionnelleLabel;
    }
    public void setNatureLigneFonctionnelleLabel(String natureLigneFonctionnelleLabel) {
        this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel;
    }
    public String getNatureLigneFonctionnelleDescription() {
        return natureLigneFonctionnelleDescription;
    }
    public void setNatureLigneFonctionnelleDescription(String natureLigneFonctionnelleDescription) {
        this.natureLigneFonctionnelleDescription = natureLigneFonctionnelleDescription;
    }
    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public OffsetDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(OffsetDateTime dateCreation) { this.dateCreation = dateCreation; }
    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }
    public OffsetDateTime getDateDepart() {return dateDepart;}
    public void setDateDepart(OffsetDateTime dateDepart) {this.dateDepart = dateDepart;}
    public OffsetDateTime getDateFin() {return dateFin;}
    public void setDateFin(OffsetDateTime dateFin) {this.dateFin = dateFin;}

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }
}