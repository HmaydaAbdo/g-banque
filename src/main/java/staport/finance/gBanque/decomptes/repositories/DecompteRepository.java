package staport.finance.gBanque.decomptes.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.decomptes.entities.Decompte;

@Repository
public interface DecompteRepository extends JpaRepository<Decompte, Long>, JpaSpecificationExecutor<Decompte> {
}
