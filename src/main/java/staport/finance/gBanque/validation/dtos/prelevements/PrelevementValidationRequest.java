package staport.finance.gBanque.validation.dtos.prelevements;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PrelevementValidationRequest {

    @NotNull(message = "L'id du prélèvement est obligatoire ")
    private Long idPrelevement;

    @NotNull(message = "TVA est obligatoire")
    private Double tva;

    @NotNull(message = "date d'mission est obligatoire ")
    private LocalDate dateEmission ;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur ;

    @NotNull(message = "La date d’opération est obligatoire")
    private LocalDate dateOperation ;

    @NotBlank(message = "L'observation' est obligatoire")
    private String observation;

    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public PrelevementValidationRequest() {
    }

    public Long getIdPrelevement() {
        return idPrelevement;
    }

    public void setIdPrelevement(Long idPrelevement) {
        this.idPrelevement = idPrelevement;
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

    public Double getTva() {
        return tva;
    }

    public void setTva(Double tva) {
        this.tva = tva;
    }

    public Long getRapprochementId() { return rapprochementId; }
    public void setRapprochementId(Long rapprochementId) { this.rapprochementId = rapprochementId; }

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
