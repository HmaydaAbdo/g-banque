package staport.finance.gBanque.decomptes.dto;

import java.math.BigDecimal;

public class EncoursDecompteSearchDto {

    private String marcheNumero;
    private String marcheName ;
    private BigDecimal montant;

    public EncoursDecompteSearchDto() {
    }


    public String getMarcheNumero() {
        return marcheNumero;
    }

    public void setMarcheNumero(String marcheNumero) {
        this.marcheNumero = marcheNumero;
    }

    public String getMarcheName() {
        return marcheName;
    }

    public void setMarcheName(String marcheName) {
        this.marcheName = marcheName;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "EncoursDecompteSearchDto{" +
                "marcheNumero='" + marcheNumero + '\'' +
                ", marcheName='" + marcheName + '\'' +
                ", montant=" + montant +
                '}';
    }
}
