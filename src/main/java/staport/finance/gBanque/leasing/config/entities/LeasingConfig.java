package staport.finance.gBanque.leasing.config.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Configuration administrable du module leasing.
 * <p>
 * Ligne unique (singleton, {@code id = 1}) portant les paramètres métier auparavant
 * codés en dur dans {@code application.yml} ({@code leasing.contract.*}) :
 * <ul>
 *   <li>{@code valeurResiduelle} — taux appliqué au montant d'achat TTC pour calculer
 *       la valeur résiduelle d'un contrat de leasing ;</li>
 *   <li>{@code tauxTva} — taux de TVA utilisé pour ventiler les échéances (montant HT / TVA).</li>
 * </ul>
 */
@Audited
@Entity
@Table(name = "leasing_config")
public class LeasingConfig {

    @Id
    private Long id;

    @NotNull
    @Column(name = "valeur_residuelle", nullable = false, precision = 6, scale = 4)
    private BigDecimal valeurResiduelle;

    @NotNull
    @Column(name = "taux_tva", nullable = false, precision = 6, scale = 4)
    private BigDecimal tauxTva;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    public LeasingConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getValeurResiduelle() { return valeurResiduelle; }
    public void setValeurResiduelle(BigDecimal valeurResiduelle) { this.valeurResiduelle = valeurResiduelle; }

    public BigDecimal getTauxTva() { return tauxTva; }
    public void setTauxTva(BigDecimal tauxTva) { this.tauxTva = tauxTva; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.dateModification = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "LeasingConfig{id=" + id + ", valeurResiduelle=" + valeurResiduelle + ", tauxTva=" + tauxTva + '}';
    }
}
