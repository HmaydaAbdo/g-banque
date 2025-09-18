package staport.finance.gBanque.lcn.dtos;

public class EncoursLcnSearchDto {

    private String numero;
    private String beneficiaire;

    public EncoursLcnSearchDto() {
    }

    public EncoursLcnSearchDto(String numero, String beneficiaire) {
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
}
