package staport.finance.gBanque.recapitulatif.repositories;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;

import java.math.BigDecimal;

/**
 * Agrégation (SUM) sur {@link RecapitulatifEncoursView} respectant une {@link Specification}
 * arbitraire — nécessaire car {@code JpaSpecificationExecutor} ne fournit pas d'agrégat natif,
 * et les filtres dynamiques de {@code DecaissementSearchDto} (sources, libellé, observation)
 * doivent être reflétés dans le total, pas seulement dans la page retournée.
 */
public interface RecapitulatifEncoursRepositoryCustom {

    BigDecimal sumMontant(Specification<RecapitulatifEncoursView> spec);
}
