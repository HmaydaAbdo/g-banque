    package staport.finance.gBanque.LigneFonctionnelle.generalLignes.specifications;

    import jakarta.persistence.criteria.CriteriaBuilder;
    import jakarta.persistence.criteria.CriteriaQuery;
    import jakarta.persistence.criteria.Predicate;
    import jakarta.persistence.criteria.Root;
    import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleSearchCriteria;
    import staport.finance.gBanque.LigneFonctionnelle.generalLignes.entities.LigneFonctionnelle;
    import org.springframework.data.jpa.domain.Specification;

    import java.time.LocalDate;
    import java.util.ArrayList;
    import java.util.List;

    public class LigneFonctionnelleSpecification {

        public static Specification<LigneFonctionnelle> searchByCriteria(LigneFonctionnelleSearchCriteria criteria) {
            return (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                LocalDate now = LocalDate.now();
                // Search by Banque ID
                if (criteria.getBanqueId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), criteria.getBanqueId()));
                }

                // Search by NatureLigneFonctionnelle ID
                if (criteria.getNatureLigneFonctionnelleId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("natureLigneFonctionnelle").get("id"), criteria.getNatureLigneFonctionnelleId()));
                }

                if (criteria.getMarcheId() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("marche").get("id"), criteria.getMarcheId()));
                }

                // Search by Year of dateDepart
                if (criteria.getYearDepart() != null) {
                    predicates.add(criteriaBuilder.equal(
                            criteriaBuilder.function("YEAR", Integer.class, root.get("dateDepart")),
                            criteria.getYearDepart()
                    ));
                }

                // If actif is null, it means we don't care about the active status, so don't add a predicate for it.
                // If actif is true/false, filter by that value.
                if (criteria.getActif() != null) {
                    predicates.add(criteriaBuilder.equal(root.get("actif"), criteria.getActif()));
                }

                // 5. Filter by 'isExpired' status
                if (criteria.getIsExpired() != null) {
                    if (criteria.getIsExpired()) {
                        predicates.add(criteriaBuilder.lessThan(root.get("dateFin"), now));
                    } else {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateFin"), now));
                    }
                }

                if (predicates.isEmpty()) {
                    return criteriaBuilder.conjunction(); // If no specific criteria, return all records
                }

                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
        }

        /**
         * Crée une Specification qui filtre uniquement les Lignes Mères (celles qui n'ont pas de parent).
         * C'est-à-dire : LigneFonctionnelle.ligneMere IS NULL.
         */
        public static Specification<LigneFonctionnelle> isLigneMereOrIndependante() {
            return (Root<LigneFonctionnelle> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
                // Le prédicat pour vérifier si la colonne 'ligneMere' est NULL
                return criteriaBuilder.isNull(root.get("ligneMere"));
            };
        }

        public static Specification<LigneFonctionnelle> hasMarche() {
            return (Root<LigneFonctionnelle> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("marche"));
        }

    }