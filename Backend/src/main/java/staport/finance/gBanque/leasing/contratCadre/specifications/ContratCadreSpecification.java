package staport.finance.gBanque.leasing.contratCadre.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratCadre.dtos.ContratCadreSearchDTO;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;

import java.util.ArrayList;
import java.util.List;

/**
 * Spécifications JPA pour la recherche filtrée des ContratCadre.
 */
public class ContratCadreSpecification {

    private ContratCadreSpecification() {}

    public static Specification<ContratCadre> searchByCriteria(ContratCadreSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.organismeId() != null) {
                predicates.add(cb.equal(root.get("organisme").get("id"), criteria.organismeId()));
            }
            if (criteria.statut() != null) {
                predicates.add(cb.equal(root.get("statut"), criteria.statut()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
