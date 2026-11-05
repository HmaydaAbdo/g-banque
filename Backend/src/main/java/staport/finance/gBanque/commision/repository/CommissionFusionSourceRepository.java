package staport.finance.gBanque.commision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import staport.finance.gBanque.commision.entities.CommissionFusionSource;

import java.time.LocalDate;
import java.util.List;

public interface CommissionFusionSourceRepository extends JpaRepository<CommissionFusionSource, Long> {
    boolean existsByCautionIdAndCommissionFusion_DateEmission(Long cautionId, LocalDate dateEmission);
    boolean existsByCautionIdAndCommissionFusion_DateEmissionBetween(Long cautionId, LocalDate debut, LocalDate fin);
    List<CommissionFusionSource> findByCommissionFusion_Id(Long commissionFusionId);
}
