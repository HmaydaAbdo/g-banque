package staport.finance.gBanque.commision.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

public class CommissionSearchDto {

    private Long banqueId;
    private String libelle;
    private TransactionSource operation;
    private BigDecimal montant;
    private Status status;

    /**
     * Filtre sur la nature de la ligne fonctionnelle de la caution d'origine
     * (commission.caution.nature.id). Sans effet pour les commissions sans caution associée
     * (ex : commissions sur virements).
     */
    private Long natureId;

    public CommissionSearchDto() {
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public Long getNatureId() {
        return natureId;
    }

    public void setNatureId(Long natureId) {
        this.natureId = natureId;
    }


    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public TransactionSource getOperation() {
        return operation;
    }

    public void setOperation(TransactionSource operation) {
        this.operation = operation;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "CommissionSearchDto{" +
                "banqueId=" + banqueId +
                ", libelle='" + libelle + '\'' +
                ", operation=" + operation +
                ", montant=" + montant +
                ", status=" + status +
                ", natureId=" + natureId +
                '}';
    }
}
