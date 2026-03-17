package staport.finance.gBanque.validation.dtos.impotsEtTaxes;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class ImpotEtTaxeValidationRequest {

    @NotNull(message = "L'id de l'impôt/taxe est obligatoire")
    private Long idImpotEtTaxe;

    @NotNull(message = "TVA est obligatoire")
    private Double tva;

    @NotNull(message = "Date d'émission est obligatoire")
    private LocalDate dateEmission ;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur ;

    @NotNull(message = "La date d'opération est obligatoire")
    private LocalDate dateOperation ;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    public ImpotEtTaxeValidationRequest() {
    }

    public Long getIdImpotEtTaxe() {
        return idImpotEtTaxe;
    }

    public void setIdImpotEtTaxe(Long idImpotEtTaxe) {
        this.idImpotEtTaxe = idImpotEtTaxe;
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

    @Override
    public String toString() {
        return "ImpotEtTaxeValidationRequest{" +
                "idImpotEtTaxe=" + idImpotEtTaxe +
                ", tva=" + tva +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                '}';
    }
}