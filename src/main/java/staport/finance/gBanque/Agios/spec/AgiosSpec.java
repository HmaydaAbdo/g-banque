package staport.finance.gBanque.Agios.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;

public class AgiosSpec {

    public static Specification<Agios> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Agios> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }


    public static Specification<Agios> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Agios> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }


    public static Specification<Agios> buildSpecification(AgiosSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasStatus(dto.getStatus()));
    }

    /**
     * Creates a Specification to filter for agios that are "outstanding."
     * Outstanding is defined as having a status of PREVU and an emission date that is in the past or present.
     *
     * @return A Specification for outstanding agios.
     */
    public static Specification<Agios> outstandingAgiosByPeriod(YearMonth period) {
        return (root, query, cb) -> {

            OffsetDateTime endOfPeriod = period.atEndOfMonth()
                    .atTime(23, 59, 59, 999_999_999)
                    .atOffset(java.time.ZoneOffset.UTC);

            Specification<Agios> hasStatusPrevu = hasStatus(Status.PREVU);

            Specification<Agios> hasDateBeforeEndOfPeriod = (r, q, crit) ->
                    crit.lessThanOrEqualTo(r.get("dateEmission"), endOfPeriod);

            return Specification.where(hasStatusPrevu)
                    .and(hasDateBeforeEndOfPeriod)
                    .toPredicate(root, query, cb);
        };
    }
}
