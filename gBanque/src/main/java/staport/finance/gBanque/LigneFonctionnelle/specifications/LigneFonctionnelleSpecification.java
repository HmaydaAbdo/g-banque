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

            // Changed: Search by NatureLigneFonctionnelle ID
            if (criteria.getNatureLigneFonctionnelleId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("natureLigneFonctionnelle").get("id"), criteria.getNatureLigneFonctionnelleId()));
            }

            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateFin"), OffsetDateTime.now()));

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
