package staport.finance.gBanque.leasing.config.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import staport.finance.gBanque.leasing.config.entities.LeasingConfig;

public interface LeasingConfigRepository extends JpaRepository<LeasingConfig, Long> {
}
