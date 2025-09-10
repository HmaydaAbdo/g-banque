package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class LigneFonctionnelleSpecifiqueSpecification {

    public static Specification<LigneFonctionnelleSpecifique> searchByCriteria(LigneFonctionnelleSpecifiqueSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();

            // Add predicates for the common fields
            if (criteria.getBanqueId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), criteria.getBanqueId()));
            }
            if (criteria.getNatureLigneFonctionnelleId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("natureLigneFonctionnelle").get("id"), criteria.getNatureLigneFonctionnelleId()));
            }
            if (criteria.getYearDepart() != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("YEAR", Integer.class, root.get("dateDepart")),
                        criteria.getYearDepart()
                ));
            }
            if (criteria.getActif() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actif"), criteria.getActif()));
            }
            if (criteria.getIsExpired() != null) {
                if (criteria.getIsExpired()) {
                    predicates.add(criteriaBuilder.lessThan(root.get("dateFin"), now));
                } else {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateFin"), now));
                }
            }

            // Add the specific predicate for the subclass
            if (criteria.getMarcheId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("marche").get("id"), criteria.getMarcheId()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}