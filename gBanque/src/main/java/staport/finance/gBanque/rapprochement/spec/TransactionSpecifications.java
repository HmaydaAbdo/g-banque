package staport.finance.gBanque.rapprochement.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.rapprochement.entities.Transaction;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides reusable specifications for the Transaction entity to support dynamic querying.
 */
public class TransactionSpecifications {

    public static Specification<Transaction> hasDateEmissionBetween(OffsetDateTime startDate, OffsetDateTime endDate) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("dateEmission"), startDate, endDate);
    }

    public static Specification<Transaction> libelleContains(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("libelle")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Transaction> hasBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            Join<Transaction, Banque> banqueJoin = root.join("banqueDePrelevement");
            return criteriaBuilder.equal(banqueJoin.get("id"), banqueId);
        };
    }

    /**
     * Filters transactions to only include those that occurred in the current month.
     * @return a Specification for the current month.
     */
    public static Specification<Transaction> isCurrentMonth() {
        return (root, query, criteriaBuilder) -> {
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime startOfMonth = now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay().atOffset(now.getOffset());
            OffsetDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth()).toLocalDate().atTime(23, 59, 59).atOffset(now.getOffset());
            return criteriaBuilder.between(root.get("dateEmission"), startOfMonth, endOfMonth);
        };
    }

    /**
     * Combines multiple specifications with an 'AND' clause.
     * @param specifications a list of Specifications to combine
     * @return a single Specification representing the combined criteria.
     */
    public static Specification<Transaction> withFilters(List<Specification<Transaction>> specifications) {
        return (root, query, criteriaBuilder) -> {
            if (specifications.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            for (Specification<Transaction> spec : specifications) {
                predicates.add(spec.toPredicate(root, query, criteriaBuilder));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
