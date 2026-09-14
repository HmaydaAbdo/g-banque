package staport.finance.gBanque.logs.repositories;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.logs.entities.Log;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long>, JpaSpecificationExecutor<Log> {

    @Query("SELECT DISTINCT l.entityName FROM Log l")
    List<String> findDistinctEntityNames();
}