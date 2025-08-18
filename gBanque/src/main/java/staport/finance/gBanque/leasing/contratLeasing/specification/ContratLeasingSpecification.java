package staport.finance.gBanque.leasing.contratLeasing.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import java.util.ArrayList;
import java.util.List;

public class ContratLeasingSpecification {

    public static Specification<ContratLeasing> bySearchCriteria(ContratLeasingSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filter by numeroContrat (case-insensitive, like search)
            if (searchDTO.getNumeroContrat() != null && !searchDTO.getNumeroContrat().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroContrat")), "%" + searchDTO.getNumeroContrat().toLowerCase() + "%"));
            }
            // Filter by banqueDePrelevementId
            if (searchDTO.getBanqueDePrelevementId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("banqueDePrelevement").get("id"), searchDTO.getBanqueDePrelevementId()));
            }
            // Filter by organismeLeasingId
            if (searchDTO.getOrganismeLeasingId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("organismeLeasing").get("id"), searchDTO.getOrganismeLeasingId()));
            }
            // Filter by immobilisation (case-insensitive, like search)
            if (searchDTO.getImmobilisation() != null && !searchDTO.getImmobilisation().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("immobilisation")), "%" + searchDTO.getImmobilisation().toLowerCase() + "%"));
            }

            // Filter by typeContrat
            if (searchDTO.getTypeContrat() != null) {
                predicates.add(criteriaBuilder.equal(root.get("typeContrat"), searchDTO.getTypeContrat()));
            }

            // ⭐ Nouveau filtre pour le champ 'completed'
            if (searchDTO.getCompleted() != null) {
                predicates.add(criteriaBuilder.equal(root.get("completed"), searchDTO.getCompleted()));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}