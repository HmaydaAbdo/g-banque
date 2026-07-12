package staport.finance.gBanque.cautions.commissions.config.entities;

import jakarta.persistence.*;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Paramètres de calcul de la commission sur cautions, par banque — administrables via l'UI.
 *
 * <p>Remplace les constantes Java auparavant codées en dur dans chaque
 * {@code CommissionCautionStrategie} (seuil minimum, diviseur du prorata journalier) —
 * voir {@code conventions/configuration.md}, "Commissions — règle spécifique".
 *
 * <p>Le calcul proportionnel lui-même (jours calendaires, mois M vs M+1, prorata
 * entrée/sortie) reste porté par chaque {@code CommissionCautionStrategie} : il s'agit d'une
 * logique métier propre à chaque banque, pas d'un paramètre configurable — seuls le seuil
 * minimum et le diviseur le sont.
 */
@Entity
@Table(
    name = "caution_commission_config",
    indexes = {
        @Index(name = "idx_caution_commission_config_banque", columnList = "banque_id")
    }
)
public class CautionCommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Banque concernée par ces paramètres — une seule configuration par banque. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false, unique = true)
    private Banque banque;

    /** Montant minimum de la commission (HT), en MAD. */
    @Column(name = "montant_minimum", nullable = false, precision = 19, scale = 2)
    private BigDecimal montantMinimum;

    /**
     * Diviseur de la formule de prorata journalier : {@code (montant × taux × jours) / diviseur}.
     * Base conventionnelle bancaire habituelle : 36000 (360 jours × 100 pour convertir le taux
     * en pourcentage vers décimal).
     */
    @Column(name = "diviseur", nullable = false, precision = 19, scale = 2)
    private BigDecimal diviseur;

    public CautionCommissionConfig() {}

    public Long getId() { return id; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

    public BigDecimal getMontantMinimum() { return montantMinimum; }
    public void setMontantMinimum(BigDecimal montantMinimum) { this.montantMinimum = montantMinimum; }

    public BigDecimal getDiviseur() { return diviseur; }
    public void setDiviseur(BigDecimal diviseur) { this.diviseur = diviseur; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CautionCommissionConfig that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "CautionCommissionConfig{id=" + id
                + ", banque=" + (banque != null ? banque.getNom() : "null")
                + ", montantMinimum=" + montantMinimum
                + ", diviseur=" + diviseur + '}';
    }
}
