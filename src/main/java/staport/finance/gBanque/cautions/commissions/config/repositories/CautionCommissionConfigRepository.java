package staport.finance.gBanque.cautions.commissions.config.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.cautions.commissions.config.entities.CautionCommissionConfig;

import java.util.Optional;

@Repository
public interface CautionCommissionConfigRepository extends JpaRepository<CautionCommissionConfig, Long>,
        JpaSpecificationExecutor<CautionCommissionConfig> {

    Optional<CautionCommissionConfig> findByBanque_NomIgnoreCase(String nomBanque);

    boolean existsByBanque_NomIgnoreCase(String nomBanque);

    boolean existsByBanque_IdAndIdNot(Long banqueId, Long id);

    boolean existsByBanque_Id(Long banqueId);
}
