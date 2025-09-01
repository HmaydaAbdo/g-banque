package staport.finance.gBanque.lcn.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.lcn.entities.Lcn;
import java.util.Optional;

@Repository
public interface LcnRepository extends JpaRepository<Lcn, String> , JpaSpecificationExecutor<Lcn> {

    /**
     * Finds an Lcn by its unique numero.
     * @param numero The unique identifier of the Lcn.
     * @return An Optional containing the found Lcn, or an empty Optional if none is found.
     */
    Optional<Lcn> findByNumero(String numero);
}
