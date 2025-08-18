package staport.finance.gBanque.cmt.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.util.Optional;

@Repository
public interface CreditMoyenTermeRepository extends JpaRepository<CreditMoyenTerme,Long>, JpaSpecificationExecutor<CreditMoyenTerme> {



}
