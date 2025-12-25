package staport.finance.gBanque.factoring.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

public class FactoringSearchDto {

    private Long marcheId;
    private Long banqueId;
    private Type type;
    private Status status;

    public FactoringSearchDto() {
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
}
