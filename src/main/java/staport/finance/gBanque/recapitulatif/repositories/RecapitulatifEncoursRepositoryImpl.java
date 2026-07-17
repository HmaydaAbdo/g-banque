package staport.finance.gBanque.recapitulatif.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;

import java.math.BigDecimal;

public class RecapitulatifEncoursRepositoryImpl implements RecapitulatifEncoursRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public BigDecimal sumMontant(Specification<RecapitulatifEncoursView> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<BigDecimal> query = cb.createQuery(BigDecimal.class);
        Root<RecapitulatifEncoursView> root = query.from(RecapitulatifEncoursView.class);

        query.select(cb.coalesce(cb.sum(root.get("montant")), BigDecimal.ZERO));

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        return entityManager.createQuery(query).getSingleResult();
    }
}
