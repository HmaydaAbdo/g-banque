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
                .and(hasStatus(dto.status()))
                .and(byDateEmissionRange(dto.dateEmissionDebut(), dto.dateEmissionFin()))
                .and(byDateOperationRange(dto.dateOperationDebut(), dto.dateOperationFin()))
                .and(byDateValeurRange(dto.dateValeurDebut(), dto.dateValeurFin()));
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

    /** Filtre générique par plage de dates sur un champ {@link LocalDate} de l'entité. */
    private static Specification<Agios> byDateRange(String field, LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut != null && fin != null) return cb.between(root.get(field), debut, fin);
            if (debut != null) return cb.greaterThanOrEqualTo(root.get(field), debut);
            if (fin != null) return cb.lessThanOrEqualTo(root.get(field), fin);
            return null;
        };
    }

    public static Specification<Agios> byDateEmissionRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateEmission", debut, fin);
    }

    public static Specification<Agios> byDateOperationRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateOperation", debut, fin);
    }

    public static Specification<Agios> byDateValeurRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateValeur", debut, fin);
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
