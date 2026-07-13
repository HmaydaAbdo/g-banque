package staport.finance.gBanque.decomptes.validation.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'un décompte.
 *
 * <p>Déplacé depuis {@code validation.dtos.decomptes} vers le module {@code decomptes} —
 * conforme à {@code architecture.md} Règle 1.
 */
public class DecompteTransactionRequestDto {

    @NotNull(message = "L'ID du décompte est obligatoire.")
    private Long decompteId;

    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long banqueId;

    @NotNull(message = "La date d'émission est obligatoire.")
    private LocalDate dateEmission;

    @NotNull(message = "La date de valeur est obligatoire.")
    private LocalDate dateValeur;

    @NotNull(message = "la date d'opération est obligatoire ")
    private LocalDate dateOperation;

    @NotNull(message = "La TVA  est obligatoire.")
    private double tva;

    @NotNull(message = "libelle est obligatoire")
    private String libelle;

    @NotNull(message = "L'observation est obligatoire")
    private String observation;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public Long getDecompteId() {
        return decompteId;
    }

    public void setDecompteId(Long decompteId) {
        this.decompteId = decompteId;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public double getTva() {
        return tva;
    }

    public void setTva(double tva) {
        this.tva = tva;
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

    public LocalDate getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(LocalDate dateEmission) {
        this.dateEmission = dateEmission;
    }

    public Long getRapprochementId() { return rapprochementId; }
    public void setRapprochementId(Long rapprochementId) { this.rapprochementId = rapprochementId; }
}
