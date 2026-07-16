package staport.finance.gBanque.chargesFixes.config.specifications;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.chargesFixes.TypeChargeFixe;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigSearchCriteria;
import staport.finance.gBanque.chargesFixes.config.entities.ChargeFixeConfig;

public class ChargeFixeConfigSpecification {

    public static Specification<ChargeFixeConfig> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) return null;
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<ChargeFixeConfig> hasType(TypeChargeFixe type) {
        return (root, query, cb) -> {
            if (type == null) return null;
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<ChargeFixeConfig> libelleOuBanqueContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("libelle")), pattern),
                    cb.like(cb.lower(root.get("banque").get("nom")), pattern)
            );
        };
    }

    public static Specification<ChargeFixeConfig> searchByCriteria(ChargeFixeConfigSearchCriteria criteria) {
        return Specification
                .where(hasBanque(criteria.banqueId()))
                .and(hasType(criteria.type()))
                .and(libelleOuBanqueContains(criteria.search()));
    }
}
