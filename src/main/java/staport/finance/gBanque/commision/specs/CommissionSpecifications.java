package staport.finance.gBanque.commision.specs;


import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.dtos.CommissionSearchDto;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

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

    public static Specification<Commission> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    /**
     * Filtre par nature de ligne fonctionnelle — via la caution d'origine de la commission
     * (commission.caution.nature.id). Les commissions sans caution associée (ex : virements)
     * sont exclues dès qu'un {@code natureId} est fourni, la jointure vers {@code caution}
     * n'ayant alors pas de correspondance.
     */
    public static Specification<Commission> hasNature(Long natureId) {
        return (root, query, cb) -> {
            if (natureId == null) {
                return null;
            }
            return cb.equal(root.get("caution").get("nature").get("id"), natureId);
        };
    }

    public static Specification<Commission> buildSpecification(CommissionSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasOperation(dto.operation()))
                .and(hasStatus(dto.status()))
                .and(hasMontant(dto.montant()))
                .and(hasNature(dto.natureId()));
    }

    public static Specification<Commission> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<Commission> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

}
