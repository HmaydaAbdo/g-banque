package staport.finance.gBanque.leasing.contratCadre.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.enums.StatutContratCadre;

import java.util.ArrayList;
import java.util.List;

/**
 * Spécifications JPA pour la recherche filtrée des ContratCadre.
 */
public class ContratCadreSpecification {

    private ContratCadreSpecification() {}

    public static Specification<ContratCadre> build(Long organismeId, StatutContratCadre statut) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (organismeId != null) {
                predicates.add(cb.equal(root.get("organisme").get("id"), organismeId));
            }
            if (statut != null) {
                predicates.add(cb.equal(root.get("statut"), statut));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
