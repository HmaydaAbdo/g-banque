package staport.finance.gBanque.recapitulatif.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class RecapitulatifEncoursRepositoryImpl implements RecapitulatifEncoursRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Map<Type, BigDecimal> sumMontantParSens(Specification<RecapitulatifEncoursView> spec) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<RecapitulatifEncoursView> root = query.from(RecapitulatifEncoursView.class);

        Path<Type> sens = root.get("sens");
        query.multiselect(sens, cb.sum(root.<BigDecimal>get("montant")));

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.groupBy(sens);

        List<Object[]> rows = entityManager.createQuery(query).getResultList();

        Map<Type, BigDecimal> totaux = new EnumMap<>(Type.class);
        for (Object[] row : rows) {
            totaux.put((Type) row[0], (BigDecimal) row[1]);
        }
        return totaux;
    }


}
