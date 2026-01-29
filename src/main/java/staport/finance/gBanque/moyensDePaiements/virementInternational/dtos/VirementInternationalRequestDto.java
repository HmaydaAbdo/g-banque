package staport.finance.gBanque.moyensDePaiements.virementInternational.dtos;

import jakarta.validation.constraints.*;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class VirementInternationalRequestDto {

    @NotNull(message = "La banque est obligatoire")
    private Long banqueId;

    @NotNull(message = "Le bénéficiaire est obligatoire")
    private String beneficiaire;

    @NotNull(message = "L'émetteur est obligatoire")
    private String emetteur;

    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    private BigDecimal montant;

    @NotNull(message = "La date d'émission est obligatoire")
    private OffsetDateTime dateEmission;

    @NotNull(message = "Le type de virement est obligatoire")
    private Type type;

    public VirementInternationalRequestDto() {
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getBeneficiaire() {
        return beneficiaire;
    }

    public void setBeneficiaire(String beneficiaire) {
        this.beneficiaire = beneficiaire;
    }

    public String getEmetteur() {
        return emetteur;
    }

    public void setEmetteur(String emetteur) {
        this.emetteur = emetteur;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }



    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "VirementInternationalRequestDto{" +
                "banqueId=" + banqueId +
                ", beneficiaire='" + beneficiaire + '\'' +
                ", emetteur='" + emetteur + '\'' +
                ", montant=" + montant +
                ", dateEmission=" + dateEmission +
                ", type=" + type +
                '}';
    }
}