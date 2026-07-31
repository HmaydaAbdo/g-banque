package staport.finance.gBanque.moyensDePaiements.recap.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementId;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementView;

public interface RecapMoyenPaiementRepository
        extends JpaRepository<RecapMoyenPaiementView, RecapMoyenPaiementId>,
        JpaSpecificationExecutor<RecapMoyenPaiementView>,
        RecapMoyenPaiementRepositoryCustom {
}
