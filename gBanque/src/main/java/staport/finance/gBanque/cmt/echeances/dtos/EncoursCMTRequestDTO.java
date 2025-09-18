package staport.finance.gBanque.cmt.echeances.dtos;

import jakarta.validation.constraints.NotNull;

public class EncoursCMTRequestDTO {

    @NotNull(message = "L'identifiant de la banque est obligatoire.")
    private Long banqueId;

    @NotNull(message = "La période de calcul des encours est obligatoire (JOUR ou MOIS).")
    private PeriodeEncoursCMT periode;

    private String designation;

    public EncoursCMTRequestDTO() {
    }

    public EncoursCMTRequestDTO(Long banqueId, PeriodeEncoursCMT periode, String designation) {
        this.banqueId = banqueId;
        this.periode = periode;
        this.designation = designation;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public PeriodeEncoursCMT getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeEncoursCMT periode) {
        this.periode = periode;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}