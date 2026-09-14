package staport.finance.gBanque.droitConstate.spec;


import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.droitConstate.dtos.request.DroitConstateSearchDto;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to create JPA Specifications for dynamic queries on the DroitConstate entity.
 * This class uses the Criteria API to build predicates based on search criteria.
 */

public class DroitConstateSpec {


    public DroitConstateSpec() {
    }

    /**
     * Creates a Specification to filter by a specific Marche numero.
     * This method performs a join to the Marche entity and applies a 'like' predicate.
     * The search is case-insensitive.
     *
     * @param marcheNumero The numero of the Marche to filter by.
     * @return A Specification for the Marche numero.
     */
    public static Specification<DroitConstate> byMarcheNumero(String marcheNumero) {
        return (root, query, cb) -> {
            if (marcheNumero == null || marcheNumero.trim().isEmpty()) {
                return cb.conjunction();
            }
            // Join the Prefinancement entity with the Marche entity
            Join<DroitConstate, Marche> marcheJoin = root.join("marche", JoinType.INNER);
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
    public static Specification<DroitConstate> byMarcheName(String marcheName) {
        return (root, query, cb) -> {
            if (marcheName == null || marcheName.trim().isEmpty()) {
                return cb.conjunction();
            }
            // Join the Prefinancement entity with the Marche entity
            Join<DroitConstate, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            // Apply a case-insensitive 'like' predicate on the 'nom' property
            return cb.like(cb.lower(marcheJoin.get("nom")), "%" + marcheName.toLowerCase() + "%");
        };
    }
    /**
     * Creates a Specification to filter by a specific Marche ID.
     * This method joins the DroitConstate entity with the Marche entity to query on the related ID.
     *
     * @param marcheId The ID of the Marche to filter by.
     * @return A Specification for the Marche ID.
     */
    public static Specification<DroitConstate> byMarcheId(Long marcheId) {
        return (root, query, cb) -> {
            if (marcheId == null) {
                return cb.conjunction(); // A no-op predicate
            }
            // Join the Prefinancement entity with the Marche entity
            return cb.equal(root.join("marche", JoinType.INNER).get("id"), marcheId);
        };
    }

    /**
     * Filtre par la banque directement liée au DroitConstate (champ banque_id).
     * Tous les DCs ont une banque_id après backfill — chemin direct uniquement.
     *
     * PATH : DroitConstate.banque.id
     */
    public static Specification<DroitConstate> byBanqueId(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return cb.conjunction();
            }
            Join<DroitConstate, Banque> banqueJoin = root.join("banque", JoinType.INNER);
            return cb.equal(banqueJoin.get("id"), banqueId);
        };
    }

    /**
     * Creates a Specification to filter by a specific Type.
     *
     * @param type The Type enum value to filter by.
     * @return A Specification for the Type.
     */
    public static Specification<DroitConstate> byType(Type type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<DroitConstate> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    /**
     * Creates a Specification to filter by a specific Status.
     *
     * @param status The Status enum value to filter by.
     * @return A Specification for the Status.
     */
    public static Specification<DroitConstate> byStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * Filtre générique par plage sur un champ de type date (bornes inclusives).
     * Les enregistrements dont le champ est {@code null} sont exclus dès qu'une borne est fournie.
     */
    private static Specification<DroitConstate> byDateRange(String field, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get(field), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get(field), from);
            return cb.lessThanOrEqualTo(root.get(field), to);
        };
    }

    /** Filtre par plage de dateEmission. */
    public static Specification<DroitConstate> byDateEmissionRange(LocalDate from, LocalDate to) {
        return byDateRange("dateEmission", from, to);
    }

    /** Filtre par plage de dateOperation. */
    public static Specification<DroitConstate> byDateOperationRange(LocalDate from, LocalDate to) {
        return byDateRange("dateOperation", from, to);
    }

    /** Filtre par plage de dateValeur. */
    public static Specification<DroitConstate> byDateValeurRange(LocalDate from, LocalDate to) {
        return byDateRange("dateValeur", from, to);
    }


    /**
     * Combines all individual specifications from the search DTO into a single,
     * comprehensive Specification.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @return A combined Specification.
     */
    public static Specification<DroitConstate> searchByCriteria(DroitConstateSearchDto searchDTO) {
        if (searchDTO == null) {
            return Specification.where(null);
        }

        return Specification.where(byMarcheId(searchDTO.marcheId()))
                .and(byBanqueId(searchDTO.banqueId()))
                .and(byType(searchDTO.type()))
                .and(byStatus(searchDTO.status()))
                .and(byDateEmissionRange(searchDTO.dateEmissionFrom(), searchDTO.dateEmissionTo()))
                .and(byDateOperationRange(searchDTO.dateOperationFrom(), searchDTO.dateOperationTo()))
                .and(byDateValeurRange(searchDTO.dateValeurFrom(), searchDTO.dateValeurTo()));
    }

    public static Specification<DroitConstate> outstandingRemboursementUntil(LocalDate until) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("type"), Type.REMBOURSEMENT));
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<DroitConstate> outstandingDeblocageUntil(LocalDate until) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("type"), Type.DEBLOCAGE));
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }


    public static Specification<DroitConstate> outstandingBetween(LocalDate from, LocalDate until, Type type) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("type"), type));
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
