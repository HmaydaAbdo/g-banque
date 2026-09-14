package staport.finance.gBanque.cmt.config.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import staport.finance.gBanque.cmt.config.entities.CmtConfig;

public interface CmtConfigRepository extends JpaRepository<CmtConfig, Long> {
}
