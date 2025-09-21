package staport.finance.gBanque.droitConstate.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.util.List;

@Repository
public interface DroitConstateRepository extends JpaRepository<DroitConstate, Long>, JpaSpecificationExecutor<DroitConstate> {

    /**
     * Finds a list of DroitConstate entities by a given Marche and Status.
     * This method leverages JPA's derived query capabilities.
     *
     * @param marche The Marche entity to filter by.
     * @param status The Status enum to filter by (e.g., Status.PAYE).
     * @return A list of DroitConstate entities that match the criteria.
     */
    List<DroitConstate> findByMarcheAndStatus(Marche marche, Status status);
}
