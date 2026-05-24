package staport.finance.gBanque.validation.dtos.droitConstate;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class DroitConstateTransactionRequest {
    @NotNull(message = "L'ID du droit constaté  est obligatoire.")
    private Long droitConstateId;

    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long  banqueId;

    @NotNull(message = "La date d'émission est obligatoire.")
    private LocalDate dateEmission ;

    @NotNull(message = "La date de valeur est obligatoire.")
    private LocalDate dateValeur ;

    @NotNull(message = "la date d'opération est obligatoire ")
    private LocalDate dateOperation ;

    @NotEmpty(message = "libelle est obligatoire")
    private String libelle;

    @NotEmpty(message = "L'observation est obligatoire")
    private String observation;

    @NotNull (message = "TVA est obligatoire")
    private double tva;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public Long getDroitConstateId() {
        return droitConstateId;
    }

    public void setDroitConstateId(Long droitConstateId) {
        this.droitConstateId = droitConstateId;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
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

    public double getTva() {
        return tva;
    }

    public void setTva(double tva) {
        this.tva = tva;
    }

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
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

    public Long getRapprochementId() { return rapprochementId; }
    public void setRapprochementId(Long rapprochementId) { this.rapprochementId = rapprochementId; }
}
