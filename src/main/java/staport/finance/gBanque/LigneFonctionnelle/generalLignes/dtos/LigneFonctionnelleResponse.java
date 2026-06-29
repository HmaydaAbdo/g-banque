package staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class LigneFonctionnelleResponse {

    private Long id;
    private Long banqueId;
    private String banqueNom;
    private Long natureLigneFonctionnelleId;
    private String natureLigneFonctionnelleLabel;
    private String natureLigneFonctionnelleDescription;

    private List<MarcheRefDto> marches = new ArrayList<>();

    private BigDecimal plafond;
    private BigDecimal encours;
    private BigDecimal solde;
    private BigDecimal taux;
    private LocalDate dateDepart ;
    private LocalDate dateFin ;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;
    private boolean actif;
    private boolean isExpired;

    // Hiérarchie
    private Long ligneMereId;
    private List<SousLigneResponseDto> sousLignes;
    private Boolean estSousLigne;
    private Boolean estLigneMere;

    // --- Constructors ---
    public LigneFonctionnelleResponse() {
    }

    // --- Getters and Setters ---
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

    public List<MarcheRefDto> getMarches() {
        return marches;
    }

    public void setMarches(List<MarcheRefDto> marches) {
        this.marches = marches;
    }

    public BigDecimal getPlafond() {
        return plafond;
    }

    public void setPlafond(BigDecimal plafond) {
        this.plafond = plafond;
    }

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

    public BigDecimal getTaux() {
        return taux;
    }

    public void setTaux(BigDecimal taux) {
        this.taux = taux;
    }

    public LocalDate getDateDepart() {
        return dateDepart;
    }

    public void setDateDepart(LocalDate dateDepart) {
        this.dateDepart = dateDepart;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }

    public boolean isActif() {
        return actif;
    }

    public void setActif(boolean actif) {
        this.actif = actif;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }

    public Long getLigneMereId() {
        return ligneMereId;
    }

    public void setLigneMereId(Long ligneMereId) {
        this.ligneMereId = ligneMereId;
    }

    public List<SousLigneResponseDto> getSousLignes() {
        return sousLignes;
    }

    public void setSousLignes(List<SousLigneResponseDto> sousLignes) {
        this.sousLignes = sousLignes;
    }

    public Boolean getEstSousLigne() {
        return estSousLigne;
    }

    public void setEstSousLigne(Boolean estSousLigne) {
        this.estSousLigne = estSousLigne;
    }

    public Boolean getEstLigneMere() {
        return estLigneMere;
    }

    public void setEstLigneMere(Boolean estLigneMere) {
        this.estLigneMere = estLigneMere;
    }

    @Override
    public String toString() {
        return "LigneFonctionnelleResponse{" +
                "id=" + id +
                ", banqueId=" + banqueId +
                ", banqueNom='" + banqueNom + '\'' +
                ", natureLigneFonctionnelleLabel='" + natureLigneFonctionnelleLabel + '\'' +
                ", marches=" + marches +
                ", plafond=" + plafond +
                ", encours=" + encours +
                ", solde=" + solde +
                ", taux=" + taux +
                ", actif=" + actif +
                ", isExpired=" + isExpired +
                ", estLigneMere=" + estLigneMere +
                ", estSousLigne=" + estSousLigne +
                '}';
    }
}