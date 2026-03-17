package staport.finance.gBanque.impayees.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ImpayeeRequestDto {

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotNull(message = "La banque d'encaissement est obligatoire")
    private Long banqueEncaissementId;

    @NotNull(message = "Le motif (source de transaction) est obligatoire")
    private TransactionSource motif;

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate dateEmission ;


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

    public Long getBanqueEncaissementId() {
        return banqueEncaissementId;
    }

    public void setBanqueEncaissementId(Long banqueEncaissementId) {
        this.banqueEncaissementId = banqueEncaissementId;
    }

    public TransactionSource getMotif() {
        return motif;
    }

    public void setMotif(TransactionSource motif) {
        this.motif = motif;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }


    @Override
    public String toString() {
        return "ImpayeeRequestDto{" +
                "libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", montant=" + montant +
                ", banqueEncaissementId=" + banqueEncaissementId +
                ", motif=" + motif +
                ", dateEmission=" + dateEmission +
                '}';
    }
}