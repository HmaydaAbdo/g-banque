package staport.finance.gBanque.moyensDePaiements.recap.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite de {@link RecapMoyenPaiementView} : (source_type, entity_id).
 *
 * <p>Unique par construction — chaque ligne source (Cheque, Lcn, Virement, VirementInternational,
 * MiseADisposition) n'apparaît qu'une seule fois dans le {@code UNION ALL} de la vue
 * {@code v_recap_moyens_paiement}, un {@code entity_id} donné ne pouvant être répété que sous des
 * {@code source_type} différents.</p>
 */
@Embeddable
public class RecapMoyenPaiementId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private TransactionSource sourceType;

    @Column(name = "entity_id")
    private Long entityId;

    public RecapMoyenPaiementId() {
    }

    public RecapMoyenPaiementId(TransactionSource sourceType, Long entityId) {
        this.sourceType = sourceType;
        this.entityId = entityId;
    }

    public TransactionSource getSourceType() {
        return sourceType;
    }

    public Long getEntityId() {
        return entityId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RecapMoyenPaiementId that)) return false;
        return sourceType == that.sourceType && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, entityId);
    }
}
