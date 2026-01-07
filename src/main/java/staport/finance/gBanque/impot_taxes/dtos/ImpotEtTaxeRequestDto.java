package staport.finance.gBanque.impot_taxes.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class ImpotEtTaxeRequestDto {

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotBlank(message = "observation est obligatoire")
    private String observation;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotNull(message = "La banque est obligatoire")
    private Long banqueId;

    @NotNull(message = "Le motif est obligatoire")
    private Motif motif;

    @NotNull(message = "La date d'émission est obligatoire")
    private OffsetDateTime dateEmission;

    // Getters & Setters
    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public Motif getMotif() {
        return motif;
    }

    public void setMotif(Motif motif) {
        this.motif = motif;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    @Override
    public String toString() {
        return "ImpotEtTaxeRequestDto{" +
                "libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", montant=" + montant +
                ", banqueId=" + banqueId +
                ", motif=" + motif +
                ", dateEmission=" + dateEmission +
                '}';
    }
}