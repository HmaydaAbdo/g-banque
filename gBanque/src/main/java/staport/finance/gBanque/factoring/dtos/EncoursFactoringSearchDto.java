package staport.finance.gBanque.factoring.dtos;

public class EncoursFactoringSearchDto {

    private String marcheName;
    private String marcheNumero;

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
}
