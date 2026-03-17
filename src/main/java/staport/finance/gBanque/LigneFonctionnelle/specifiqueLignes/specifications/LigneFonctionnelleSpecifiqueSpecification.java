package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.SpecifiqueLineSearchDto;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LigneFonctionnelleSpecifiqueSpecification {

    public static Specification<LigneFonctionnelleSpecifique> searchByCriteria(LigneFonctionnelleSpecifiqueSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDate now = LocalDate.now();

            // Add predicates for the common fields

            // --- Accessing Banque ID through Marche entity ---
            if (criteria.getBanqueId() != null) {
                // Path: LigneFonctionnelleSpecifique -> Marche -> Banque -> id
                predicates.add(criteriaBuilder.equal(
                        root.get("marche").get("banque").get("id"),
                        criteria.getBanqueId()
                ));
            }


            // --- NatureLigneFonctionnelle ID ---
            if (criteria.getNatureLigneFonctionnelleId() != null) {
                // Path: LigneFonctionnelleSpecifique -> NatureLigneFonctionnelle -> id
                predicates.add(criteriaBuilder.equal(
                        root.get("natureLigneFonctionnelle").get("id"),
                        criteria.getNatureLigneFonctionnelleId()
                ));
            }

            // --- Date Depart Year ---
            if (criteria.getYearDepart() != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("YEAR", Integer.class, root.get("dateDepart")),
                        criteria.getYearDepart()
                ));
            }

            // --- Actif Status ---
            if (criteria.getActif() != null) {
                predicates.add(criteriaBuilder.equal(root.get("actif"), criteria.getActif()));
            }
            // --- Expiration Status ---
            if (criteria.getIsExpired() != null) {
                if (criteria.getIsExpired()) {
                    predicates.add(criteriaBuilder.lessThan(root.get("dateFin"), now));
                } else {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateFin"), now));
                }
            }

            // --- Marche ID ---
            if (criteria.getMarcheId() != null) {
                // Path: LigneFonctionnelleSpecifique -> Marche -> id
                predicates.add(criteriaBuilder.equal(
                        root.get("marche").get("id"),
                        criteria.getMarcheId()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<LigneFonctionnelleSpecifique> searchForPrefi(SpecifiqueLineSearchDto criteria) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // --- Accessing Banque ID through Marche entity ---
            if (criteria.getBanqueId() != null) {
                // Path: LigneFonctionnelleSpecifique -> Marche -> Banque -> id
                predicates.add(criteriaBuilder.equal(
                        root.get("marche").get("banque").get("id"),
                        criteria.getBanqueId()
                ));
            }
            // --- Marche ID ---
            if (criteria.getMarcheId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("marche").get("id"),
                        criteria.getMarcheId()
                ));
            }


            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}