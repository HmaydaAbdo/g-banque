package staport.finance.gBanque.leasing.contratLeasing.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;

import java.util.Optional;

@Repository
public interface ContratLeasingRepository extends JpaRepository<ContratLeasing, Long>, JpaSpecificationExecutor<ContratLeasing> {
    Optional<ContratLeasing> findByNumeroContrat(String numeroContrat);
    boolean existsByNumeroContrat(String numeroContrat);
}