package staport.finance.gBanque.cmt.echeances.dtos;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class EncoursCMTRequestDTO {

    @NotNull(message = "L'identifiant de la banque est obligatoire.")
    private Long banqueId;

    private String designation;

    private BigDecimal montant;

    public EncoursCMTRequestDTO() {
    }

    public EncoursCMTRequestDTO(Long banqueId, String designation) {
        this.banqueId = banqueId;
        this.designation = designation;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "EncoursCMTRequestDTO{" +
                "banqueId=" + banqueId +
                ", designation='" + designation + '\'' +
                ", montant=" + montant +
                '}';
    }
}