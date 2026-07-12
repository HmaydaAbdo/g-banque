package staport.finance.gBanque.cautions.commissions.config.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.cautions.commissions.config.entities.CautionCommissionConfig;

import java.util.Optional;

@Repository
public interface CautionCommissionConfigRepository extends JpaRepository<CautionCommissionConfig, Long> {

    Optional<CautionCommissionConfig> findByBanque_NomIgnoreCase(String nomBanque);

    boolean existsByBanque_NomIgnoreCase(String nomBanque);

    boolean existsByBanque_IdAndIdNot(Long banqueId, Long id);

    boolean existsByBanque_Id(Long banqueId);
}
