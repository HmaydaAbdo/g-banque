package staport.finance.gBanque.commision.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.commision.enums.CommissionCondition;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Règle de commission configurable par un administrateur.
 *
 * <p>Remplace les valeurs codées en dur dans {@code application.yml}
 * ({@code commission.leasing.*} et {@code commission.virement.bmci.montant-fixe}).
 *
 * <p>Logique de résolution dans {@code CommissionConfigService.resolveCommission()} :
 * <ol>
 *   <li>Chercher une règle {@code SI_MATCH} pour la banque + organisme → montant si trouvé.</li>
 *   <li>Si une règle {@code SI_MATCH} existe pour la banque mais l'organisme ne correspond pas
 *       → chercher {@code SI_NON_MATCH} pour la même banque.</li>
 *   <li>Sinon → chercher {@code DEFAULT} pour la banque.</li>
 * </ol>
 *
 * <p>{@code @Audited} : cette table ne porte pas un résultat de calcul mais la
 * <em>règle</em> qui le produit. Une modification de montant change tout ce qui sera
 * commissionné ensuite, sans qu'aucune commission existante ne bouge — sans historique
 * ici, un écart constaté après coup serait inexplicable.
 */
@Entity
@Audited
@Table(
    name = "commission_config",
    indexes = {
        @Index(name = "idx_commission_config_banque_type", columnList = "banque_id, type"),
        @Index(name = "idx_commission_config_condition", columnList = "commission_condition")
    }
)
public class CommissionConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Banque concernée par la règle. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    /**
     * Nom de l'organisme de référence.
     * Renseigné uniquement pour les conditions {@code SI_MATCH} / {@code SI_NON_MATCH}.
     * Null pour {@code DEFAULT}.
     */
    @Column(name = "organisme_nom", length = 150)
    private String organismeNom;

    /** Montant fixe de la commission (en MAD). */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal montant;

    /** Source de transaction concernée (LEASING, Virement, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TransactionSource type;

    /** Condition d'application de la règle. */
    @Enumerated(EnumType.STRING)
    @Column(name = "commission_condition", nullable = false, length = 20)
    private CommissionCondition condition;

    public CommissionConfig() {}

    public Long getId() { return id; }

    public Banque getBanque() { return banque; }
    public void setBanque(Banque banque) { this.banque = banque; }

    public String getOrganismeNom() { return organismeNom; }
    public void setOrganismeNom(String organismeNom) { this.organismeNom = organismeNom; }

    public BigDecimal getMontant() { return montant; }
    public void setMontant(BigDecimal montant) { this.montant = montant; }

    public TransactionSource getType() { return type; }
    public void setType(TransactionSource type) { this.type = type; }

    public CommissionCondition getCondition() { return condition; }
    public void setCondition(CommissionCondition condition) { this.condition = condition; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommissionConfig that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return "CommissionConfig{id=" + id + ", banque=" + (banque != null ? banque.getNom() : "null")
                + ", type=" + type + ", condition=" + condition + ", montant=" + montant + '}';
    }
}
