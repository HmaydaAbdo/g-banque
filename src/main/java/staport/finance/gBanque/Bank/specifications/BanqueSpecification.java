package staport.finance.gBanque.Bank.specifications;

import jakarta.persistence.criteria.Predicate;
import staport.finance.gBanque.Bank.dtos.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.entities.Banque;
import org.springframework.data.jpa.domain.Specification;
import java.util.ArrayList;
import java.util.List;

public class BanqueSpecification {

    public static Specification<Banque> searchByCriteria(BanqueSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Search by Nom (case-insensitive LIKE)
            if (criteria.nom() != null && !criteria.nom().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nom")), "%" + criteria.nom().toLowerCase() + "%"));
            }

            // Search by NumeroDeCompte (case-insensitive LIKE)
            if (criteria.numeroDeCompte() != null && !criteria.numeroDeCompte().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroDeCompte")), "%" + criteria.numeroDeCompte().toLowerCase() + "%"));
            }

            // If no criteria are provided, return a "true" predicate
            // This ensures findAll without criteria returns all records.
            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction(); // Represents "true" predicate
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }


}