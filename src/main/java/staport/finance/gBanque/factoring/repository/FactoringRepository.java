package staport.finance.gBanque.factoring.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.factoring.entities.Factoring;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;

@Repository
public interface FactoringRepository extends JpaRepository<Factoring,Long> , JpaSpecificationExecutor<Factoring> {

    /**
     * Finds a list of Factoring entities by a given Marche and Status.
     * This method leverages JPA's derived query capabilities.
     *
     * @param marche The Marche entity to filter by.
     * @param status The Status enum to filter by (e.g., Status.PAYE).
     * @return A list of Factoring entities that match the criteria.
     */
    List<Factoring> findByMarcheAndStatus(Marche marche, Status status);

}
