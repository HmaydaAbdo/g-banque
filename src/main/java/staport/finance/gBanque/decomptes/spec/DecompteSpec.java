package staport.finance.gBanque.decomptes.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.decomptes.dto.DecompteSearchDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class DecompteSpec {

    /**
     * Filter by Marche ID.
     */
    public static Specification<Decompte> byMarcheId(Long marcheId) {
        return (root, query, cb) -> {
            if (marcheId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.join("marche").get("id"), marcheId);
        };
    }

    /**
     * Creates a Specification to filter by a specific Marche numero.
     * This method performs a join to the Marche entity and applies a 'like' predicate.
     * The search is case-insensitive.
     *
     * @param marcheNumero The numero of the Marche to filter by.
     * @return A Specification for the Marche numero.
     */
    public static Specification<Decompte> byMarcheNumero(String marcheNumero) {
        return (root, query, cb) -> {
            if (marcheNumero == null || marcheNumero.trim().isEmpty()) {
                return cb.conjunction();
            }
            // Join the Prefinancement entity with the Marche entity
            Join<Decompte, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            // Apply a case-insensitive 'like' predicate on the 'numero' property
            return cb.like(cb.lower(marcheJoin.get("numero")), "%" + marcheNumero.toLowerCase() + "%");
        };
    }

    /**
     * Creates a Specification to filter by a specific Marche name.
     * This method performs a join to the Marche entity and applies a 'like' predicate.
     * The search is case-insensitive.
     *
     * @param marcheName The name of the Marche to filter by.
     * @return A Specification for the Marche name.
     */
    public static Specification<Decompte> byMarcheName(String marcheName) {
        return (root, query, cb) -> {
            if (marcheName == null || marcheName.trim().isEmpty()) {
                return cb.conjunction();
            }
            // Join the Decompte entity with the Marche entity
            Join<Decompte, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            // Apply a case-insensitive 'like' predicate on the 'nom' property
            return cb.like(cb.lower(marcheJoin.get("nom")), "%" + marcheName.toLowerCase() + "%");
        };
    }

    /*
     * Filter by Banque d'encaissement ID.
     * @return A Specification for filtering by banque d'encaissement
     */
    public static Specification<Decompte> byBanqueId(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return cb.conjunction();
            }
            Join<Decompte, Banque> banqueEncaissementJoin = root.join("banqueEncaissement", JoinType.INNER);
            return cb.equal(banqueEncaissementJoin.get("id"), banqueId);
        };
    }

    /**
     * Filter by Status.
     */
    public static Specification<Decompte> byStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }


    public static Specification<Decompte> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> montant == null ? cb.conjunction() : cb.equal(root.get("montant"), montant);
    }

    /**
     * Filtre par plage de dateValeur.
     * Les enregistrements avec dateValeur null sont exclus si au moins une borne est fournie.
     */
    public static Specification<Decompte> byDateValeurRange(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null)
                return cb.between(root.get("dateValeur"), from, to);
            if (from != null)
                return cb.greaterThanOrEqualTo(root.get("dateValeur"), from);
            return cb.lessThanOrEqualTo(root.get("dateValeur"), to);
        };
    }

    /** Filtre par plage de montant (min et/ou max). */
    public static Specification<Decompte> byMontantRange(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return cb.conjunction();
            if (min != null && max != null)
                return cb.between(root.get("montant"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(root.get("montant"), min);
            return cb.lessThanOrEqualTo(root.get("montant"), max);
        };
    }

    /**
     * Combine tous les critères de recherche.
     */
    public static Specification<Decompte> bySearchCriteria(DecompteSearchDto searchDto) {
        if (searchDto == null) return Specification.where(null);

        Specification<Decompte> spec = Specification.where(null);

        if (searchDto.getMarcheId() != null)
            spec = spec.and(byMarcheId(searchDto.getMarcheId()));
        if (searchDto.getBanqueId() != null)
            spec = spec.and(byBanqueId(searchDto.getBanqueId()));
        if (searchDto.getStatus() != null)
            spec = spec.and(byStatus(searchDto.getStatus()));

        spec = spec.and(byDateValeurRange(searchDto.getDateValeurFrom(), searchDto.getDateValeurTo()));
        spec = spec.and(byMontantRange(searchDto.getMontantMin(), searchDto.getMontantMax()));

        return spec;
    }


    public static Specification<Decompte> outstandingUntil(LocalDate until) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }


    public static Specification<Decompte> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
