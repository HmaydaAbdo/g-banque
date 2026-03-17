package staport.finance.gBanque.commision.dtos;

import jakarta.validation.constraints.NotEmpty;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public class CommissionRequestDto {

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotEmpty(message = "L'observation est obligatoire")
    private String observation;

    @NotNull(message = "Le montant est obligatoire")
    @Positive(message = "Le montant doit être positif")
    private BigDecimal montant;

    @NotNull(message = "La banque est obligatoire")
    private Long banqueId;

    @NotNull(message = "L’opération est obligatoire")
    private TransactionSource operation;

    @NotNull(message = "La date d’émission est obligatoire")
    private LocalDate dateEmission ;

    // Getters & Setters
    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
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

    public TransactionSource getOperation() {
        return operation;
    }

    public void setOperation(TransactionSource operation) {
        this.operation = operation;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    @Override
    public String toString() {
        return "CommissionRequestDto{" +
                "libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", montant=" + montant +
                ", banqueId=" + banqueId +
                ", operation=" + operation +
                ", dateEmission=" + dateEmission +
                '}';
    }
}
