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

            if (criteria.userName() != null && !criteria.userName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("userName")), "%" + criteria.userName().toLowerCase() + "%"));
            }
            if (criteria.operationType() != null && !criteria.operationType().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("operationType"), criteria.operationType()));
            }
            if (criteria.entityName() != null && !criteria.entityName().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("entityName")), "%" + criteria.entityName().toLowerCase() + "%"));
            }

            if (criteria.timestampStart() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), criteria.timestampStart()));
            }
            if (criteria.timestampEnd() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), criteria.timestampEnd()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}