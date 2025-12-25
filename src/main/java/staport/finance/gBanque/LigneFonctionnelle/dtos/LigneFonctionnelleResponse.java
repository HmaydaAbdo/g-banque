package staport.finance.gBanque.LigneFonctionnelle.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class LigneFonctionnelleResponse {

    private Long id;
    private Long banqueId;
    private String banqueNom;
    private Long natureLigneFonctionnelleId;
    private String natureLigneFonctionnelleLabel;
    private String natureLigneFonctionnelleDescription;
    private BigDecimal plafond;
    private BigDecimal encours;
    private BigDecimal solde;
    private BigDecimal taux;
    private OffsetDateTime dateDepart;
    private OffsetDateTime dateFin;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private boolean actif;
    private boolean isExpired;
    private Long ligneMereId;
    private List<SousLigneResponseDto> sousLignes;
    private Boolean estSousLigne;
    private Boolean estLigneMere;

    // --- Constructors ---
    public LigneFonctionnelleResponse() {
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
    public void setNatureLigneFonctionnelleId(Long natureLigneFonctionnelleId) {this.natureLigneFonctionnelleId = natureLigneFonctionnelleId;}
    public String getNatureLigneFonctionnelleLabel() {
        return natureLigneFonctionnelleLabel;
    }
    public void setNatureLigneFonctionnelleLabel(String natureLigneFonctionnelleLabel) {this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel;}
    public String getNatureLigneFonctionnelleDescription() {
        return natureLigneFonctionnelleDescription;
    }
    public void setNatureLigneFonctionnelleDescription(String natureLigneFonctionnelleDescription) {this.natureLigneFonctionnelleDescription = natureLigneFonctionnelleDescription;}
    public BigDecimal getPlafond() { return plafond; }
    public void setPlafond(BigDecimal plafond) { this.plafond = plafond; }
    public BigDecimal getEncours() {
        return encours;
    }
    public void setEncours(BigDecimal encours) {
        this.encours = encours;
    }
    public BigDecimal getSolde() {
        return solde;
    }
    public void setSolde(BigDecimal solde) {
        this.solde = solde;
    }
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

    public boolean isExpired() {return isExpired;}
    public void setExpired(boolean expired) {isExpired = expired;}

    public Long getLigneMereId() { return ligneMereId; }
    public void setLigneMereId(Long ligneMereId) { this.ligneMereId = ligneMereId; }

    public List<SousLigneResponseDto> getSousLignes() { return sousLignes; }
    public void setSousLignes(List<SousLigneResponseDto> sousLignes) { this.sousLignes = sousLignes; }

    public Boolean getEstSousLigne() { return estSousLigne; }
    public void setEstSousLigne(Boolean estSousLigne) { this.estSousLigne = estSousLigne; }

    public Boolean getEstLigneMere() { return estLigneMere; }
    public void setEstLigneMere(Boolean estLigneMere) { this.estLigneMere = estLigneMere; }
}