package staport.finance.gBanque.transactions.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO for filtering transactions with a focused set of criteria.
 */
public class TransactionSearchDto {

    private String libelle;
    private Long banqueId;
    private List<TransactionSource> transactionSources;
    private String observation;
    private Boolean isConfirmed;
    private BigDecimal montant;
    private OffsetDateTime dateOperationDebut;
    private OffsetDateTime   dateOperationFin;
    // Getters and Setters

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public Boolean getConfirmed() {
        return isConfirmed;
    }

    public void setConfirmed(Boolean confirmed) {
        isConfirmed = confirmed;
    }

    public List<TransactionSource> getTransactionSources() {
        return transactionSources;
    }

    public void setTransactionSources(List<TransactionSource> transactionSources) {
        this.transactionSources = transactionSources;
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

    public OffsetDateTime getDateOperationDebut() {
        return dateOperationDebut;
    }

    public void setDateOperationDebut(OffsetDateTime dateOperationDebut) {
        this.dateOperationDebut = dateOperationDebut;
    }

    public OffsetDateTime getDateOperationFin() {
        return dateOperationFin;
    }

    public void setDateOperationFin(OffsetDateTime dateOperationFin) {
        this.dateOperationFin = dateOperationFin;
    }

    @Override
    public String toString() {
        return "TransactionSearchDto{" +
                "libelle='" + libelle + '\'' +
                ", banqueId=" + banqueId +
                ", transactionSources=" + transactionSources +
                ", observation='" + observation + '\'' +
                ", isConfirmed=" + isConfirmed +
                ", montant=" + montant +
                ", dateOperationDebut=" + dateOperationDebut +
                ", dateOperationFin=" + dateOperationFin +
                '}';
    }
}