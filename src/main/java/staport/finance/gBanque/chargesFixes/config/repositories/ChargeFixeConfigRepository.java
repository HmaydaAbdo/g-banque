package staport.finance.gBanque.chargesFixes.config.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import staport.finance.gBanque.chargesFixes.config.entities.ChargeFixeConfig;

public interface ChargeFixeConfigRepository extends JpaRepository<ChargeFixeConfig, Long>,
        JpaSpecificationExecutor<ChargeFixeConfig> {
}
