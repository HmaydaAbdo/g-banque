package staport.finance.gBanque.cautions.commissions.config.specifications;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigSearchCriteria;
import staport.finance.gBanque.cautions.commissions.config.entities.CautionCommissionConfig;

public class CautionCommissionConfigSpecification {

    public static Specification<CautionCommissionConfig> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) return null;
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<CautionCommissionConfig> banqueContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) return null;
            return cb.like(cb.lower(root.get("banque").get("nom")), "%" + search.toLowerCase() + "%");
        };
    }

    public static Specification<CautionCommissionConfig> searchByCriteria(CautionCommissionConfigSearchCriteria criteria) {
        return Specification
                .where(hasBanque(criteria.banqueId()))
                .and(banqueContains(criteria.search()));
    }
}
