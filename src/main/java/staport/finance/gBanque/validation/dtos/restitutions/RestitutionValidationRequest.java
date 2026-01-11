package staport.finance.gBanque.validation.dtos.restitutions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public class RestitutionValidationRequest {

    @NotNull(message = "L'id de la restitution est obligatoire")
    private Long idRestitution;

    @NotNull(message = "La TVA est obligatoire")
    private Double tva;

    @NotNull(message = "La date d'émission est obligatoire")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "La date d'opération est obligatoire")
    private OffsetDateTime dateOperation;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    public RestitutionValidationRequest() {
    }

    public Long getIdRestitution() {
        return idRestitution;
    }

    public void setIdRestitution(Long idRestitution) {
        this.idRestitution = idRestitution;
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

    public Double getTva() {
        return tva;
    }

    public void setTva(Double tva) {
        this.tva = tva;
    }

    @Override
    public String toString() {
        return "RestitutionValidationRequest{" +
                "idRestitution=" + idRestitution +
                ", tva=" + tva +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}