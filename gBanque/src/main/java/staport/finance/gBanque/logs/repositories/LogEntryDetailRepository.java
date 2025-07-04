package staport.finance.gBanque.logs.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.logs.entities.LogEntryDetail;

@Repository
public interface LogEntryDetailRepository extends JpaRepository<LogEntryDetail, Long> {
    // You can add custom query methods here if needed
}