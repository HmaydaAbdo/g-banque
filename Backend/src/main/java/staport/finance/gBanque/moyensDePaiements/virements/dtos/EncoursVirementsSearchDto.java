package staport.finance.gBanque.moyensDePaiements.virements.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EncoursVirementsSearchDto {

    private LocalDate dateEmission ;
    private String beneficiaire;
    private BigDecimal montant;
    private String emetteur;

    public EncoursVirementsSearchDto() {
    }


    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
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

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    @Override
    public String toString() {
        return "EncoursVirementsSearchDto{" +
                "dateEmission=" + dateEmission +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", emetteur='" + emetteur + '\'' +
                '}';
    }
}
