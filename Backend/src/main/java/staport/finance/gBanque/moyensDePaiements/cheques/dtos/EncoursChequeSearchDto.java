package staport.finance.gBanque.moyensDePaiements.cheques.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public class EncoursChequeSearchDto {

    private String numero;
    private LocalDate dateEmission ;
    private String emetteur;
    private String beneficiaire;
    private BigDecimal montant;

    public EncoursChequeSearchDto() {
    }

    public String getNumero() {
        return numero;
    }
    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
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

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    @Override
    public String toString() {
        return "EncoursChequeSearchDto{" +
                "emetteur=" + emetteur +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", numero='" + numero + '\'' +
                ", dateEmission=" + dateEmission +
                '}';
    }
}
