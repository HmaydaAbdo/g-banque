package staport.finance.gBanque.cautions.entities;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.marches.entities.Marche;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité représentant une caution bancaire.
 *
 * <p>Une caution est une garantie émise par une {@link Banque} couvrant les obligations
 * d'un organisme dans le cadre d'un contrat ou d'un {@link Marche} .
 *
 * <p>Règles métier :
 * <ul>
 *   <li>{@code actif = true}  → caution en cours</li>
 *   <li>{@code actif = false} → caution expirée </li>
 *   <li>{@code numeroCaution} est l'identifiant métier unique</li>
 *   <li>{@code marche} est optionnel — une caution peut exister sans marché associé</li>
 * </ul>
 */
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(
    name = "cautions",
    indexes = {
        @Index(name = "idx_caution_numero",    columnList = "numeroCaution"),
        @Index(name = "idx_caution_banque_id", columnList = "banque_id"),
        @Index(name = "idx_caution_nature_id", columnList = "nature_id"),
        @Index(name = "idx_caution_marche_id", columnList = "marche_id"),
        @Index(name = "idx_caution_actif",     columnList = "actif")
    }
)
@NamedEntityGraph(
    name = "Caution.avecAssociations",
    attributeNodes = {
        @NamedAttributeNode("banque"),
        @NamedAttributeNode("nature"),
        @NamedAttributeNode("marche")
    }
)
public class Caution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nature_id", nullable = false)
    private NatureLigneFonctionnelle nature;

    /**
     * Marché associé à la caution — optionnel.
     * Une caution peut exister sans être rattachée à un marché.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "marche_id")
    private Marche marche;

    private LocalDate dateDePrise;

    /** Date de main levée — null tant que la caution est encore active. */
    private LocalDate dateDeMainLevee;

    @Column(unique = true)
    private String numeroCaution;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String designation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String organisme;

    /** Montant de la caution — précision 19 chiffres, 2 décimales. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    /** Taux de commission — précision 19 chiffres, 4 décimales (ex: 1.2500%). */
    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal taux;

    /**
     * Indicateur d'état :
     * {@code true} = caution active, {@code false} = caution expirée.
     * Modifié uniquement via appel API explicite (PATCH toggle-actif).
     */
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean actif = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt;

    // ── Constructeurs ─────────────────────────────────────────────────────────

    public Caution() {}

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public Long getId() { return id; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

    public NatureLigneFonctionnelle getNature() { return nature; }
    public void setNature(NatureLigneFonctionnelle nature) { this.nature = nature; }

    public Marche getMarche() { return marche; }
    public void setMarche(Marche marche) { this.marche = marche; }

    public LocalDate getDateDePrise() { return dateDePrise; }
    public void setDateDePrise(LocalDate dateDePrise) { this.dateDePrise = dateDePrise; }

    public LocalDate getDateDeMainLevee() { return dateDeMainLevee; }
    public void setDateDeMainLevee(LocalDate dateDeMainLevee) { this.dateDeMainLevee = dateDeMainLevee; }

    public String getNumeroCaution() { return numeroCaution; }
    public void setNumeroCaution(String numeroCaution) { this.numeroCaution = numeroCaution; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getOrganisme() { return organisme; }
    public void setOrganisme(String organisme) { this.organisme = organisme; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public BigDecimal getTaux() { return taux; }
    public void setTaux(BigDecimal taux) { this.taux = taux; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }

    public LocalDate getCreatedAt() { return createdAt; }
    public LocalDate getUpdatedAt() { return updatedAt; }

    // ── equals / hashCode (sur identifiant métier) ────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Caution caution)) return false;
        return Objects.equals(numeroCaution, caution.numeroCaution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(numeroCaution);
    }

    @Override
    public String toString() {
        return "Caution{id=" + id +
               ", numeroCaution='" + numeroCaution + '\'' +
               ", organisme='" + organisme + '\'' +
               ", montant=" + montant +
               ", actif=" + actif + '}';
    }
}
