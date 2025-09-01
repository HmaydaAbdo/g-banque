package staport.finance.gBanque.lcn.dtos;


public class LcnSearchDTO {

    private String numero;
    private String beneficiaire;
    private String nomBanque;
    private Boolean active;

    public LcnSearchDTO() {
    }

    public LcnSearchDTO(String numero, String beneficiaire, String nomBanque, Boolean active) {
        this.numero = numero;
        this.beneficiaire = beneficiaire;
        this.nomBanque = nomBanque;
        this.active = active;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getNomBanque() {
        return nomBanque;
    }

    public void setNomBanque(String nomBanque) {
        this.nomBanque = nomBanque;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    @Override
    public String toString() {
        return "LcnSearchDTO{" +
                "numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", nomBanque='" + nomBanque + '\'' +
                ", active=" + active +
                '}';
    }
}