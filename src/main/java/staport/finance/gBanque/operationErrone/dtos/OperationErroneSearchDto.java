// ============================================================
// OperationErroneSearchDto.java
// ============================================================
package staport.finance.gBanque.operationErrone.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

public class OperationErroneSearchDto {

    private Long banqueId;
    private String libelle;
    private BigDecimal montant;
    private Status status;
    private TransactionSource motif;

    public OperationErroneSearchDto() {}

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

    public TransactionSource getMotif() {
        return motif;
    }

    public void setMotif(TransactionSource motif) {
        this.motif = motif;
    }

    @Override
    public String toString() {
        return "OperationErroneSearchDto{" +
                "banqueId=" + banqueId +
                ", libelle='" + libelle + '\'' +
                ", montant=" + montant +
                ", status=" + status +
                ", motif=" + motif +
                '}';
    }
}
