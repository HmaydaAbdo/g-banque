package staport.finance.gBanque.cmt.config.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.envers.Audited;

import java.time.OffsetDateTime;

/**
 * Configuration administrable du module CMT.
 * <p>
 * Ligne unique (singleton, {@code id = 1}) portant les paramètres métier auparavant
 * codés en dur dans {@code CmtMetrics} :
 * <ul>
 *   <li>{@code alerteEcheanceJoursCourtTerme} — fenêtre (en jours) de la métrique
 *       "échéances à venir à court terme" ;</li>
 *   <li>{@code alerteEcheanceJoursLongTerme} — fenêtre (en jours) de la métrique
 *       "échéances à venir à long terme".</li>
 * </ul>
 */
@Audited
@Entity
@Table(name = "cmt_config")
public class CmtConfig {

    @Id
    private Long id;

    @NotNull
    @Column(name = "alerte_echeance_jours_court_terme", nullable = false)
    private Integer alerteEcheanceJoursCourtTerme;

    @NotNull
    @Column(name = "alerte_echeance_jours_long_terme", nullable = false)
    private Integer alerteEcheanceJoursLongTerme;

    @Column(name = "date_modification", nullable = false)
    private OffsetDateTime dateModification;

    public CmtConfig() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getAlerteEcheanceJoursCourtTerme() { return alerteEcheanceJoursCourtTerme; }
    public void setAlerteEcheanceJoursCourtTerme(Integer alerteEcheanceJoursCourtTerme) { this.alerteEcheanceJoursCourtTerme = alerteEcheanceJoursCourtTerme; }

    public Integer getAlerteEcheanceJoursLongTerme() { return alerteEcheanceJoursLongTerme; }
    public void setAlerteEcheanceJoursLongTerme(Integer alerteEcheanceJoursLongTerme) { this.alerteEcheanceJoursLongTerme = alerteEcheanceJoursLongTerme; }

    public OffsetDateTime getDateModification() { return dateModification; }
    public void setDateModification(OffsetDateTime dateModification) { this.dateModification = dateModification; }

    @PrePersist
    @PreUpdate
    protected void touch() {
        this.dateModification = OffsetDateTime.now();
    }

    @Override
    public String toString() {
        return "CmtConfig{id=" + id
                + ", alerteEcheanceJoursCourtTerme=" + alerteEcheanceJoursCourtTerme
                + ", alerteEcheanceJoursLongTerme=" + alerteEcheanceJoursLongTerme + '}';
    }
}
