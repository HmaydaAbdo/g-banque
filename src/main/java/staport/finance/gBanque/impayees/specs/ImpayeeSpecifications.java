package staport.finance.gBanque.impayees.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.impayees.dtos.ImpayeeSearchDto;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class ImpayeeSpecifications {

    public static Specification<Impayee> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banqueEncaissement").get("id"), banqueId);
        };
    }

    public static Specification<Impayee> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<Impayee> hasMotif(Enum<?> motif) {
        return (root, query, cb) -> {
            if (motif == null) {
                return null;
            }
            return cb.equal(root.get("motif"), motif);
        };
    }

    public static Specification<Impayee> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Impayee> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Impayee> buildSpecification(ImpayeeSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueEncaissementId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasMotif(dto.getMotif()))
                .and(hasStatus(dto.getStatus()));
    }

    public static Specification<Impayee> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<Impayee> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}