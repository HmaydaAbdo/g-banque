package staport.finance.gBanque.moyensDePaiements.recap.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.recap.entities.RecapMoyenPaiementView;

import java.math.BigDecimal;

public class RecapMoyenPaiementRepositoryImpl implements RecapMoyenPaiementRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BigDecimal sumMontant(Specification<RecapMoyenPaiementView> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<RecapMoyenPaiementView> root = query.from(RecapMoyenPaiementView.class);

        query.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        return entityManager.createQuery(query).getSingleResult();
    }
}
