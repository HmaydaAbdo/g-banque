package staport.finance.gBanque.decomptes.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.decomptes.dtos.request.DecompteSearchDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DecompteSpec {

    private DecompteSpec() {
        // Classe utilitaire — pas d'instanciation
    }

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
            Join<Decompte, Marche> marcheJoin = root.join("marche", JoinType.INNER);
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
            Join<Decompte, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            return cb.like(cb.lower(marcheJoin.get("nom")), "%" + marcheName.toLowerCase() + "%");
        };
    }

    /**
     * Filter by Banque d'encaissement ID.
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
     * Filtre générique par plage sur un champ de type date (bornes inclusives).
     * Les enregistrements dont le champ est {@code null} sont exclus dès qu'une borne est fournie.
     */
    private static Specification<Decompte> byDateRange(String field, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get(field), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get(field), from);
            return cb.lessThanOrEqualTo(root.get(field), to);
        };
    }

    /**
     * Filtre par plage de dateValeur.
     * Les enregistrements avec dateValeur null sont exclus si au moins une borne est fournie.
     */
    public static Specification<Decompte> byDateValeurRange(LocalDate from, LocalDate to) {
        return byDateRange("dateValeur", from, to);
    }

    /** Filtre par plage de dateEmission. */
    public static Specification<Decompte> byDateEmissionRange(LocalDate from, LocalDate to) {
        return byDateRange("dateEmission", from, to);
    }

    /** Filtre par plage de dateOperation. */
    public static Specification<Decompte> byDateOperationRange(LocalDate from, LocalDate to) {
        return byDateRange("dateOperation", from, to);
    }

    /**
     * Combine tous les critères de recherche.
     */
    public static Specification<Decompte> searchByCriteria(DecompteSearchDto searchDto) {
        if (searchDto == null) return Specification.where(null);

        Specification<Decompte> spec = Specification.where(null);

        if (searchDto.marcheId() != null)
            spec = spec.and(byMarcheId(searchDto.marcheId()));
        if (searchDto.banqueId() != null)
            spec = spec.and(byBanqueId(searchDto.banqueId()));
        if (searchDto.status() != null)
            spec = spec.and(byStatus(searchDto.status()));

        spec = spec.and(byDateEmissionRange(searchDto.dateEmissionFrom(), searchDto.dateEmissionTo()));
        spec = spec.and(byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()));
        spec = spec.and(byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()));
        spec = spec.and(hasMontant(searchDto.montant()));

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
