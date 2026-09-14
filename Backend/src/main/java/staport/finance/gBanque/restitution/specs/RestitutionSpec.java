package staport.finance.gBanque.restitution.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;
import staport.finance.gBanque.restitution.entities.Restitution;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RestitutionSpec {

    public static Specification<Restitution> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Restitution> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<Restitution> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Restitution> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Restitution> byDateRange(String field, LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut == null && fin == null) {
                return cb.conjunction();
            }
            if (debut != null && fin != null) {
                return cb.between(root.get(field), debut, fin);
            } else if (debut != null) {
                return cb.greaterThanOrEqualTo(root.get(field), debut);
            } else {
                return cb.lessThanOrEqualTo(root.get(field), fin);
            }
        };
    }

    public static Specification<Restitution> byDateEmissionRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateEmission", debut, fin);
    }

    public static Specification<Restitution> byDateOperationRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateOperation", debut, fin);
    }

    public static Specification<Restitution> byDateValeurRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateValeur", debut, fin);
    }

    public static Specification<Restitution> buildSpecification(RestitutionSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasStatus(dto.status()))
                .and(byDateEmissionRange(dto.dateEmissionDebut(), dto.dateEmissionFin()))
                .and(byDateOperationRange(dto.dateOperationDebut(), dto.dateOperationFin()))
                .and(byDateValeurRange(dto.dateValeurDebut(), dto.dateValeurFin()));
    }

    public static Specification<Restitution> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<Restitution> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}
