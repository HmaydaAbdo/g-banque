package staport.finance.gBanque.validation.dtos.restitutions;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class RestitutionValidationRequest {

    @NotNull(message = "L'id de la restitution est obligatoire")
    private Long idRestitution;

    @NotNull(message = "La TVA est obligatoire")
    private Double tva;

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate dateEmission ;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur ;

    @NotNull(message = "La date d'opération est obligatoire")
    private LocalDate dateOperation ;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public RestitutionValidationRequest() {
    }

    public Long getIdRestitution() {
        return idRestitution;
    }

    public void setIdRestitution(Long idRestitution) {
        this.idRestitution = idRestitution;
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