package staport.finance.gBanque.transactions.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides reusable specifications for the Transaction entity to support dynamic querying.
 */
public class TransactionSpecifications {



    public static Specification<Transaction> hasRapprochementId(Long rapprochementId) {
        return (root, query, criteriaBuilder) -> {
            if (rapprochementId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("rapprochement").get("id"), rapprochementId);
        };
    }

    public static Specification<Transaction> libelleContains(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("libelle")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Transaction> observationContains(String keyword) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(criteriaBuilder.lower(root.get("observation")), "%" + keyword.toLowerCase() + "%");
    }

    public static Specification<Transaction> hasBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            Join<Transaction, Banque> banqueJoin = root.join("banqueDePrelevement");
            return criteriaBuilder.equal(banqueJoin.get("id"), banqueId);
        };
    }



    public static Specification<Transaction> isConfirmed(boolean confirmed) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isConfirmed"), confirmed);
    }

    public static Specification<Transaction> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return null; // ou criteriaBuilder.conjunction() pour ne rien filtrer
            }

            return criteriaBuilder.or(
                    criteriaBuilder.equal(root.get("debit"), montant),
                    criteriaBuilder.equal(root.get("credit"), montant)
            );
        };
    }



    /**
     * Filters transactions by a list of sources.
     * @param sources The list of transaction source enum values.
     * @return a Specification that matches transactions whose source is in the given list.
     */
    public static Specification<Transaction> hasTransactionSources(List<TransactionSource> sources) {
        return (root, query, criteriaBuilder) -> {
            if (sources == null || sources.isEmpty()) {
                return criteriaBuilder.conjunction(); // no filtering if list is empty
            }
            return root.get("transactionSource").in(sources);
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
