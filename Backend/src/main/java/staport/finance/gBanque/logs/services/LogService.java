// LogService.java
package staport.finance.gBanque.logs.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.logs.dtos.LogEntryDetailResponse;
import staport.finance.gBanque.logs.dtos.LogResponse;
import staport.finance.gBanque.logs.dtos.LogSearchCriteria;

import java.util.List;

public interface LogService {

    //  récupérer les logs avec recherche, pagination et tri
    Page<LogResponse> getLogs(LogSearchCriteria criteria, Pageable pageable);

    void deleteLogById(Long id);

    List<String> getEntityNames();

    /**
     * Détail champ-par-champ d'une ligne {@code Log}. Si {@code logDetails} a été
     * stocké à l'écriture (anciens aspects par entité), le renvoie directement ;
     * sinon (nouvel aspect générique, Envers), le reconstruit à la demande via
     * {@code EnversDiffService} à partir de {@code revisionNumber}.
     */
    List<LogEntryDetailResponse> getAuditDetail(Long logId);
}