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
            if (criteria.getNom() != null && !criteria.getNom().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nom")), "%" + criteria.getNom().toLowerCase() + "%"));
            }

            // Search by NumeroDeCompte (case-insensitive LIKE)
            if (criteria.getNumeroDeCompte() != null && !criteria.getNumeroDeCompte().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroDeCompte")), "%" + criteria.getNumeroDeCompte().toLowerCase() + "%"));
            }

            // Search by Solde Actuel range
            if (criteria.getMinSolde() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("soldeActuel"), criteria.getMinSolde()));
            }
            if (criteria.getMaxSolde() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("soldeActuel"), criteria.getMaxSolde()));
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