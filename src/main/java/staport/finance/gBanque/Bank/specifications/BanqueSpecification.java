package staport.finance.gBanque.Bank.specifications;

import jakarta.persistence.criteria.Predicate;
import staport.finance.gBanque.Bank.dtos.request.BanqueSearchCriteria;
import staport.finance.gBanque.Bank.entities.Banque;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BanqueSpecification {

    private static final String FIELD_NOM            = "nom";
    private static final String FIELD_NUMERO_COMPTE  = "numeroDeCompte";

    private BanqueSpecification() {}

    public static Specification<Banque> searchByCriteria(BanqueSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.nom() != null && !criteria.nom().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get(FIELD_NOM)),
                        "%" + criteria.nom().toLowerCase() + "%"));
            }

            if (criteria.numeroDeCompte() != null && !criteria.numeroDeCompte().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get(FIELD_NUMERO_COMPTE)),
                        "%" + criteria.numeroDeCompte().toLowerCase() + "%"));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
