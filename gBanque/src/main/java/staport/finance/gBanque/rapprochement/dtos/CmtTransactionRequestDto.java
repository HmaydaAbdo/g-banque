package staport.finance.gBanque.rapprochement.dtos;

import staport.finance.gBanque.rapprochement.enums.TransactionSource;

import java.time.OffsetDateTime;
import java.util.Objects;

public class CmtTransactionRequestDto {
    private Long cmtId;
    private Long echeanceId;
    private TransactionSource transactionSource=TransactionSource.LEASING;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateValeur;
    private OffsetDateTime dateOperation;
    private String observation;
    private double tvaCollectee;
    private double tvaDeductible;

    // Constructeur par défaut
    public CmtTransactionRequestDto() {
    }

    public CmtTransactionRequestDto(Long cmtId, Long echeanceId, TransactionSource transactionSource, OffsetDateTime dateEmission, OffsetDateTime dateValeur, OffsetDateTime dateOperation, String observation, double tvaCollectee, double tvaDeductible) {
        this.cmtId = cmtId;
        this.echeanceId = echeanceId;
        this.transactionSource = transactionSource;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperation = dateOperation;
        this.observation = observation;
        this.tvaCollectee = tvaCollectee;
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

    public double getTvaCollectee() {
        return tvaCollectee;
    }

    public void setTvaCollectee(double tvaCollectee) {
        this.tvaCollectee = tvaCollectee;
    }

    public double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    // Méthodes utilitaires (facultatif mais recommandé)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CmtTransactionRequestDto that = (CmtTransactionRequestDto) o;
        return Double.compare(that.tvaCollectee, tvaCollectee) == 0 &&
                Double.compare(that.tvaDeductible, tvaDeductible) == 0 &&
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
                dateOperation, observation, tvaCollectee, tvaDeductible);
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
                ", tvaCollectee=" + tvaCollectee +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }

}
