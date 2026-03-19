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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class to create JPA Specifications for dynamic queries on the Prefinancement entity.
 * This class uses the Criteria API to build predicates based on search criteria.
 */
public final class PrefinancementSpecification {



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

            //  PATH: Prefinancement -> Marche -> Banque
            Join<Prefinancement, Marche> marcheJoin = root.join("marche", JoinType.INNER);
            // The Marche entity has a direct ManyToOne relationship with Banque
            Join<Marche, Banque> banqueJoin = marcheJoin.join("banque", JoinType.INNER);

            // Add predicate for the Banque ID
            return cb.equal(banqueJoin.get("id"), banqueId);
        };
    }

    public static Specification<Prefinancement> byType(Type type) {
        return (root, query, cb) -> {
            if (type == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<Prefinancement> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Prefinancement> byStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Prefinancement> bySearchCriteria(PrefinancementSearchDTO searchDTO) {
        if (searchDTO == null) {
            return Specification.where(null);
        }

        return Specification.where(byMarcheId(searchDTO.getMarcheId()))
                .and(byBanqueId(searchDTO.getBanqueId()))
                .and(byType(searchDTO.getType()))
                .and(byStatus(searchDTO.getStatus()));
    }

    public static Specification<Prefinancement> outstandingRemboursementUntil(LocalDate until) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("type"), Type.REMBOURSEMENT));
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
    public static Specification<Prefinancement> outstandingDeblocageUntil(LocalDate until) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("type"), Type.DEBLOCAGE));
            predicates.add(builder.equal(root.get("status"), Status.PREVU));
            predicates.add(builder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
    public static Specification<Prefinancement> outstandingBetween(LocalDate from, LocalDate until, Type type) {
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
