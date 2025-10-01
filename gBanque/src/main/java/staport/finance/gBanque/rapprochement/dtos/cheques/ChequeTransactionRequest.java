package staport.finance.gBanque.rapprochement.dtos.cheques;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.OffsetDateTime;
import java.util.Objects;

public class ChequeTransactionRequest {

    @NotBlank(message = "Le numéro du chèque est obligatoire")
    private String numeroCheque;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.CHEQUE;

    @NotNull(message = "La date d’émission est obligatoire")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "La date d’opération est obligatoire")
    private OffsetDateTime dateOperation;

    @NotBlank(message = "L’observation est obligatoire")
    private String observation;

    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    @NotNull(message = "La TVA déductible est obligatoire")
    private Double tvaDeductible;

    // Default constructor
    public ChequeTransactionRequest() {
    }

    // All-args constructor (except transactionSource since it's final)
    public ChequeTransactionRequest(String numeroCheque,
                                    OffsetDateTime dateEmission,
                                    OffsetDateTime dateValeur,
                                    OffsetDateTime dateOperation,
                                    String observation,
                                    Double tvaDeductible) {
        this.numeroCheque = numeroCheque;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperation = dateOperation;
        this.observation = observation;
        this.tvaDeductible = tvaDeductible;
    }

    // Getters and Setters
    public String getNumeroCheque() {
        return numeroCheque;
    }

    public void setNumeroCheque(String numeroCheque) {
        this.numeroCheque = numeroCheque;
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

    // equals & hashCode (based on numeroCheque, transactionSource, and dates)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChequeTransactionRequest that)) return false;
        return Objects.equals(numeroCheque, that.numeroCheque) &&
                transactionSource == that.transactionSource &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(observation, that.observation) &&
                Objects.equals(tvaDeductible, that.tvaDeductible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroCheque, transactionSource, dateEmission, dateValeur, dateOperation, observation, tvaDeductible);
    }

    // toString
    @Override
    public String toString() {
        return "ChequeTransactionRequest{" +
                "numeroCheque='" + numeroCheque + '\'' +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }
}
