package staport.finance.gBanque.provision.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Provision : opération financière manuelle rattachée à une banque, dont le montant est toujours
 * positif et dont {@code sens} (ENCAISSEMENT/DECAISSEMENT) détermine l'impact sur le solde une
 * fois validée (voir {@code ProvisionTransactionMapper}).
 * <p>Relation auditée : {@code banque} (déjà {@code @Audited}) — aucune annotation
 * {@code @NotAudited} nécessaire.
 */
@Audited
@Entity
public class Provision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    private LocalDate dateEmission;
    private LocalDate dateValeur;
    private LocalDate dateOperation;

    @Column(length = 150, nullable = false)
    private String libelle;
    @Column(length = 500, nullable = false)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionSource motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SensOperation sens;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PREVU;

    public Provision() {
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

    public TransactionSource getMotif() {
        return motif;
    }

    public void setMotif(TransactionSource motif) {
        this.motif = motif;
    }

    public SensOperation getSens() {
        return sens;
    }

    public void setSens(SensOperation sens) {
        this.sens = sens;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Provision that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(montant, that.montant) && Objects.equals(banque, that.banque)
                && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateValeur, that.dateValeur)
                && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(libelle, that.libelle)
                && Objects.equals(observation, that.observation) && motif == that.motif && sens == that.sens
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur, dateOperation, libelle, observation, motif, sens, status);
    }

    @Override
    public String toString() {
        return "Provision{" +
                "id=" + id +
                ", montant=" + montant +
                ", banque=" + banque +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", motif=" + motif +
                ", sens=" + sens +
                ", status=" + status +
                '}';
    }
}
