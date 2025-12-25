package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.specifications;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;

import java.util.ArrayList;
import java.util.List;

public class NatureLigneFonctionnelleSpecification {

    public static Specification<NatureLigneFonctionnelle> searchByCriteria(NatureLigneFonctionnelleSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by label (case-insensitive, contains)
            if (criteria.getLabel() != null && !criteria.getLabel().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("label")),
                        "%" + criteria.getLabel().toLowerCase() + "%"
                ));
            }

            // Filter by description (case-insensitive, contains)
            if (criteria.getDescription() != null && !criteria.getDescription().trim().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        "%" + criteria.getDescription().toLowerCase() + "%"
                ));
            }

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Return a true predicate if no criteria
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
