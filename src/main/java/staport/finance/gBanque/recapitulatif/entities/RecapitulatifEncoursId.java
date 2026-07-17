package staport.finance.gBanque.recapitulatif.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite de {@link RecapitulatifEncoursView} : (source_type, entity_id).
 *
 * <p>Unique par construction — chaque ligne source (Cheque, Lcn, Virement, ...) ne peut
 * satisfaire qu'une seule branche du {@code UNION ALL} de la vue {@code v_recapitulatif_encours},
 * car sa colonne {@code type}/{@code nature_lcn} route exclusivement vers un seul sens
 * (DECAISSEMENT xor ENCAISSEMENT). Un même {@code entity_id} ne peut donc jamais apparaître deux
 * fois sous le même {@code source_type}.</p>
 */
@Embeddable
public class RecapitulatifEncoursId implements Serializable {

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private TransactionSource sourceType;

    @Column(name = "entity_id")
    private Long entityId;

    public RecapitulatifEncoursId() {
    }

    public RecapitulatifEncoursId(TransactionSource sourceType, Long entityId) {
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
        if (!(o instanceof RecapitulatifEncoursId that)) return false;
        return sourceType == that.sourceType && Objects.equals(entityId, that.entityId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceType, entityId);
    }
}
