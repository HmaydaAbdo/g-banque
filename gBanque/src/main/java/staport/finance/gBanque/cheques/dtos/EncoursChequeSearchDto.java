package staport.finance.gBanque.cheques.dtos;

public class EncoursChequeSearchDto {

    private String numero;
    private String beneficiaire;

    public EncoursChequeSearchDto() {
    }

    public EncoursChequeSearchDto(String numero, String beneficiaire) {
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
