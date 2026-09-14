package staport.finance.gBanque.LigneFonctionnelle.config.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;

/**
 * Configuration administrable du module LigneFonctionnelle (générale + spécifique).
 * <p>
 * Ligne unique (singleton, {@code id = 1}) portant les paramètres métier auparavant
 * codés en dur via {@code @Value} dans {@code FunctionalLineProcessor} et
 * {@code SpecificFunctionalLineProcessor} :
 * <ul>
 *   <li>{@code alertThresholdMonths} — nombre de mois restants avant expiration en-deçà
 *       duquel une notification d'ALERTE (action immédiate requise) est générée ;</li>
 *   <li>{@code warningThresholdMonths} — nombre de mois restants avant expiration en-deçà
 *       duquel une notification d'AVERTISSEMENT (planifier le renouvellement) est générée.</li>
 * </ul>
 * Ces deux seuils s'appliquent identiquement aux lignes générales et spécifiques (le batch
 * d'expiration des deux domaines partage la même logique de seuils).
 */
@Audited
@Entity
@Table(name = "ligne_fonctionnelle_config")
public class LigneFonctionnelleConfig {

    @Id
    private Long id;

    @NotNull
    @Column(name = "alert_threshold_months", nullable = false)
    private Integer alertThresholdMonths;

    @NotNull
    @Column(name = "warning_threshold_months", nullable = false)
    private Integer warningThresholdMonths;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    public LigneFonctionnelleConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAlertThresholdMonths() { return alertThresholdMonths; }
    public void setAlertThresholdMonths(Integer alertThresholdMonths) { this.alertThresholdMonths = alertThresholdMonths; }

    public Integer getWarningThresholdMonths() { return warningThresholdMonths; }
    public void setWarningThresholdMonths(Integer warningThresholdMonths) { this.warningThresholdMonths = warningThresholdMonths; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.dateModification = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "LigneFonctionnelleConfig{id=" + id
                + ", alertThresholdMonths=" + alertThresholdMonths
                + ", warningThresholdMonths=" + warningThresholdMonths + '}';
    }
}
