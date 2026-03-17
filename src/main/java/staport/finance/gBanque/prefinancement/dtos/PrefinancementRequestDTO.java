package staport.finance.gBanque.prefinancement.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.prefinancement.enums.Type;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for creating or updating a Prefinancement.
 * This DTO is used to receive data from the client, preventing exposure of the internal entity.
 */
public class PrefinancementRequestDTO {

    @NotNull(message = "Marche ID cannot be null")
    private Long marcheId;

    @NotNull(message = "Date of emission cannot be null")
    private LocalDate dateEmission ;

    @NotNull(message = "Montant cannot be null")
    @DecimalMin(value = "0.01", message = "Montant must be a positive value")
    private BigDecimal montant;

    @NotNull(message = "Type cannot be null")
    private Type type;

    public PrefinancementRequestDTO() {
    }

    public PrefinancementRequestDTO(Long marcheId, LocalDate dateEmission, BigDecimal montant, Type type  ) {
        this.marcheId = marcheId;
        this.dateEmission = dateEmission;
        this.montant = montant;
        this.type = type;
    }

    // Getters and setters
    public Long getMarcheId() {
        return marcheId;
    }

    public void setMarcheId(Long marcheId) {
        this.marcheId = marcheId;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "PrefinancementRequestDTO{" +
                "marcheId=" + marcheId +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                '}';
    }
}