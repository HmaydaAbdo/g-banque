package staport.finance.gBanque.leasing.lesOrganismsLeasing.specifications;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;

import java.util.ArrayList;
import java.util.List;

public class OrganismeLeasingSpecification {

    public static Specification<OrganismeLeasing> withFilters(OrganismeLeasingSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getNom() != null && !criteria.getNom().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nom")), "%" + criteria.getNom().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}