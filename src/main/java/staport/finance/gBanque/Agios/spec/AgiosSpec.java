package staport.finance.gBanque.Agios.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Specifications JPA pour le module Agios.
 * Convention : {@code AgiosSpec.searchByCriteria(AgiosSearchDto)}.
 */
public class AgiosSpec {

    private AgiosSpec() {}

    // ── Entrée principale ─────────────────────────────────────────────────────

    public static Specification<Agios> searchByCriteria(AgiosSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasStatus(dto.status()));
    }

    // ── Filtres unitaires ─────────────────────────────────────────────────────

    public static Specification<Agios> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) return null;
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Agios> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.isBlank()) return null;
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<Agios> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) return null;
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Agios> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) return null;
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Agios> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<Agios> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}
