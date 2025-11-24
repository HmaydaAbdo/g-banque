package staport.finance.gBanque.prefinancement.dtos;

import java.math.BigDecimal;

public class EncoursPrefinancementSearchDto {

    private String marcheName;
    private String marcheNumero;
    private BigDecimal montant;

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
        return "EncoursPrefinancementSearchDto{" +
                "marcheName='" + marcheName + '\'' +
                ", marcheNumero='" + marcheNumero + '\'' +
                ", montant=" + montant +
                '}';
    }
}
