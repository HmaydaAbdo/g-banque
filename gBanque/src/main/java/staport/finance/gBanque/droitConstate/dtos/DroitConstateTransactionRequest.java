package staport.finance.gBanque.droitConstate.dtos;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class DroitConstateTransactionRequest {
    @NotNull(message = "L'ID du droit constaté  est obligatoire.")
    private Long droitConstateId;
    @NotNull(message = "L'ID de la banque est obligatoire.")
    private Long  banqueId;
    @NotNull(message = "La date de valeur est obligatoire.")
    private OffsetDateTime dateValeur;
    @NotNull(message = "la date d'opération est obligatoire ")
    private OffsetDateTime dateOperation;
    @NotNull (message = "TVA est obligatoire")
    private double tva;

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
}
