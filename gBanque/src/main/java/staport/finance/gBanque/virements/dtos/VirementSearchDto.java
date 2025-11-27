package staport.finance.gBanque.virements.dtos;

import staport.finance.gBanque.virements.enums.Type;

public class VirementSearchDto {

    private String numero;
    private String beneficiaire;
    private String banqueBeneficiaire;
    private String nomBanqueEmettrice;
    private String ribBeneficiaire;
    private Boolean active;
    private String emetteur;
    private Type type;

    public VirementSearchDto() {
    }

    public VirementSearchDto(String numero, Boolean active, String ribBeneficiaire, String nomBanqueEmettrice, String banqueBeneficiaire, String beneficiaire) {
        this.numero = numero;
        this.active = active;
        this.ribBeneficiaire = ribBeneficiaire;
        this.nomBanqueEmettrice = nomBanqueEmettrice;
        this.banqueBeneficiaire = banqueBeneficiaire;
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

    public String getBanqueBeneficiaire() {
        return banqueBeneficiaire;
    }

    public void setBanqueBeneficiaire(String banqueBeneficiaire) {
        this.banqueBeneficiaire = banqueBeneficiaire;
    }

    public String getNomBanqueEmettrice() {
        return nomBanqueEmettrice;
    }

    public void setNomBanqueEmettrice(String nomBanqueEmettrice) {
        this.nomBanqueEmettrice = nomBanqueEmettrice;
    }

    public String getRibBeneficiaire() {
        return ribBeneficiaire;
    }

    public void setRibBeneficiaire(String ribBeneficiaire) {
        this.ribBeneficiaire = ribBeneficiaire;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Type getType() {return type;}
    public void setType(Type type) { this.type = type;}

    public String getEmetteur() {return emetteur;}
    public void setEmetteur(String emetteur) {this.emetteur = emetteur;}

    @Override
    public String toString() {
        return "VirementSearchDto{" +
                "numero='" + numero + '\'' +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", banqueBeneficiaire='" + banqueBeneficiaire + '\'' +
                ", nomBanqueEmettrice='" + nomBanqueEmettrice + '\'' +
                ", ribBeneficiaire='" + ribBeneficiaire + '\'' +
                ", active=" + active +
                ", emetteur='" + emetteur + '\'' +
                ", type=" + type +
                '}';
    }
}