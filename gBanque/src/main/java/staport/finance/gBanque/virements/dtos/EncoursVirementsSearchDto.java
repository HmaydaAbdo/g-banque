package staport.finance.gBanque.virements.dtos;

import java.math.BigDecimal;

public class EncoursVirementsSearchDto {

    private String numero;
    private String beneficiaire;
    private BigDecimal montant;

    public EncoursVirementsSearchDto() {
    }

    public EncoursVirementsSearchDto(String numero, String beneficiaire) {
        this.numero = numero;
        this.beneficiaire = beneficiaire;
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

    @Override
    public String toString() {
        return "EncoursVirementsSearchDto{" +
                "numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                '}';
    }
}
