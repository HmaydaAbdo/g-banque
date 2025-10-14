package staport.finance.gBanque.fraisBancaires.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;

@Repository
public interface FraisBancaireRepository extends JpaRepository <FraisBancaire, Long>, JpaSpecificationExecutor<FraisBancaire> {
}
