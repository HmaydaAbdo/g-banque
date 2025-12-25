package staport.finance.gBanque.moyensDePaiements.lcn.dtos;


import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LcnSearchDTO {

    private OffsetDateTime dateEmission;
    private OffsetDateTime dateEcheance;
    private String emetteur;
    private BigDecimal montant;
    private String numero;
    private String beneficiaire;
    private String nomBanque;
    private Boolean active;

    public LcnSearchDTO() {
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    @Override
    public String toString() {
        return "LcnSearchDTO{" +
                "dateEmission=" + dateEmission +
                ", dateEcheance=" + dateEcheance +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", nomBanque='" + nomBanque + '\'' +
                ", active=" + active +
                '}';
    }
}