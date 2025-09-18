package staport.finance.gBanque.virements.dtos;

public class EncoursVirementsSearchDto {

    private String numero;
    private String beneficiaire;

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
}
