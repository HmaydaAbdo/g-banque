package staport.finance.gBanque.commision.specs;


import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.dtos.CommissionSearchDto;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.time.OffsetDateTime;

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

    /**
     * Creates a Specification to filter for commissions that are "outstanding."
     * Outstanding is defined as having a status of PREVU and an emission date that is in the past or present.
     *
     * @return A Specification for outstanding commissions.
     */
    public static Specification<Commission> outstandingCommissions() {
        return (root, query, cb) -> {
            // Condition 1: status is PREVU
            Specification<Commission> hasStatusPrev = hasStatus(Status.PREVU);

            // Condition 2: dateEmission is on or before the current date and time
            Specification<Commission> hasDatePassed = (rootDate, queryDate, cbDate) ->
                    cbDate.lessThanOrEqualTo(rootDate.get("dateEmission"), OffsetDateTime.now());

            // Combine the two conditions with a logical AND
            return Specification.where(hasStatusPrev).and(hasDatePassed).toPredicate(root, query, cb);
        };
    }
}
