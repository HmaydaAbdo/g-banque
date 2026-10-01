package staport.finance.gBanque.recapitulatif.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.SourceRecapDto;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
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

    @Override
    public List<SourceRecapDto> aggregateBySource(Specification<RecapitulatifEncoursView> spec, LocalDate today) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = cb.createQuery(Object[].class);
        Root<RecapitulatifEncoursView> root = query.from(RecapitulatifEncoursView.class);

        Path<TransactionSource> source = root.get("id").get("sourceType");
        Path<Type> sens = root.get("sens");
        Expression<BigDecimal> montant = root.get("montant");

        Expression<BigDecimal> debit = cb.<BigDecimal>selectCase()
                .when(cb.equal(sens, Type.DECAISSEMENT), montant)
                .otherwise(BigDecimal.ZERO);
        Expression<BigDecimal> credit = cb.<BigDecimal>selectCase()
                .when(cb.equal(sens, Type.ENCAISSEMENT), montant)
                .otherwise(BigDecimal.ZERO);
        Expression<Long> echue = cb.<Long>selectCase()
                .when(cb.lessThanOrEqualTo(root.<LocalDate>get("dateReference"), today), 1L)
                .otherwise(0L);

        query.multiselect(source, cb.count(root), cb.sum(debit), cb.sum(credit), cb.sum(echue));

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.groupBy(source);

        List<SourceRecapDto> result = new ArrayList<>();
        for (Object[] row : entityManager.createQuery(query).getResultList()) {
            result.add(new SourceRecapDto(
                    (TransactionSource) row[0],
                    ((Number) row[1]).longValue(),
                    row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO,
                    row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO,
                    row[4] != null ? ((Number) row[4]).longValue() : 0L));
        }
        return result;
    }
}