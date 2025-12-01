package staport.finance.gBanque.miseADisposition.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;

import java.util.Optional;

@Repository
public interface MiseADispositionRepository extends JpaRepository<MiseADisposition, Long>, JpaSpecificationExecutor<MiseADisposition> {

    Optional<MiseADisposition> findByNumero(String numero);
}
