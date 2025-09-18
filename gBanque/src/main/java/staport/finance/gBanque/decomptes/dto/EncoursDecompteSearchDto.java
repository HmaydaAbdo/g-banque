package staport.finance.gBanque.decomptes.dto;

public class EncoursDecompteSearchDto {

    private String marcheNumero;
    private String marcheName ;

    public EncoursDecompteSearchDto() {
    }

    public EncoursDecompteSearchDto(String marcheNumero, String marcheName) {
        this.marcheNumero = marcheNumero;
        this.marcheName = marcheName;
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
}
