package staport.finance.gBanque.rapprochement.dtos.decomptes;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class DecompteTransactionRequestDto {

    @NotNull(message = "L'ID du décompte  est obligatoire.")
    private Long decompteId;

    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long  banqueId;

    @NotNull(message = "La date d'émission est obligatoire.")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire.")
    private OffsetDateTime dateValeur;

    @NotNull(message = "la date d'opération est obligatoire ")
    private OffsetDateTime dateOperation;

    @NotNull(message = "TVA est obligatoire")
    private double tva;

    @NotEmpty(message = "L'observation est obligatoire")
    private String observation;

    @NotEmpty(message = "libelle est obligatoire ")
    private String libelle;

    public Long getDecompteId() {
        return decompteId;
    }

    public void setDecompteId(Long decompteId) {
        this.decompteId = decompteId;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
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

    public double getTva() {
        return tva;
    }
    public void setTva(double tva) {
        this.tva = tva;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
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
