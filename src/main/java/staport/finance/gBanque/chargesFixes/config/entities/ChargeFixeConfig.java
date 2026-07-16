package staport.finance.gBanque.chargesFixes.config.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.chargesFixes.TypeChargeFixe;
import staport.finance.gBanque.impot_taxes.enums.Motif;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Définition administrable d'une charge fixe bancaire récurrente (frais bancaire, impôt/taxe,
 * agios ou prélèvement), générée automatiquement chaque mois par
 * {@code staport.finance.gBanque.chargesFixes.ChargesFixesScheduler}.
 *
 * <p>Remplace les définitions auparavant codées en dur dans {@code ChargesFixesRegistry}
 * (violation de {@code conventions/configuration.md} — décision de conformité du 16/07/2026,
 * validée par HMAYDA). Les ~25 charges historiques ont été reprises telles quelles comme données
 * d'amorçage (voir {@code ChargeFixeConfigServiceImpl.ensureSeedData()}), modifiables ensuite
 * exclusivement via l'écran d'administration.
 */
@Audited
@Entity
@Table(
    name = "charge_fixe_config",
    indexes = {
        @Index(name = "idx_charge_fixe_config_banque", columnList = "banque_id")
    }
)
public class ChargeFixeConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Banque à laquelle cette charge fixe est rattachée. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @NotBlank
    @Column(nullable = false, length = 150)
    private String libelle;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal montant;

    /** FRAIS_BANCAIRE | IMPOT_TAXE | AGIOS | PRELEVEMENT — détermine le service cible du scheduler. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TypeChargeFixe type;

    /** Obligatoire si {@code type == IMPOT_TAXE}, sinon toujours null. */
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Motif motif;

    /**
     * Jour d'émission dans le mois : {@code > 0} → jour fixe du mois ; {@code -1} → dernier jour
     * du mois ; {@code -2} → dernier jour du trimestre.
     */
    @Column(name = "jour_emission", nullable = false)
    private int jourEmission;

    /** Si {@code true}, le scheduler ajoute automatiquement la période (mois/trimestre) au libellé généré. */
    @Column(name = "append_periode", nullable = false)
    private boolean appendPeriode;

    /**
     * Si {@code true} (nécessite {@code appendPeriode = true}), la période affichée dans le
     * libellé est celle du mois précédant la date d'émission (charges "M-1" : TVA, IR...).
     */
    @Column(name = "mois_precedent", nullable = false)
    private boolean moisPrecedent;

    public ChargeFixeConfig() {}

    public Long getId() { return id; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

    public String getLibelle() { return libelle; }
    public void setLibelle(String libelle) { this.libelle = libelle; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public TypeChargeFixe getType() { return type; }
    public void setType(TypeChargeFixe type) { this.type = type; }

    public Motif getMotif() { return motif; }
    public void setMotif(Motif motif) { this.motif = motif; }

    public int getJourEmission() { return jourEmission; }
    public void setJourEmission(int jourEmission) { this.jourEmission = jourEmission; }

    public boolean isAppendPeriode() { return appendPeriode; }
    public void setAppendPeriode(boolean appendPeriode) { this.appendPeriode = appendPeriode; }

    public boolean isMoisPrecedent() { return moisPrecedent; }
    public void setMoisPrecedent(boolean moisPrecedent) { this.moisPrecedent = moisPrecedent; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChargeFixeConfig that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "ChargeFixeConfig{id=" + id + ", banque=" + (banque != null ? banque.getNom() : "null")
                + ", libelle='" + libelle + '\'' + ", montant=" + montant + ", type=" + type
                + ", jourEmission=" + jourEmission + '}';
    }
}
