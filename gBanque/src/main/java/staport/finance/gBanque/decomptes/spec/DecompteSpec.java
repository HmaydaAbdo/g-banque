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
import java.time.OffsetDateTime;
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

    /**
     * Filter by Banque ID (direct relation).
     */
    public static Specification<Decompte> byBanqueId(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return cb.conjunction();
            }
            Join<Decompte, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            Join<Marche, Banque> banqueJoin = marcheJoin.join("banque", JoinType.INNER);
            return cb.equal(banqueJoin.get("id"), banqueId);
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

    /**
     * Combine multiple search criteria using DecompteSearchDto.
     * Each field is tested for null before applying the filter.
     */
    public static Specification<Decompte> bySearchCriteria(DecompteSearchDto searchDto) {
        if (searchDto == null) {
            return Specification.where(null); // aucun filtre
        }

        Specification<Decompte> spec = Specification.where(null);

        if (searchDto.getMarcheId() != null) {
            spec = spec.and(byMarcheId(searchDto.getMarcheId()));
        }
        if (searchDto.getBanqueId() != null) {
            spec = spec.and(byBanqueId(searchDto.getBanqueId()));
        }
        if (searchDto.getStatus() != null) {
            spec = spec.and(byStatus(searchDto.getStatus()));
        }

        return spec;
    }

    /**
     * Creates a Specification to filter for outstanding décomptes records for the current month.
     * Outstanding is defined as status = PREVU and dateEmission is on or before the end of the current month.
     *
     * @return A Specification for outstanding décomptes records.
     */
    public static Specification<Decompte> outstandingDecomptesForCurrentMonth() {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Condition 1: Filter by status
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
