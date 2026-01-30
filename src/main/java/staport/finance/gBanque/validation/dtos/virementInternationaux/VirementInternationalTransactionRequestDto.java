package staport.finance.gBanque.validation.dtos.virementInternationaux;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.time.OffsetDateTime;
import java.util.Objects;

public class VirementInternationalTransactionRequestDto {

    @NotNull(message = "L'id du virement international est obligatoire")
    private Long id;

    @NotNull(message = "La source de transaction est obligatoire")
    private final TransactionSource transactionSource = TransactionSource.VIREMENT_INTERNATIONAL;

    @NotNull(message = "La date d'émission est obligatoire")
    private OffsetDateTime dateEmission;

    @NotNull(message = "La date de valeur est obligatoire")
    private OffsetDateTime dateValeur;

    @NotNull(message = "La date d'opération est obligatoire")
    private OffsetDateTime dateOperation;

    @NotBlank(message = "L'observation est obligatoire")
    private String observation;

    @NotBlank(message = "Le libellé est obligatoire")
    private String libelle;

    @NotNull(message = "La TVA déductible est obligatoire")
    private Double tva;

    // Default constructor
    public VirementInternationalTransactionRequestDto() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TransactionSource getTransactionSource() {
        return transactionSource;
    }

    public OffsetDateTime getDateEmission() {
        return dateEmission;
    }

    public void setDateEmission(OffsetDateTime dateEmission) {
        this.dateEmission = dateEmission;
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

    // equals & hashCode
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

    // toString
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