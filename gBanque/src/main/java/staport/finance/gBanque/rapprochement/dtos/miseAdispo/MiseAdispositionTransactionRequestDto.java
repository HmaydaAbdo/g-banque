package staport.finance.gBanque.rapprochement.dtos.miseAdispo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.OffsetDateTime;
import java.util.Objects;

public class MiseAdispositionTransactionRequestDto {

    @NotBlank(message = "Le numéro de mise à disposition est obligatoire")
    private String numeroMiseAdisposition;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.MiseADisposition;

    @NotNull(message = "La date d’émission est obligatoire")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "La date d’opération est obligatoire")
    private OffsetDateTime dateOperation;

    @NotBlank(message = "L’observation est obligatoire")
    private String observation;

    @NotNull(message = "La TVA déductible est obligatoire")
    private Double tvaDeductible;

    //  Getters & Setters
    public String getNumeroMiseAdisposition() {
        return numeroMiseAdisposition;
    }

    public void setNumeroMiseAdisposition(String numeroMiseAdisposition) {
        this.numeroMiseAdisposition = numeroMiseAdisposition;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public Double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(Double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    //  equals & hashCode (generated with all fields)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MiseAdispositionTransactionRequestDto that)) return false;
        return Objects.equals(numeroMiseAdisposition, that.numeroMiseAdisposition) &&
                transactionSource == that.transactionSource &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(observation, that.observation) &&
                Objects.equals(tvaDeductible, that.tvaDeductible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroMiseAdisposition, transactionSource, dateEmission, dateValeur, dateOperation, observation, tvaDeductible);
    }

    //  toString (for logging/debugging)
    @Override
    public String toString() {
        return "MiseAdispositionTransactionRequestDto{" +
                "numeroMiseAdisposition='" + numeroMiseAdisposition + '\'' +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }
}
