package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

public class AgiosSearchDto {

    private Long banqueId;
    private String libelle;
    private Status status;

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
}
