package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
public class VirementErrone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    private OffsetDateTime dateEmission;

    private OffsetDateTime dateValeur;

    private OffsetDateTime dateOperation;

    @Column(length = 150)
    private String libelle;

    @Column(length = 500)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PREVU;

    public VirementErrone() {
    }

    public VirementErrone(BigDecimal montant, Banque banque, OffsetDateTime dateEmission,
                          OffsetDateTime dateValeur, OffsetDateTime dateOperation,
                          String libelle, String observation, Status status) {
        this.montant = montant;
        this.banque = banque;
        this.dateEmission = dateEmission;
        this.dateValeur = dateValeur;
        this.dateOperation = dateOperation;
        this.libelle = libelle;
        this.observation = observation;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        VirementErrone that = (VirementErrone) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(montant, that.montant) &&
                Objects.equals(banque, that.banque) &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(libelle, that.libelle) &&
                Objects.equals(observation, that.observation) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur,
                dateOperation, libelle, observation, status);
    }

    @Override
    public String toString() {
        return "VirementErrone{" +
                "id=" + id +
                ", montant=" + montant +
                ", banque=" + banque +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", status=" + status +
                '}';
    }
}