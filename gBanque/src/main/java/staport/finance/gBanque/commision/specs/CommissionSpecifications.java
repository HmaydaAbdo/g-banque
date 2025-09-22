package staport.finance.gBanque.commision.specs;


import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.dtos.CommissionSearchDto;

public class CommissionSpecifications {

    public static Specification<Commission> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Commission> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<Commission> hasOperation(Enum<?> operation) {
        return (root, query, cb) -> {
            if (operation == null) {
                return null;
            }
            return cb.equal(root.get("operation"), operation);
        };
    }

    public static Specification<Commission> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Commission> buildSpecification(CommissionSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasOperation(dto.getOperation()))
                .and(hasStatus(dto.getStatus()));
    }
}
