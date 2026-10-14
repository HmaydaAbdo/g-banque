package staport.finance.gBanque.droitConstate.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Droit constaté (déblocage/remboursement) rattaché à un marché, couvert par une ligne
 * fonctionnelle (spécifique ou générale). Contrairement à Préfinancement/Factoring, la banque est
 * un champ direct (indépendant de {@code marche.banque}) : pré-remplie depuis {@code marche.banque}
 * à la création côté UI, mais modifiable indépendamment.
 *
 * <p>{@code marche} et {@code banque} sont déjà {@code @Audited} (sweep Marchés/Bank) — pas de
 * traitement particulier. {@code ligneFonctionnelleSpecifique}/{@code ligneFonctionnelle} ne sont
 * pas auditées : {@code @Audited(targetAuditMode = NOT_AUDITED)} (cf.
 * feedback_envers_audit_relations — {@code @ManyToOne} avec FK propre, pas un
 * {@code @OneToMany(mappedBy)}).
 */
@Entity
@Audited
@Table(
        indexes = {
                @Index(name = "idx_droit_constate_marche_id", columnList = "marche_id"),
                @Index(name = "idx_droit_constate_banque_id", columnList = "banque_id"),
                @Index(name = "idx_droit_constate_status", columnList = "status"),
                @Index(name = "idx_droit_constate_type", columnList = "type"),
                @Index(name = "idx_droit_constate_date_valeur", columnList = "dateValeur")
        }
)
@NamedEntityGraph(
        name = "DroitConstate.avecAssociations",
        attributeNodes = {
                @NamedAttributeNode("marche"),
                @NamedAttributeNode("banque")
        }
)
public class DroitConstate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "marche_id")
    private Marche marche;

    /**
     * Banque directement liée à ce droit constaté.
     * Pré-remplie depuis marche.banque à la création, mais modifiable indépendamment.
     * - Pour un DEBLOCAGE  : banque qui reçoit les fonds (encaissement).
     * - Pour un REMBOURSEMENT : banque qui rembourse (décaissement).
     * Nullable pour compatibilité avec les enregistrements existants (backfill SQL requis).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    /** Ligne spécifique active au moment de la création (null si couverte par une ligne générale). */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_fonctionnelle_specifique_id")
    private LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique;

    /** Ligne générale active au moment de la création (null si couverte par une ligne spécifique). */
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ligne_fonctionnelle_id")
    private LigneFonctionnelle ligneFonctionnelle;

    private LocalDate dateEmission ;

    private BigDecimal montant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private String observation;
    @Column(nullable = false)
    private String libelle;

    @Enumerated(EnumType.STRING)
    private Status status=Status.PREVU;

    private LocalDate dateOperation ;
    private LocalDate dateValeur ;

    public DroitConstate() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Marche getMarche() {
        return marche;
    }

    public void setMarche(Marche marche) {
        this.marche = marche;
    }

    public Banque getBanque() {
        return banque;
    }

    public void setBanque(Banque banque) {
        this.banque = banque;
    }

    public LigneFonctionnelleSpecifique getLigneFonctionnelleSpecifique() {
        return ligneFonctionnelleSpecifique;
    }

    public void setLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifique ligneFonctionnelleSpecifique) {
        this.ligneFonctionnelleSpecifique = ligneFonctionnelleSpecifique;
    }

    public LigneFonctionnelle getLigneFonctionnelle() {
        return ligneFonctionnelle;
    }

    public void setLigneFonctionnelle(LigneFonctionnelle ligneFonctionnelle) {
        this.ligneFonctionnelle = ligneFonctionnelle;
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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDate getDateOperation() {
        return dateOperation;
    }

    public void setDateOperation(LocalDate dateOperation) {
        this.dateOperation = dateOperation;
    }

    public LocalDate getDateValeur() {
        return dateValeur;
    }

    public void setDateValeur(LocalDate dateValeur) {
        this.dateValeur = dateValeur;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DroitConstate that = (DroitConstate) o;
        return Objects.equals(id, that.id) && Objects.equals(marche, that.marche) && Objects.equals(dateEmission, that.dateEmission) && Objects.equals(montant, that.montant) && type == that.type && Objects.equals(observation, that.observation) && status == that.status && Objects.equals(dateOperation, that.dateOperation) && Objects.equals(dateValeur, that.dateValeur);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, marche, dateEmission, montant, type, observation, status, dateOperation, dateValeur);
    }

    @Override
    public String toString() {
        return "DroitConstate{" +
                "id=" + id +
                ", marche=" + marche +
                ", dateEmission=" + dateEmission +
                ", montant=" + montant +
                ", type=" + type +
                ", observation='" + observation + '\'' +
                ", libelle='" + libelle + '\'' +
                ", status=" + status +
                ", dateOperation=" + dateOperation +
                ", dateValeur=" + dateValeur +
                '}';
    }
}
