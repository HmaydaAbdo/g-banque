package staport.finance.gBanque.prefinancement.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

/**
 * DTO for searching Prefinancement records.
 * This DTO contains the query parameters specified by the user.
 * Fields are optional (can be null), allowing for flexible search criteria.
 * No validation is needed on this DTO since all fields are optional for search.
 */
public class PrefinancementSearchDTO {
    private Long marcheId;
    private Long banqueId;
    private Type type;
    private Status status;

    public PrefinancementSearchDTO() {
    }

    public PrefinancementSearchDTO(Long marcheId, Long banqueId, Type type, Status status) {
        this.marcheId = marcheId;
        this.banqueId = banqueId;
        this.type = type;
        this.status = status;
    }

    // Getters and setters
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