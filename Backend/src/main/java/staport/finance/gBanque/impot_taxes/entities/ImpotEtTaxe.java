package staport.finance.gBanque.impot_taxes.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.impot_taxes.enums.Motif;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Impôt ou taxe (TVA, IR, IS, pénalité, etc.) rattaché à une banque.
 *
 * <p>Relations auditées : {@code banque} (déjà {@code @Audited}), {@code commission} et
 * {@code caution} (déjà {@code @Audited} toutes les deux) — aucune annotation
 * {@code targetAuditMode=NOT_AUDITED} nécessaire (cf. {@code feedback_envers_audit_relations}).
 */
@Entity
@Audited
@Table(
        name = "impot_et_taxe",
        indexes = {
                @Index(name = "idx_impot_et_taxe_banque_id", columnList = "banque_id"),
                @Index(name = "idx_impot_et_taxe_status", columnList = "status"),
                @Index(name = "idx_impot_et_taxe_motif", columnList = "motif"),
                @Index(name = "idx_impot_et_taxe_date_emission", columnList = "dateEmission")
        }
)
@NamedEntityGraph(
        name = "ImpotEtTaxe.avecBanque",
        attributeNodes = @NamedAttributeNode("banque")
)
public class ImpotEtTaxe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Column
    private LocalDate dateEmission ;

    @Column
    private LocalDate dateValeur ;

    @Column
    private LocalDate dateOperation ;

    @Column(length = 500, nullable = false)
    private String libelle;

    @Column(length = 500, nullable = false)
    private String observation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 50)
    private Motif motif;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status=Status.PREVU;

    /**
     * Commission à l'origine de cet impôt/taxe — nullable.
     * Renseigné uniquement pour les TVA générées automatiquement depuis une commission caution.
     * Permet de retrouver et mettre à jour la TVA si le montant de la commission change.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", unique = true)
    private Commission commission;

    /**
     * Caution à l'origine de cet impôt/taxe — nullable.
     * Renseigné uniquement pour les TVA générées depuis une caution.
     * Utilisé pour bloquer la désactivation d'une caution si sa TVA est en cours (PREVU).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caution_id")
    private Caution caution;

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

    public ImpotEtTaxe() {
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Motif getMotif() {
        return motif;
    }

    public void setMotif(Motif motif) {
        this.motif = motif;
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

    public Commission getCommission() {
        return commission;
    }

    public void setCommission(Commission commission) {
        this.commission = commission;
    }

    public Caution getCaution() {
        return caution;
    }

    public void setCaution(Caution caution) {
        this.caution = caution;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ImpotEtTaxe that)) return false;
        return Objects.equals(id, that.id) && Objects.equals(montant, that.montant) && Objects.equals(banque, that.banque) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(dateValeur, that.dateValeur) && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(libelle, that.libelle) && Objects.equals(observation, that.observation) && motif == that.motif && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, montant, banque, dateEmission, dateValeur, dateOperation, libelle, observation, motif, status);
    }

    @Override
    public String toString() {
        return "ImpotEtTaxe{" +
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

