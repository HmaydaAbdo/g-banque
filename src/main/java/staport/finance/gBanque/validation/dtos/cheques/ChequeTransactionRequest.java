package staport.finance.gBanque.validation.dtos.cheques;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;

public class ChequeTransactionRequest {

    @NotNull(message = "Le id du chèque est obligatoire")
    private Long idCheque;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.CHEQUE;

    @NotNull(message = "La date d’émission est obligatoire")
    private LocalDate dateEmission ;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur ;

    @NotNull(message = "La date d’opération est obligatoire")
    private LocalDate dateOperation ;

    @NotBlank(message = "L’observation est obligatoire")
    private String observation;

    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    @NotNull(message = "La TVA déductible est obligatoire")
    private Double tvaDeductible;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    // Default constructor
    public ChequeTransactionRequest() {
    }


    public Long getIdCheque() {
        return idCheque;
    }

    public void setIdCheque(Long idCheque) {
        this.idCheque = idCheque;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
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

    public Long getRapprochementId() { return rapprochementId; }
    public void setRapprochementId(Long rapprochementId) { this.rapprochementId = rapprochementId; }

    @Override
    public String toString() {
        return "ChequeTransactionRequest{" +
                "idCheque='" + idCheque + '\'' +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }
}
