package staport.finance.gBanque.impot_taxes.dtos;

import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;

public class ImpotEtTaxeSearchDto {

    private Long banqueId;
    private String libelle;
    private Motif motif;
    private BigDecimal montant;
    private Status status;

    public ImpotEtTaxeSearchDto() {
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

    public Motif getMotif() {
        return motif;
    }

    public void setMotif(Motif motif) {
        this.motif = motif;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ImpotEtTaxeSearchDto{" +
                "banqueId=" + banqueId +
                ", libelle='" + libelle + '\'' +
                ", motif=" + motif +
                ", montant=" + montant +
                ", status=" + status +
                '}';
    }
}