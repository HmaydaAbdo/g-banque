package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.Objects;

/**
 * DTO de requête pour la validation (rapprochement) d'un virement international.
 *
 * <p>Déplacé depuis {@code validation.dtos.virementInternationaux} vers le module
 * {@code virementInternational} — conforme à {@code architecture.md} Règle 1 : la logique de
 * validation d'un module appartient à ce module, pas au module transverse {@code validation}.
 *
 * <p>Champ {@code tva} (et non {@code tvaDeductible} comme les autres moyens de paiement) —
 * comportement identique à l'ancien DTO, non renommé dans ce sweep.
 */
public class VirementInternationalTransactionRequestDto {

    @NotNull(message = "L'id du virement international est obligatoire")
    private Long id;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.VIREMENT_INTERNATIONAL;

    @NotNull(message = "La date d'émission est obligatoire")
    private LocalDate dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur;

    @NotNull(message = "La date d'opération est obligatoire")
    private LocalDate dateOperation;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La TVA déductible est obligatoire")
    private Double tva;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public VirementInternationalTransactionRequestDto() {
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

    public Double getTva() {
        return tva;
    }

    public void setTva(Double tva) {
        this.tva = tva;
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public Long getRapprochementId() { return rapprochementId; }
    public void setRapprochementId(Long rapprochementId) { this.rapprochementId = rapprochementId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirementInternationalTransactionRequestDto)) return false;
        VirementInternationalTransactionRequestDto that = (VirementInternationalTransactionRequestDto) o;
        return Objects.equals(id, that.id) &&
                transactionSource == that.transactionSource &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(observation, that.observation) &&
                Objects.equals(libelle, that.libelle) &&
                Objects.equals(tva, that.tva);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, transactionSource, dateEmission, dateValeur, dateOperation, observation, libelle, tva);
    }

    @Override
    public String toString() {
        return "VirementInternationalTransactionRequestDto{" +
                "id=" + id +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", tva=" + tva +
                '}';
    }
}
