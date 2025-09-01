package staport.finance.gBanque.miseADisposition.dtos;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

public class MiseADispositionSearchDto {

    private String numero;
    private String beneficiaire;
    private String cinBeneficiaire;
    private String banqueName;
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
}