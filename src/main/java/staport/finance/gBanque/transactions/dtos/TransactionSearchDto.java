package staport.finance.gBanque.transactions.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.util.List;

/**
 * DTO for filtering transactions with a focused set of criteria.
 */
public class TransactionSearchDto {

    private String libelle;
    private Long banqueId;
    private List<TransactionSource> transactionSources;
    private String observation;
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
}