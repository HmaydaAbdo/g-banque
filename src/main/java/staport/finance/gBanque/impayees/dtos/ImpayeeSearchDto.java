package staport.finance.gBanque.impayees.dtos;


import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;

public class ImpayeeSearchDto {

    private Long banqueEncaissementId;
    private String libelle;
    private TransactionSource motif;
    private BigDecimal montant;
    private Status status;


    public ImpayeeSearchDto() {
    }

    // Getters & Setters
    public Long getBanqueEncaissementId() {
        return banqueEncaissementId;
    }

    public void setBanqueEncaissementId(Long banqueEncaissementId) {
        this.banqueEncaissementId = banqueEncaissementId;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public TransactionSource getMotif() {
        return motif;
    }

    public void setMotif(TransactionSource motif) {
        this.motif = motif;
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
        return "ImpayeeSearchDto{" +
                "banqueEncaissementId=" + banqueEncaissementId +
                ", libelle='" + libelle + '\'' +
                ", motif=" + motif +
                ", montant=" + montant +
                ", status=" + status +
                '}';
    }
}