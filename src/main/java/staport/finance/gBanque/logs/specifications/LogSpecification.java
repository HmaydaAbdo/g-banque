package staport.finance.gBanque.logs.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.logs.dtos.LogSearchCriteria;
import staport.finance.gBanque.logs.entities.Log;

import java.util.ArrayList;
import java.util.List;

public class LogSpecification {

    public static Specification<Log> searchByCriteria(LogSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getUserName() != null && !criteria.getUserName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("userName")), "%" + criteria.getUserName().toLowerCase() + "%"));
            }
            if (criteria.getOperationType() != null && !criteria.getOperationType().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("operationType"), criteria.getOperationType()));
            }
            if (criteria.getEntityName() != null && !criteria.getEntityName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("entityName")), "%" + criteria.getEntityName().toLowerCase() + "%"));
            }

            if (criteria.getTimestampStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), criteria.getTimestampStart()));
            }
            if (criteria.getTimestampEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), criteria.getTimestampEnd()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}