package staport.finance.gBanque.moyensDePaiements.cheques.validation.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de requête pour la validation (rapprochement) d'un chèque.
 *
 * <p>Déplacé depuis {@code validation.dtos.cheques} vers le module {@code cheques} —
 * conforme à {@code architecture.md} Règle 1 : la logique de validation d'un module
 * appartient à ce module, pas au module transverse {@code validation}.
 *
 * <p><strong>Saisie de la TVA</strong> : {@code tauxTva} et {@code montantTva} sont mutuellement
 * exclusifs — exactement l'un des deux est renseigné. L'utilisateur choisit un taux dans la liste
 * déroulante, ou saisit directement le montant de TVA. La conversion taux → montant reste serveur
 * (voir {@code TvaResolver}) ; le frontend ne transmet jamais un montant dérivé d'un taux.
 * En validation en lot, seul {@code tauxTva} est utilisé : un montant fixe appliqué à N opérations
 * de montants différents n'aurait pas de sens comptable (voir {@code BatchValidationRequestDto}).
 */
public class ChequeTransactionRequest {

    @NotNull(message = "Le id du chèque est obligatoire")
    private Long idCheque;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.CHEQUE;

    @NotNull(message = "La date d’émission est obligatoire")
    private LocalDate dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private LocalDate dateValeur;

    @NotNull(message = "La date d’opération est obligatoire")
    private LocalDate dateOperation;

    @NotBlank(message = "L’observation est obligatoire")
    private String observation;

    @NotBlank(message = "libelle est obligatoire")
    private String libelle;

    /** Taux de TVA choisi dans la liste, en pourcentage (ex : 20 pour 20 %). Exclusif de {@link #montantTva}. */
    @DecimalMin(value = "0", message = "Le taux de TVA ne peut pas être négatif")
    @DecimalMax(value = "100", message = "Le taux de TVA ne peut pas dépasser 100")
    private Double tauxTva;

    /** Montant de TVA saisi directement, en MAD. Exclusif de {@link #tauxTva}. */
    @DecimalMin(value = "0.00", message = "Le montant de TVA ne peut pas être négatif")
    private BigDecimal montantTva;

    @NotNull(message = "rapprochementId est obligatoire")
    private Long rapprochementId;

    public ChequeTransactionRequest() {
    }

    public Long getIdCheque() {
        return idCheque;
    }

    public void setIdCheque(Long idCheque) {
        this.idCheque = idCheque;
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

    public Double getTauxTva() {
        return tauxTva;
    }

    public void setTauxTva(Double tauxTva) {
        this.tauxTva = tauxTva;
    }

    public BigDecimal getMontantTva() {
        return montantTva;
    }

    public void setMontantTva(BigDecimal montantTva) {
        this.montantTva = montantTva;
    }

    /**
     * Exactement l'une des deux saisies de TVA doit être renseignée — vérifié dès la couche
     * Jakarta, avant d'atteindre le service. {@code TvaResolver} refait la vérification pour les
     * appels internes (validation en lot) qui ne passent pas par la validation du contrôleur.
     */
    @JsonIgnore
    @AssertTrue(message = "Renseigner soit un taux de TVA, soit un montant de TVA, mais pas les deux")
    public boolean isSaisieTvaCoherente() {
        return (tauxTva == null) ^ (montantTva == null);
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
        return "ChequeTransactionRequest{" +
                "idCheque='" + idCheque + '\'' +
                ", transactionSource=" + transactionSource +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", tauxTva=" + tauxTva +
                ", montantTva=" + montantTva +
                '}';
    }
}
