package staport.finance.gBanque.cmt.echeances.dtos;

import jakarta.validation.constraints.NotNull;

public class EncoursCMTRequestDTO {

    @NotNull(message = "L'identifiant de la banque est obligatoire.")
    private Long banqueId;

    private String designation;

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
}