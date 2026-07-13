package staport.finance.gBanque.marches.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.marches.dtos.request.MarcheSearchDTO;
import staport.finance.gBanque.marches.entities.Marche;

import java.util.ArrayList;
import java.util.List;

public final class MarcheSpecification {

    private static final String FIELD_NOM             = "nom";
    private static final String FIELD_NUMERO           = "numero";
    private static final String FIELD_MAITRE_DOUVRAGE  = "maitreDouvrage";
    private static final String FIELD_BANQUE           = "banque";
    private static final String FIELD_ACTIF            = "actif";

    private MarcheSpecification() {
    }

    public static Specification<Marche> searchByCriteria(MarcheSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.nom() != null && !criteria.nom().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get(FIELD_NOM)),
                        "%" + criteria.nom().toLowerCase() + "%"));
            }

            if (criteria.numero() != null && !criteria.numero().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get(FIELD_NUMERO)),
                        "%" + criteria.numero().toLowerCase() + "%"));
            }

            if (criteria.maitreDouvrage() != null && !criteria.maitreDouvrage().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get(FIELD_MAITRE_DOUVRAGE)),
                        "%" + criteria.maitreDouvrage().toLowerCase() + "%"));
            }

            if (criteria.banqueId() != null) {
                predicates.add(cb.equal(root.get(FIELD_BANQUE).get("id"), criteria.banqueId()));
            }

            if (criteria.actif() != null) {
                predicates.add(cb.equal(root.get(FIELD_ACTIF), criteria.actif()));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
