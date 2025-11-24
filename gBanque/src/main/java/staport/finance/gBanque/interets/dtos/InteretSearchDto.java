package staport.finance.gBanque.interets.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

public class InteretSearchDto {

    private Long banqueId;
    private String libelle;
    private BigDecimal montant;
    private Status status;


    public InteretSearchDto() {}

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

    @Override
    public String toString() {
        return "InteretSearchDto{" +
                "banqueId=" + banqueId +
                ", libelle='" + libelle + '\'' +
                ", montant=" + montant +
                ", status=" + status +
                '}';
    }
}
