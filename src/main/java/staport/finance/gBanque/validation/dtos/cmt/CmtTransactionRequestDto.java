package staport.finance.gBanque.validation.dtos.cmt;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.Objects;

public class CmtTransactionRequestDto {
    private Long cmtId;
    private Long echeanceId;
    private TransactionSource transactionSource=TransactionSource.LEASING;
    private LocalDate dateEmission ;
    private LocalDate dateValeur ;
    private LocalDate dateOperation ;
    private String observation;
    private String libelle;
    private Double tvaDeductible;

    // Constructeur par défaut
    public CmtTransactionRequestDto() {
    }

    public CmtTransactionRequestDto(Long cmtId, Long echeanceId, TransactionSource transactionSource, LocalDate dateEmission, LocalDate dateValeur, LocalDate dateOperation, String observation , Double tvaDeductible) {
        this.cmtId = cmtId;
        this.echeanceId = echeanceId;
        this.transactionSource = transactionSource;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperation = dateOperation;
        this.observation = observation;
        this.tvaDeductible = tvaDeductible;
    }

    // Constructeur avec tous les champs


    // Getters et Setters


    public Long getCmtId() {
        return cmtId;
    }

    public void setCmtId(Long cmtId) {
        this.cmtId = cmtId;
    }

    public Long getEcheanceId() {
        return echeanceId;
    }

    public void setEcheanceId(Long echeanceId) {
        this.echeanceId = echeanceId;
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


    public double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public void setTransactionSource(TransactionSource transactionSource) {
        this.transactionSource = transactionSource;
    }

    public void setTvaDeductible(Double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    // Méthodes utilitaires (facultatif mais recommandé)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CmtTransactionRequestDto that = (CmtTransactionRequestDto) o;
        return Double.compare(that.tvaDeductible, tvaDeductible) == 0 &&
                Objects.equals(cmtId, that.cmtId) &&
                Objects.equals(echeanceId, that.echeanceId) &&
                Objects.equals(transactionSource, that.transactionSource) &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(observation, that.observation);
    }
    @Override
    public int hashCode() {
        return Objects.hash(cmtId, echeanceId, transactionSource, dateEmission, dateValeur,
                dateOperation, observation, tvaDeductible);
    }

    @Override
    public String toString() {
        return "CmtTransactionRequestDto{" +
                "cmtId='" + cmtId + '\'' +
                ", echeanceId=" + echeanceId +
                ", transactionSource='" + transactionSource + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }

}
