package staport.finance.gBanque.moyensDePaiements.recap.repositories;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementView;

import java.math.BigDecimal;

/**
 * Agrégation (SUM) sur {@link RecapMoyenPaiementView} respectant une {@link Specification}
 * arbitraire — {@code JpaSpecificationExecutor} ne fournit pas d'agrégat natif, et le total doit
 * refléter les mêmes filtres dynamiques que la page retournée.
 */
public interface RecapMoyenPaiementRepositoryCustom {

    BigDecimal sumMontant(Specification<RecapMoyenPaiementView> spec);
}
