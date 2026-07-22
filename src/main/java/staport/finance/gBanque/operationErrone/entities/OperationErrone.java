package staport.finance.gBanque.operationErrone.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Opération erronée rattachée à une banque.
 *
 * <p>Relation auditée : {@code banque} (déjà {@code @Audited}) — aucune annotation
 * {@code @NotAudited} nécessaire.
 */
@Audited
@Entity
public class OperationErrone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    private LocalDate dateEmission ;

    private LocalDate dateValeur ;

    private LocalDate dateOperation ;

    @Column(length = 150, nullable = false)
    private String libelle;
    @Column(length = 500, nullable = false)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionSource motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PREVU;


    public OperationErrone() {
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public TransactionSource getMotif() {
        return motif;
    }

    public void setMotif(TransactionSource motif) {
        this.motif = motif;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof OperationErrone that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(montant, that.montant) && Objects.equals(banque, that.banque) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateValeur, that.dateValeur) && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(libelle, that.libelle) && Objects.equals(observation, that.observation) && motif == that.motif && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur, dateOperation, libelle, observation, motif, status);
    }

    @Override
    public String toString() {
        return "OperationErrone{" +
                "id=" + id +
                ", montant=" + montant +
                ", banque=" + banque +
                ", dateEmission=" + dateEmission +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", motif=" + motif +
                ", status=" + status +
                '}';
    }
}