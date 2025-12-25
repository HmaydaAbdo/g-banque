package staport.finance.gBanque.factoring.dtos;

import java.math.BigDecimal;

public class EncoursFactoringSearchDto {

    private String marcheName;
    private String marcheNumero;
    private BigDecimal montant;

    public EncoursFactoringSearchDto() {
    }

    public String getMarcheName() {
        return marcheName;
    }

    public void setMarcheName(String marcheName) {
        this.marcheName = marcheName;
    }

    public String getMarcheNumero() {
        return marcheNumero;
    }

    public void setMarcheNumero(String marcheNumero) {
        this.marcheNumero = marcheNumero;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "EncoursFactoringSearchDto{" +
                "marcheName='" + marcheName + '\'' +
                ", marcheNumero='" + marcheNumero + '\'' +
                ", montant=" + montant +
                '}';
    }
}
