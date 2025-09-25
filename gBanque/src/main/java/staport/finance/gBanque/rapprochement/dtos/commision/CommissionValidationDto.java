package staport.finance.gBanque.rapprochement.dtos.commision;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class CommissionValidationDto {

    @NotNull(message = "L'id de la commission est obligatoire ")
    private Long idCommission;

    @NotNull(message = "La date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "La date d’opération est obligatoire")
    private OffsetDateTime dateOperation;

    @NotBlank(message = "L'observation' est obligatoire")
    private String observation;




    public CommissionValidationDto() {
    }

    public Long getIdCommission() {
        return idCommission;
    }

    public void setIdCommission(Long idCommission) {
        this.idCommission = idCommission;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
        this.dateValeur = dateValeur;
    }

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }
}
