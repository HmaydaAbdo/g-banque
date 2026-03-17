package staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos;

import java.math.BigDecimal;

public class MiseADispositionSearchDto {

    private String numero;
    private String beneficiaire;
    private String cinBeneficiaire;
    private String banqueName;
    private String emetteur;
    private Boolean active;

    // Getters and Setters
    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public String getCinBeneficiaire() {
        return cinBeneficiaire;
    }

    public void setCinBeneficiaire(String cinBeneficiaire) {
        this.cinBeneficiaire = cinBeneficiaire;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public String getBanqueName() {
        return banqueName;
    }

    public void setBanqueName(String banqueName) {
        this.banqueName = banqueName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "MiseADispositionSearchDto{" +
                "numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", cinBeneficiaire='" + cinBeneficiaire + '\'' +
                ", banqueName='" + banqueName + '\'' +
                ", emetteur='" + emetteur + '\'' +
                ", active=" + active +
                '}';
    }
}