package staport.finance.gBanque.impayees.dtos;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ImpayeeRequestDto {

    @NotNull(message = "La banque  est obligatoire")
    private Long banqueId;

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate dateEmission ;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotNull(message = "Le type est obligatoire")
    private Type type;

    @NotNull(message = "Le motif (source de transaction) est obligatoire")
    private TransactionSource motif;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;



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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "ImpayeeRequestDto{" +
                "banqueId=" + banqueId +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                ", motif=" + motif +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                '}';
    }
}