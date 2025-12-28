package staport.finance.gBanque.droitConstate.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

public class DroitConstateSearchDto {

    private Long marcheId;
    private Long banqueId;
    private Type type;
    private Status status;

    public DroitConstateSearchDto() {
    }

    public DroitConstateSearchDto(Long marcheId, Long banqueId, Type type, Status status) {
        this.marcheId = marcheId;
        this.banqueId = banqueId;
        this.type = type;
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

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "DroitConstateSearchDto{" +
                "marcheId=" + marcheId +
                ", banqueId=" + banqueId +
                ", type=" + type +
                ", status=" + status +
                '}';
    }
}
