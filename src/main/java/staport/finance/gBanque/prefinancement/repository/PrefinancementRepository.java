package staport.finance.gBanque.prefinancement.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;


@Repository
public interface PrefinancementRepository extends JpaRepository <Prefinancement, Long>, JpaSpecificationExecutor<Prefinancement> {
    /**
     * Finds a list of Prefinancement entities by a given Marche and Status.
     * This method leverages JPA's derived query capabilities.
     *
     * @param marche The Marche entity to filter by.
     * @param status The Status enum to filter by (e.g., Status.PAYE).
     * @return A list of Prefinancement entities that match the criteria.
     */
    List<Prefinancement> findByMarcheAndStatus(Marche marche, Status status);

}
