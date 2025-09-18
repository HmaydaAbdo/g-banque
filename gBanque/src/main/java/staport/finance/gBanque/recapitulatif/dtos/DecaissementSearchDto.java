package staport.finance.gBanque.recapitulatif.dtos;

import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.util.List;

public class DecaissementSearchDto {

    private List<TransactionSource> transactionSources;
    private Long banqueId;
    private String libelle;
    private String observation;

    public List<TransactionSource> getTransactionSources() {
        return transactionSources;
    }

    public void setTransactionSources(List<TransactionSource> transactionSources) {
        this.transactionSources = transactionSources;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getLibelle() {
        return libelle;
    }
    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
    public String getObservation() {
        return observation;
    }
    public void setObservation(String observation) {
        this.observation = observation;
    }
}
