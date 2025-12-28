package staport.finance.gBanque.droitConstate.dtos;


import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO for creating or updating a DroitConstate entity.
 * This DTO is used to receive data from the client, preventing exposure of the internal entity.
 */
public class DroitConstateRequestDto {

    @NotNull(message = "Marche ID cannot be null")
    private Long marcheId;

    @NotNull(message = "Date of emission cannot be null")
    private OffsetDateTime dateEmission;

    @NotNull(message = "Montant cannot be null")
    @DecimalMin(value = "0.01", message = "Montant must be a positive value")
    private BigDecimal montant;

    @NotNull(message = "Type cannot be null")
    private Type type;

    public DroitConstateRequestDto() {
    }

    public DroitConstateRequestDto(Long marcheId, OffsetDateTime dateEmission, BigDecimal montant, Type type) {
        this.marcheId = marcheId;
        this.dateEmission = dateEmission;
        this.montant = montant;
        this.type = type;
    }


    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    @Override
    public String toString() {
        return "DroitConstateRequestDto{" +
                "marcheId=" + marcheId +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                '}';
    }
}
