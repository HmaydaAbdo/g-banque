package staport.finance.gBanque.impayees.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Impayée (chèque/effet impayé) rattachée à une banque.
 *
 * <p>Relation auditée : {@code banque} (déjà {@code @Audited}) — aucune annotation
 * {@code @NotAudited} nécessaire.
 */
@Audited
@Entity
public class Impayee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column(nullable = false)
    private LocalDate dateEmission ;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(name = "type",nullable = false)
    private Type type;

    @Column
    private LocalDate dateValeur ;

    @Column
    private LocalDate dateOperation ;

    /**
     * Numéro du moyen de paiement d'origine à l'origine de l'impayée (numéro de chèque ou de LCN).
     * Obligatoire lorsque {@code motif} vaut {@code CHEQUE} ou {@code LCN} — voir la validation
     * dans {@code ImpayeeServiceImpl}. Optionnel pour les autres motifs.
     */
    @Column(name = "numero")
    private String numero;

    @Column(length = 500, nullable = false)
    private String libelle;

    @Column(length = 500, nullable = false)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionSource motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PREVU;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private OffsetDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    // JPA callbacks for audit fields
    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.dateCreation = now;
        this.dateModification = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.dateModification = OffsetDateTime.now();
    }

    public Impayee() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getMontant() {
        return montant;
    }

    public void setMontant(BigDecimal montant) {
        this.montant = montant;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
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

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
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
    public boolean equals(Object o) {
        if (!(o instanceof Impayee that)) return false;
        return Objects.equals(id, that.id) &&
                Objects.equals(montant, that.montant) &&
                Objects.equals(banque, that.banque) &&
                Objects.equals(dateEmission, that.dateEmission) &&
                Objects.equals(dateValeur, that.dateValeur) &&
                Objects.equals(dateOperation, that.dateOperation) &&
                Objects.equals(libelle, that.libelle) &&
                Objects.equals(observation, that.observation) &&
                Objects.equals(numero, that.numero) &&
                motif == that.motif &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur,
                dateOperation, libelle, observation, numero, motif, status);
    }

    @Override
    public String toString() {
        return "Impayee{" +
                "id=" + id +
                ", banque=" + banque +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                ", dateValeur=" + dateValeur +
                ", dateOperation=" + dateOperation +
                ", numero='" + numero + '\'' +
                ", libelle='" + libelle + '\'' +
                ", observation='" + observation + '\'' +
                ", motif=" + motif +
                ", status=" + status +
                ", dateCreation=" + dateCreation +
                ", dateModification=" + dateModification +
                '}';
    }
}