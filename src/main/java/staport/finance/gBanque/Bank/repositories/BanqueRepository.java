package staport.finance.gBanque.Bank.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import staport.finance.gBanque.Bank.entities.Banque;


public interface BanqueRepository  extends JpaRepository<Banque, Long>, JpaSpecificationExecutor<Banque> {


}
