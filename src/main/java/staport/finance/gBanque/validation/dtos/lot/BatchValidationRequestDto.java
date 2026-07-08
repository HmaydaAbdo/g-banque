package staport.finance.gBanque.validation.dtos.lot;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.LocalDate;
import java.util.List;

/**
 * Requête de validation (rapprochement) en lot : applique les mêmes champs communs
 * (dates, TVA, libellé, observation, rapprochement cible) à un ensemble d'opérations
 * de même type et même sens, en une seule opération atomique.
 * <p>
 * {@code sens} n'est obligatoire que pour les types qui distinguent décaissement et encaissement
 * (Virement, Cheque, Lcn, MiseADisposition, VirementInternational) ; ignoré pour les autres types.
 * <p>
 * {@code banqueId} n'est obligatoire que pour les types dont l'endpoint unitaire l'exige
 * (Decompte, DroitConstate, Factoring, Prefinancement) ; ignoré pour les autres types.
 */
public class BatchValidationRequestDto {

    @NotNull(message = "La source de transaction est obligatoire")
    private TransactionSource transactionSource;

    private SensOperation sens;

    private Long banqueId;

    @NotEmpty(message = "La liste des identifiants à valider est obligatoire et ne peut être vide")
    private List<Long> ids;

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
    private Double tvaDeductible;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public BatchValidationRequestDto() {
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public void setTransactionSource(TransactionSource transactionSource) {
        this.transactionSource = transactionSource;
    }

    public SensOperation getSens() {
        return sens;
    }

    public void setSens(SensOperation sens) {
        this.sens = sens;
    }

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
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

    public Double getTvaDeductible() {
        return tvaDeductible;
    }

    public void setTvaDeductible(Double tvaDeductible) {
        this.tvaDeductible = tvaDeductible;
    }

    public Long getRapprochementId() {
        return rapprochementId;
    }

    public void setRapprochementId(Long rapprochementId) {
        this.rapprochementId = rapprochementId;
    }

    @Override
    public String toString() {
        return "BatchValidationRequestDto{" +
                "transactionSource=" + transactionSource +
                ", sens=" + sens +
                ", banqueId=" + banqueId +
                ", ids=" + ids +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", tvaDeductible=" + tvaDeductible +
                ", rapprochementId=" + rapprochementId +
                '}';
    }
}
