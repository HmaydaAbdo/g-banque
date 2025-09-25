package staport.finance.gBanque.rapprochement.dtos.prefinancements;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public class PrefinancementTransactionRequest {

    @NotNull(message = "L'ID du préfinancement  est obligatoire.")
    private Long prefinancementId;
    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long  banqueId;
    @NotNull(message = "La date de valeur est obligatoire.")
    private OffsetDateTime dateValeur;
    @NotNull(message = "la date d'opération est obligatoire ")
    private OffsetDateTime dateOperation;

    @NotNull(message = "La TVA  est obligatoire.")
    private double tva;

    public Long getPrefinancementId() {
        return prefinancementId;
    }

    public void setPrefinancementId(Long prefinancementId) {
        this.prefinancementId = prefinancementId;
    }

    public OffsetDateTime getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(OffsetDateTime dateOperation) {
        this.dateOperation = dateOperation;
    }

    public OffsetDateTime getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(OffsetDateTime dateValeur) {
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

    public void setTva(double tvaDeductible) {
        this.tva = tvaDeductible;
    }
}
