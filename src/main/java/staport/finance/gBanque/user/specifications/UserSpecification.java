package staport.finance.gBanque.user.specifications;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.user.dtos.UserSearchCriteria;
import staport.finance.gBanque.user.entities.User;
import staport.finance.gBanque.user.entities.UserDepartmentRole;

import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchByCriteria(UserSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.username() != null && !criteria.username().isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("username")),
                        "%" + criteria.username().toLowerCase() + "%"));
            }

            if (criteria.enabled() != null) {
                predicates.add(criteriaBuilder.equal(root.get("enabled"), criteria.enabled()));
            }

            if (criteria.systemAdmin() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isSystemAdmin"), criteria.systemAdmin()));
            }

            if (criteria.departmentId() != null) {
                Join<User, UserDepartmentRole> departmentRoleJoin = root.join("departmentRoles");
                predicates.add(criteriaBuilder.equal(
                        departmentRoleJoin.get("department").get("id"), criteria.departmentId()));
                query.distinct(true);
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
