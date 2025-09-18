package staport.finance.gBanque.decomptes.dto;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

public class DecompteSearchDto {

    private Long marcheId;
    private Long banqueId;

    private Status status;

    public DecompteSearchDto() {
    }

    public DecompteSearchDto(Long marcheId, Long banqueId, Status status) {
        this.marcheId = marcheId;
        this.banqueId = banqueId;
        this.status = status;

    }

    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }



    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
