package staport.finance.gBanque.miseADisposition.dtos;

public class EncoursMiseAdispositionSearchDto {

    private String numero;
    private String beneficiaire;

    public EncoursMiseAdispositionSearchDto() {
    }

    public EncoursMiseAdispositionSearchDto(String numero, String beneficiaire) {
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
