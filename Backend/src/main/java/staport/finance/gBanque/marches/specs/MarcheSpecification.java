package staport.finance.gBanque.marches.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.marches.dtos.request.MarcheSearchDTO;
import staport.finance.gBanque.marches.entities.Marche;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class MarcheSpecification {

    private static final String FIELD_NOM             = "nom";
    private static final String FIELD_NUMERO           = "numero";
    private static final String FIELD_MAITRE_DOUVRAGE  = "maitreDouvrage";
    private static final String FIELD_BANQUE           = "banque";
    private static final String FIELD_ACTIF            = "actif";
    private static final String FIELD_DATE_DEBUT       = "dateDebutDesTravaux";
    private static final String FIELD_DATE_ACHEVEMENT  = "dateDAchievementDesTravaux";

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

            addDateRange(predicates, cb, root, FIELD_DATE_DEBUT,
                    criteria.dateDebutDesTravauxFrom(), criteria.dateDebutDesTravauxTo());
            addDateRange(predicates, cb, root, FIELD_DATE_ACHEVEMENT,
                    criteria.dateDAchievementDesTravauxFrom(), criteria.dateDAchievementDesTravauxTo());

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Ajoute un prédicat de plage inclusive sur un champ de type date, s'il y a au moins une
     * borne fournie. Les enregistrements dont le champ est {@code null} sont exclus dans ce cas.
     */
    private static void addDateRange(List<Predicate> predicates, jakarta.persistence.criteria.CriteriaBuilder cb,
                                      jakarta.persistence.criteria.Root<Marche> root, String field,
                                      LocalDate from, LocalDate to) {
        if (from == null && to == null) return;
        if (from != null && to != null) {
            predicates.add(cb.between(root.get(field), from, to));
        } else if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get(field), from));
        } else {
            predicates.add(cb.lessThanOrEqualTo(root.get(field), to));
        }
    }
}
