package staport.finance.gBanque.impayees.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public class ImpayeeResponseDto {

    private Long id;
    private String libelle;
    private String observation;
    private BigDecimal montant;
    private Long banqueEncaissementId;
    private String banqueEncaissementNom;
    private TransactionSource motif;
    private LocalDate dateEmission ;
    private LocalDate dateValeur ;
    private LocalDate dateOperation ;
    private Status status;
    private OffsetDateTime dateCreation;
    private OffsetDateTime dateModification;

    // Getters & Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Long getBanqueEncaissementId() {
        return banqueEncaissementId;
    }

    public void setBanqueEncaissementId(Long banqueEncaissementId) {
        this.banqueEncaissementId = banqueEncaissementId;
    }

    public String getBanqueEncaissementNom() {
        return banqueEncaissementNom;
    }

    public void setBanqueEncaissementNom(String banqueEncaissementNom) {
        this.banqueEncaissementNom = banqueEncaissementNom;
    }

    public TransactionSource getMotif() {
        return motif;
    }

    public void setMotif(TransactionSource motif) {
        this.motif = motif;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OffsetDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(OffsetDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public OffsetDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(OffsetDateTime dateModification) {
        this.dateModification = dateModification;
    }

    @Override
    public String toString() {
        return "ImpayeeResponseDto{" +
                "id=" + id +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", montant=" + montant +
                ", banqueEncaissementId=" + banqueEncaissementId +
                ", banqueEncaissementNom='" + banqueEncaissementNom + '\'' +
                ", motif=" + motif +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", status=" + status +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                '}';
    }
}