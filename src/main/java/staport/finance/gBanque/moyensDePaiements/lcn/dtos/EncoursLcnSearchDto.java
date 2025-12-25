package staport.finance.gBanque.moyensDePaiements.lcn.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class EncoursLcnSearchDto {

    private String numero;
    private String beneficiaire;
    private BigDecimal montant;
    private OffsetDateTime dateEcheance;
    private String emetteur;
    private OffsetDateTime dateEmission;

    public EncoursLcnSearchDto() {
    }


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

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public OffsetDateTime getDateEcheance() {
        return dateEcheance;
    }

    public void setDateEcheance(OffsetDateTime dateEcheance) {
        this.dateEcheance = dateEcheance;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    @Override
    public String toString() {
        return "EncoursLcnSearchDto{" +
                "numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", dateEcheance=" + dateEcheance +
                ", emetteur='" + emetteur + '\'' +
                ", dateEmission=" + dateEmission +
                '}';
    }
}
