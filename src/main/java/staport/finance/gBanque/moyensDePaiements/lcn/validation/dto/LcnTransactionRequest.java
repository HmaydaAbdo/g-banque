package staport.finance.gBanque.moyensDePaiements.lcn.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'un LCN.
 *
 * <p>Déplacé depuis {@code validation.dtos.lcn} vers le module {@code lcn} —
 * conforme à {@code architecture.md} Règle 1 : la logique de validation d'un module
 * appartient à ce module, pas au module transverse {@code validation}.
 */
public class LcnTransactionRequest {

    @NotNull(message = "Le id LCN est obligatoire")
    private Long id;

    // Jamais renseigné par l'appelant (pas de setter) — comportement identique à l'ancien
    // LcnTransactionRequest (validation/dtos/lcn), non modifié dans ce sweep.
    private TransactionSource transactionSource;

    @NotNull(message = "La date d’émission est obligatoire")
    private LocalDate dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur;

    @NotNull(message = "La date d’opération est obligatoire")
    private LocalDate dateOperation;

    @NotBlank(message = "L’observation est obligatoire")
    private String observation;

    @NotNull(message = "La TVA déductible est obligatoire")
    private Double tvaDeductible;

    @NotNull(message = "libelle  est obligatoire")
    private String libelle;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public LcnTransactionRequest() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
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

    public Double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(Double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getRapprochementId() {
        return rapprochementId;
    }

    public void setRapprochementId(Long rapprochementId) {
        this.rapprochementId = rapprochementId;
    }

    @Override
    public String toString() {
        return "LcnTransactionRequest{" +
                "id=" + id +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                '}';
    }
}
