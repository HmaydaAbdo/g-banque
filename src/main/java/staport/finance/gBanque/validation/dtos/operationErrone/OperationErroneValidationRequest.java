package staport.finance.gBanque.validation.dtos.operationErrone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class OperationErroneValidationRequest {

    @NotNull(message = "L'id de l operation  erronée est obligatoire")
    private Long idOperationErrone;

    @NotNull(message = "TVA est obligatoire")
    private Double tva;

    @NotNull(message = "date d'émission est obligatoire")
    private LocalDate dateEmission ;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur ;

    @NotNull(message = "La date d'opération est obligatoire")
    private LocalDate dateOperation ;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    public OperationErroneValidationRequest() {
    }


    public Long getIdOperationErrone() {
        return idOperationErrone;
    }

    public void setIdOperationErrone(Long idOperationErrone) {
        this.idOperationErrone = idOperationErrone;
    }

    public Double getTva() {
        return tva;
    }

    public void setTva(Double tva) {
        this.tva = tva;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }
}
