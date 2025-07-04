package staport.finance.gBanque.LigneFonctionnelle.specifications;

import jakarta.persistence.criteria.Predicate;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class LigneFonctionnelleSpecification {

    public static Specification<LigneFonctionnelle> searchByCriteria(LigneFonctionnelleSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by Banque ID
            if (criteria.getBanqueId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), criteria.getBanqueId()));
            }

            // Search by NatureLigneFonctionnelle ID
            if (criteria.getNatureLigneFonctionnelleId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("natureLigneFonctionnelle").get("id"), criteria.getNatureLigneFonctionnelleId()));
            }

            // Search by Year of dateDepart
            if (criteria.getYearDepart() != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("YEAR", Integer.class, root.get("dateDepart")),
                        criteria.getYearDepart()
                ));
            }

            // NEW: Filter by 'actif' status if provided in criteria
            // If actif is null, it means we don't care about the active status, so don't add a predicate for it.
            // If actif is true/false, filter by that value.
            if (criteria.getActif() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actif"), criteria.getActif()));
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // If no specific criteria, return all records
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Renamed and modified: This specification now applies general criteria and implicitly filters by actif=true
    // The "current" term is now better represented by the 'actif' flag.
    public static Specification<LigneFonctionnelle> searchActiveByCriteria(LigneFonctionnelleSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apply all criteria from the general search
            // We pass the criteria as is, and then explicitly add actif=true if not already set.
            // If criteria.getActif() is already set to false, this would create conflicting predicates.
            // Let's modify this to ensure actif=true if it's "searchActiveByCriteria"
            Specification<LigneFonctionnelle> generalSpec = searchByCriteria(criteria);
            Predicate generalPredicate = generalSpec.toPredicate(root, query, criteriaBuilder);
            if (generalPredicate != null) {
                predicates.add(generalPredicate);
            }

            // Ensure 'actif' is true for "active" lines, unless criteria explicitly requests false
            if (criteria.getActif() == null || criteria.getActif()) { // If not set, or explicitly true
                predicates.add(criteriaBuilder.isTrue(root.get("actif")));
            }


            // The old "dateFin >= OffsetDateTime.now()" condition for "current" is now superseded by 'actif'
            // If you still need "current by date" AND "active by flag", you'd add this back.
            // Based on the request, 'actif' now defines "current".
            // predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateFin"), OffsetDateTime.now()));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}