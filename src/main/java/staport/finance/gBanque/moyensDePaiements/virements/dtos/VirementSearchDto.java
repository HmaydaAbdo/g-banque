package staport.finance.gBanque.moyensDePaiements.virements.dtos;

import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VirementSearchDto {

    private String numero;
    private LocalDate dateEmission ;
    private BigDecimal montant;
    private String beneficiaire;
    private String banqueBeneficiaire;
    private String nomBanqueEmettrice;
    private String ribBeneficiaire;
    private Boolean active;
    private String emetteur;
    private Type type;

    public VirementSearchDto() {
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

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "VirementSearchDto{" +
                "numero='" + numero + '\'' +
                ", dateEmisson=" + dateEmission +
                ", montant=" + montant +
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