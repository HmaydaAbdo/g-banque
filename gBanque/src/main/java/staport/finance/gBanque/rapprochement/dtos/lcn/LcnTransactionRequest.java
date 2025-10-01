package staport.finance.gBanque.rapprochement.dtos.lcn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.OffsetDateTime;
import java.util.Objects;

public class LcnTransactionRequest {

    @NotBlank(message = "Le numéro LCN est obligatoire")
    private String numeroLcn;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.LCN;

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

    @NotNull(message = "libelle  est obligatoire")
    private String libelle;

    // Default constructor
    public LcnTransactionRequest() {
    }

    // All-args constructor (except transactionSource since it's final)
    public LcnTransactionRequest(String numeroLcn,
                                 OffsetDateTime dateEmission,
                                 OffsetDateTime dateValeur,
                                 OffsetDateTime dateOperation,
                                 String observation,
                                 Double tvaDeductible) {
        this.numeroLcn = numeroLcn;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperation = dateOperation;
        this.observation = observation;
        this.tvaDeductible = tvaDeductible;
    }

    // Getters and Setters
    public String getNumeroLcn() {
        return numeroLcn;
    }

    public void setNumeroLcn(String numeroLcn) {
        this.numeroLcn = numeroLcn;
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

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    // equals & hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LcnTransactionRequest that)) return false;
        return Objects.equals(numeroLcn, that.numeroLcn) &&
                transactionSource == that.transactionSource &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(observation, that.observation) &&
                Objects.equals(tvaDeductible, that.tvaDeductible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroLcn, transactionSource, dateEmission, dateValeur, dateOperation, observation, tvaDeductible);
    }

    // toString
    @Override
    public String toString() {
        return "LcnTransactionRequest{" +
                "numeroLcn='" + numeroLcn + '\'' +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }
}
