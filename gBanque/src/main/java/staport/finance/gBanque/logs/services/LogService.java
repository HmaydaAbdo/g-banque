// LogService.java
package staport.finance.gBanque.logs.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.dtos.LogSearchCriteria;
import staport.finance.gBanque.logs.entities.Log;
import staport.finance.gBanque.logs.entities.LogEntryDetail;

import java.util.List;

public interface LogService {
    void saveLogWithDetails(Log log, List<LogEntryDetail> details);

    //  récupérer les logs avec recherche, pagination et tri
    Page<LogResponse> getLogs(LogSearchCriteria criteria, Pageable pageable);

    void deleteLogById(Long id);

    List<String> getEntityNames();
}