package staport.finance.gBanque.moyensDePaiements.virements.dtos;

import jakarta.validation.constraints.*;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;

public class VirementRequestDto {

    @NotNull(message = "La banque bénéficiaire est obligatoire")
    private Long banqueBeneficiaireId;

    @NotNull(message = "La date de réception est obligatoire")
    private LocalDate dateReception ;

    @NotBlank(message = "Le bénéficiaire est obligatoire")
    private final String beneficiaire = "STAPORT";

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotBlank(message = "L'émetteur est obligatoire")
    private String emetteur;

    @NotNull(message = "Le type de virement est obligatoire")
    private final Type type = Type.ENCAISSEMENT;

    public VirementRequestDto() {
    }


    public Long getBanqueBeneficiaireId() {
        return banqueBeneficiaireId;
    }

    public void setBanqueBeneficiaireId(Long banqueBeneficiaireId) {
        this.banqueBeneficiaireId = banqueBeneficiaireId;
    }

    public LocalDate getDateReception() {
        return dateReception;
    }

    public void setDateReception(LocalDate dateReception) {
        this.dateReception = dateReception;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return "VirementRequestDto{" +
                "banqueBeneficiaireId=" + banqueBeneficiaireId +
                ", dateReception=" + dateReception +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", montant=" + montant +
                ", emetteur='" + emetteur + '\'' +
                ", type=" + type +
                '}';
    }
}
