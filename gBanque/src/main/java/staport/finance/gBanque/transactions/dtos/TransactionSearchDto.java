package staport.finance.gBanque.transactions.dtos;

/**
 * DTO for filtering transactions with a focused set of criteria.
 */
public class TransactionSearchDto {

    private String libelle;
    private Long banqueId;
    private String transactionSource;

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


    public String getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(String transactionSource) {
        this.transactionSource = transactionSource;
    }
}