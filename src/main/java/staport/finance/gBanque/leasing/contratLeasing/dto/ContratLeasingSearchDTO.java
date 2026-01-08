package staport.finance.gBanque.leasing.contratLeasing.dto;

import staport.finance.gBanque.leasing.contratLeasing.enums.TypeContratLeasing;

import java.math.BigDecimal;

public class ContratLeasingSearchDTO {

    private String numeroContrat;
    private Long banqueDePrelevementId;
    private Long organismeLeasingId;
    private String immobilisation;
    private TypeContratLeasing typeContrat;
    private Boolean completed;
    private BigDecimal montantEcheance;


    // Getters and Setters
    public String getNumeroContrat() { return numeroContrat; }
    public void setNumeroContrat(String numeroContrat) { this.numeroContrat = numeroContrat; }
    public Long getBanqueDePrelevementId() { return banqueDePrelevementId; }
    public void setBanqueDePrelevementId(Long banqueDePrelevementId) { this.banqueDePrelevementId = banqueDePrelevementId; }
    public Long getOrganismeLeasingId() { return organismeLeasingId; }
    public void setOrganismeLeasingId(Long organismeLeasingId) { this.organismeLeasingId = organismeLeasingId; }
    public String getImmobilisation() { return immobilisation; }
    public void setImmobilisation(String immobilisation) { this.immobilisation = immobilisation; }
    public TypeContratLeasing getTypeContrat() { return typeContrat; }
    public void setTypeContrat(TypeContratLeasing typeContrat) { this.typeContrat = typeContrat; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public BigDecimal getMontantEcheance() {
        return montantEcheance;
    }

    public void setMontantEcheance(BigDecimal montantEcheance) {
        this.montantEcheance = montantEcheance;
    }

    @Override
    public String toString() {
        return "ContratLeasingSearchDTO{" +
                "numeroContrat='" + numeroContrat + '\'' +
                ", banqueDePrelevementId=" + banqueDePrelevementId +
                ", organismeLeasingId=" + organismeLeasingId +
                ", immobilisation='" + immobilisation + '\'' +
                ", typeContrat=" + typeContrat +
                ", completed=" + completed +
                ", montantEcheance=" + montantEcheance +
                '}';
    }
}