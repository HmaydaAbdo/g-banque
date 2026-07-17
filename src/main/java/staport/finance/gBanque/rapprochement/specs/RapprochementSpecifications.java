package staport.finance.gBanque.rapprochement.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.dtos.RapprochementSearchDto;
import staport.finance.gBanque.rapprochement.enums.StatutRapprochement;

import java.time.YearMonth;

public class RapprochementSpecifications {

    private RapprochementSpecifications() {}

    /**
     * Crée une Specification pour filtrer par l'ID de la Banque.
     */
    public static Specification<Rapprochement> hasBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            if (banqueId == null) {
                return criteriaBuilder.conjunction(); // Clause "WHERE 1=1" si null
            }
            // Utilise 'banque' (ManyToOne) pour accéder à l'attribut 'id'
            return criteriaBuilder.equal(root.get("banque").get("id"), banqueId);
        };
    }

    /**
     * Crée une Specification pour filtrer par la date de rapprochement (YearMonth).
     */
    public static Specification<Rapprochement> hasDateRapprochement(YearMonth dateRapprochement) {
        return (root, query, criteriaBuilder) -> {
            if (dateRapprochement == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateRapprochement"), dateRapprochement);
        };
    }

    public static Specification<Rapprochement> hasStatut(StatutRapprochement statut) {
        return (root, query, criteriaBuilder) -> {
            if (statut == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("statut"), statut);
        };
    }

    /**
     * Combine toutes les spécifications du DTO de recherche.
     */
    public static Specification<Rapprochement> searchByCriteria(RapprochementSearchDto searchDto) {
        Specification<Rapprochement> spec = Specification.where(null); // Point de départ neutre

        if (searchDto != null) {
            spec = spec.and(hasBanqueId(searchDto.banqueId()));
            spec = spec.and(hasDateRapprochement(searchDto.parsedDateRapprochement()));
            spec = spec.and(hasStatut(searchDto.statutRapprochement()));
        }

        return spec;
    }
}
