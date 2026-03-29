package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.specifications;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import java.util.ArrayList;
import java.util.List;

public class NatureLigneFonctionnelleSpecification {

    public static Specification<NatureLigneFonctionnelle> searchByCriteria(NatureLigneFonctionnelleSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria == null) {
                return cb.conjunction();
            }

            // Filter by label (case-insensitive, contains)
            if (criteria.label() != null && !criteria.label().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("label")),
                        "%" + criteria.label().toLowerCase() + "%"
                ));
            }

            // Filter by description (case-insensitive, contains)
            if (criteria.description() != null && !criteria.description().isBlank()) {
                predicates.add(cb.like(
                        cb.lower(root.get("description")),
                        "%" + criteria.description().toLowerCase() + "%"
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }}
