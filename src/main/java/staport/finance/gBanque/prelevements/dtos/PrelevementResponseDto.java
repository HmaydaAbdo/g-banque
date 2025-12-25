package staport.finance.gBanque.prelevements.dtos;

import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class PrelevementResponseDto {


    private Long id;
    private String libelle;
    private String observation;
    private BigDecimal montant;
    private Long banqueId;
    private String banqueNom;
    private OffsetDateTime dateEmission;
    private OffsetDateTime dateValeur;
    private OffsetDateTime dateOperation;
    private Status status;


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

    public Long getBanqueId() {
        return banqueId;
    }

    public void setBanqueId(Long banqueId) {
        this.banqueId = banqueId;
    }

    public String getBanqueNom() {
        return banqueNom;
    }

    public void setBanqueNom(String banqueNom) {
        this.banqueNom = banqueNom;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "PrelevementResponseDto{" +
                "id=" + id +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", montant=" + montant +
                ", banqueId=" + banqueId +
                ", banqueNom='" + banqueNom + '\'' +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", status=" + status +
                '}';
    }
}
