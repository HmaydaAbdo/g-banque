package staport.finance.gBanque.logs.repositories;


import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph; // Importez EntityGraph
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Importez JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.logs.entities.Log;

import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Long>, JpaSpecificationExecutor<Log> {



    // Cette annotation indique à JPA/Hibernate de récupérer également la collection 'logDetails'
    // avec le Log principal en une seule requête (JOIN FETCH), évitant ainsi le problème N+1.
    // 'attributePaths' doit correspondre au nom de la propriété dans l'entité Log.
    @EntityGraph(attributePaths = "logDetails")
    Page<Log> findAll(org.springframework.data.jpa.domain.Specification<Log> spec, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT DISTINCT l.entityName FROM Log l")
    List<String> findDistinctEntityNames();
}