package staport.finance.gBanque.rapprochement.dtos.prelevements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class PrelevementValidationRequest {

    @NotNull(message = "L'id du prélèvement est obligatoire ")
    private Long idPrelevement;

    @NotNull(message = "TVA est obligatoire")
    private Double tva;

    @NotNull(message = "date d'mission est obligatoire ")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "La date d’opération est obligatoire")
    private OffsetDateTime dateOperation;

    @NotBlank(message = "L'observation' est obligatoire")
    private String observation;

    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    public PrelevementValidationRequest() {
    }

    public Long getIdPrelevement() {
        return idPrelevement;
    }

    public void setIdPrelevement(Long idPrelevement) {
        this.idPrelevement = idPrelevement;
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
        return "PrelevementValidationRequest{" +
                "idPrelevement=" + idPrelevement +
                ", tva=" + tva +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}
