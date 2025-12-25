package staport.finance.gBanque.validation.dtos.lcn;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.OffsetDateTime;

public class LcnTransactionRequest {

    @NotNull(message = "Le id LCN est obligatoire")
    private Long id;

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


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    // toString
    @Override
    public String toString() {
        return "LcnTransactionRequest{" +
                "id='" + id + '\'' +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }
}
