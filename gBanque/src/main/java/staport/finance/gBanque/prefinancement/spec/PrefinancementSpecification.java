package staport.finance.gBanque.prefinancement.spec;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.dtos.PrefinancementSearchDTO;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.enums.Type;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to create JPA Specifications for dynamic queries on the Prefinancement entity.
 * This class uses the Criteria API to build predicates based on search criteria.
 */
public final class PrefinancementSpecification {

    // Private constructor to prevent instantiation
    private PrefinancementSpecification() {
    }

    /**
     * Creates a Specification to filter by a specific Marche ID.
     * This method joins the Prefinancement entity with the Marche entity to query on the related ID.
     *
     * @param marcheId The ID of the Marche to filter by.
     * @return A Specification for the Marche ID.
     */
    public static Specification<Prefinancement> byMarcheId(Long marcheId) {
        return (root, query, cb) -> {
            if (marcheId == null) {
                return cb.conjunction(); // A no-op predicate
            }
            // Join the Prefinancement entity with the Marche entity
            return cb.equal(root.join("marche", JoinType.INNER).get("id"), marcheId);
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
    public static Specification<Prefinancement> byMarcheNumero(String marcheNumero) {
        return (root, query, cb) -> {
            if (marcheNumero == null || marcheNumero.trim().isEmpty()) {
                return cb.conjunction();
            }
            // Join the Prefinancement entity with the Marche entity
            Join<Prefinancement, Marche> marcheJoin = root.join("marche", JoinType.INNER);
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
    public static Specification<Prefinancement> byMarcheName(String marcheName) {
        return (root, query, cb) -> {
            if (marcheName == null || marcheName.trim().isEmpty()) {
                return cb.conjunction();
            }
            // Join the Prefinancement entity with the Marche entity
            Join<Prefinancement, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            // Apply a case-insensitive 'like' predicate on the 'nom' property
            return cb.like(cb.lower(marcheJoin.get("nom")), "%" + marcheName.toLowerCase() + "%");
        };
    }
    public static Specification<Prefinancement> byBanqueId(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return cb.conjunction();
            }

            // Path: Prefinancement -> Marche -> LigneFonctionnelleSpecifique -> Banque
            Join<Prefinancement, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            Join<Marche, LigneFonctionnelleSpecifique> ligneJoin = marcheJoin.join("lignesFonctionnellesSpecifiques", JoinType.INNER);
            Join<LigneFonctionnelleSpecifique, Banque> banqueJoin = ligneJoin.join("banque", JoinType.INNER);

            // Add predicates for the Banque ID
            return cb.equal(banqueJoin.get("id"), banqueId);
        };
    }
    /**
     * Creates a Specification to filter by a specific Type.
     *
     * @param type The Type enum value to filter by.
     * @return A Specification for the Type.
     */
    public static Specification<Prefinancement> byType(Type type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    /**
     * Creates a Specification to filter by a specific Status.
     *
     * @param status The Status enum value to filter by.
     * @return A Specification for the Status.
     */
    public static Specification<Prefinancement> byStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    /**
     * Combines all individual specifications from the search DTO into a single,
     * comprehensive Specification.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @return A combined Specification.
     */
    public static Specification<Prefinancement> bySearchCriteria(PrefinancementSearchDTO searchDTO) {
        if (searchDTO == null) {
            return Specification.where(null);
        }

        return Specification.where(byMarcheId(searchDTO.getMarcheId()))
                .and(byBanqueId(searchDTO.getBanqueId()))
                .and(byType(searchDTO.getType()))
                .and(byStatus(searchDTO.getStatus()));
    }

    /**
     * Creates a Specification to filter for outstanding pre-financing records for the current month.
     * Outstanding is defined as status = PREVU and dateEmission is on or before the end of the current month.
     *
     * @return A Specification for outstanding pre-financing records.
     */
    public static Specification<Prefinancement> outstandingRemboursementForCurrentMonth() {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Condition 1: Filter by type
            // The type must be 'REMBOURSEMENT'
            predicates.add(builder.equal(root.get("type"), Type.REMBOURSEMENT));

            // Condition 2: Filter by status
            // The status must be 'PREVU'
            predicates.add(builder.equal(root.get("status"), Status.PREVU));

            // Condition 3: Filter by date
            // The dateEmission must be less than or equal to the end of the current month
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999999999);

            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            // Combine all three conditions with a logical AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Creates a Specification to filter for outstanding pre-financing records for the current month.
     * Outstanding is defined as status = PREVU and dateEmission is on or before the end of the current month.
     *
     * @return A Specification for outstanding pre-financing records.
     */
    public static Specification<Prefinancement> outstandingDeblocageForCurrentMonth() {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Condition 1: Filter by type
            // The type must be 'DEBLOCAGE'
            predicates.add(builder.equal(root.get("type"), Type.DEBLOCAGE));

            // Condition 2: Filter by status
            // The status must be 'PREVU'
            predicates.add(builder.equal(root.get("status"), Status.PREVU));

            // Condition 3: Filter by date
            // The dateEmission must be less than or equal to the end of the current month
            OffsetDateTime now = OffsetDateTime.now();
            OffsetDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999999999);

            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            // Combine all three conditions with a logical AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
