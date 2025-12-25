package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class SpecificLigneResposneDto {

    private Long id;
    private String marcheNumero;
    private String banqueNom;
    private BigDecimal montant;
    private BigDecimal encours;
    private BigDecimal solde;
    private BigDecimal taux;
    private OffsetDateTime dateDepart;
    private OffsetDateTime dateFin;
    private String natureLigneFonctionnelleLabel;
    private boolean actif;


    // --- Constructors ---
    public SpecificLigneResposneDto() {
    }

    public SpecificLigneResposneDto(Long id, String marcheNumero, String banqueNom, BigDecimal montant, BigDecimal encours, BigDecimal solde, BigDecimal taux, OffsetDateTime dateDepart, OffsetDateTime dateFin, String natureLigneFonctionnelleLabel, boolean actif) {
        this.id = id;
        this.marcheNumero = marcheNumero;
        this.banqueNom = banqueNom;
        this.montant = montant;
        this.encours = encours;
        this.solde = solde;
        this.taux = taux;
        this.dateDepart = dateDepart;
        this.dateFin = dateFin;
        this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel;
        this.actif = actif;
    }

    // --- Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBanqueNom() { return banqueNom; }
    public void setBanqueNom(String banqueNom) { this.banqueNom = banqueNom; }
    public String getNatureLigneFonctionnelleLabel() { return natureLigneFonctionnelleLabel; }
    public void setNatureLigneFonctionnelleLabel(String natureLigneFonctionnelleLabel) { this.natureLigneFonctionnelleLabel = natureLigneFonctionnelleLabel; }
    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }
    public OffsetDateTime getDateDepart() { return dateDepart; }
    public void setDateDepart(OffsetDateTime dateDepart) { this.dateDepart = dateDepart; }
    public OffsetDateTime getDateFin() { return dateFin; }
    public void setDateFin(OffsetDateTime dateFin) { this.dateFin = dateFin; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public String getMarcheNumero() { return marcheNumero; }
    public void setMarcheNumero(String marcheNumero) { this.marcheNumero = marcheNumero; }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
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

    @Override
    public String toString() {
        return "PrefinancementResponseDto{" +
                "id=" + id +
                ", marcheNumero='" + marcheNumero + '\'' +
                ", banqueNom='" + banqueNom + '\'' +
                ", montant=" + montant +
                ", encours=" + encours +
                ", solde=" + solde +
                ", taux=" + taux +
                ", dateDepart=" + dateDepart +
                ", dateFin=" + dateFin +
                ", natureLigneFonctionnelleLabel='" + natureLigneFonctionnelleLabel + '\'' +
                ", actif=" + actif +
                '}';
    }
}
