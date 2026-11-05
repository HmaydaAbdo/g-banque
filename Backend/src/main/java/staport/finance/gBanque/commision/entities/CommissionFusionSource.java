package staport.finance.gBanque.commision.entities;

import jakarta.persistence.*;
import org.hibernate.envers.Audited;

/** Provenance d'une commission caution absorbée dans une commission fusionnée. */
@Audited
@Entity
@Table(name = "commission_fusion_source", indexes = {
        @Index(name = "idx_commission_fusion_source_caution", columnList = "caution_id"),
        @Index(name = "idx_commission_fusion_source_fusion", columnList = "commission_fusion_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_commission_fusion_source_pair",
        columnNames = {"commission_fusion_id", "source_commission_id"}))
public class CommissionFusionSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commission_fusion_id", nullable = false)
    private Commission commissionFusion;

    @Column(name = "source_commission_id", nullable = false)
    private Long sourceCommissionId;

    @Column(name = "caution_id", nullable = false)
    private Long cautionId;

    protected CommissionFusionSource() {}

    public CommissionFusionSource(Commission commissionFusion, Long sourceCommissionId, Long cautionId) {
        this.commissionFusion = commissionFusion;
        this.sourceCommissionId = sourceCommissionId;
        this.cautionId = cautionId;
    }

    public Long getId() { return id; }
    public Commission getCommissionFusion() { return commissionFusion; }
    public Long getSourceCommissionId() { return sourceCommissionId; }
    public Long getCautionId() { return cautionId; }
}
