package staport.finance.gBanque.commision.specifications;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.commision.dtos.CommissionConfigSearchCriteria;
import staport.finance.gBanque.commision.entities.CommissionConfig;
import staport.finance.gBanque.commision.enums.CommissionCondition;
import staport.finance.gBanque.transactions.enums.TransactionSource;

public class CommissionConfigSpecification {

    public static Specification<CommissionConfig> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) return null;
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<CommissionConfig> hasType(TransactionSource type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<CommissionConfig> hasCondition(CommissionCondition condition) {
        return (root, query, cb) -> {
            if (condition == null) return null;
            return cb.equal(root.get("condition"), condition);
        };
    }

    public static Specification<CommissionConfig> organismeOuBanqueContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("banque").get("nom")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("organismeNom"), "")), pattern)
            );
        };
    }

    public static Specification<CommissionConfig> searchByCriteria(CommissionConfigSearchCriteria criteria) {
        return Specification
                .where(hasBanque(criteria.banqueId()))
                .and(hasType(criteria.type()))
                .and(hasCondition(criteria.condition()))
                .and(organismeOuBanqueContains(criteria.search()));
    }
}
