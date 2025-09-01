package staport.finance.gBanque.miseADisposition.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class MiseADispositionResponseDto {

    private String numero;
    private OffsetDateTime dateEmission;
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

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
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



}