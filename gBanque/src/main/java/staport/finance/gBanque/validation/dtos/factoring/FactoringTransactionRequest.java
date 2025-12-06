package staport.finance.gBanque.validation.dtos.factoring;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class FactoringTransactionRequest {

    @NotNull(message = "L'ID du factoring  est obligatoire.")
    private Long factoringId;

    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long  banqueId;

    @NotNull(message = "La date d'émission est obligatoire.")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire.")
    private OffsetDateTime dateValeur;

    @NotNull(message = "la date d'opération est obligatoire ")
    private OffsetDateTime dateOperation;

    @NotEmpty(message = "libelle est obligatoire")
    private String libelle;

    @NotEmpty(message = "L'observation est obligatoire")
    private String observation;

    @NotNull (message = "TVA est obligatoire")
    private double tva;

    public FactoringTransactionRequest() {
    }

    public Long getFactoringId() {
        return factoringId;
    }

    public void setFactoringId(Long factoringId) {
        this.factoringId = factoringId;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
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

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public String getObservation() {
        return observation;
    }

    public void setObservation(String observation) {
        this.observation = observation;
    }

    public double getTva() {
        return tva;
    }

    public void setTva(double tva) {
        this.tva = tva;
    }
}
