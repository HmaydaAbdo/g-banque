package staport.finance.gBanque.leasing.contratLeasing.specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
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

            //  filtre pour le champ 'completed'
            if (searchDTO.getCompleted() != null) {
                predicates.add(criteriaBuilder.equal(root.get("completed"), searchDTO.getCompleted()));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification to filter for active contracts.
     * Condition: completed = false AND there is at least one unpaid installment.
     */
    public static Specification<ContratLeasing> getActiveContrats() {
        return (root, query, criteriaBuilder) -> {
            // Predicate 1: completed is false
            Predicate completedIsFalse = criteriaBuilder.isFalse(root.get("completed"));

            // Predicate 2: exists a subquery with unpaid installments
            Subquery<EcheanceLeasing> subquery = query.subquery(EcheanceLeasing.class);
            jakarta.persistence.criteria.Root<EcheanceLeasing> echeanceRoot = subquery.from(EcheanceLeasing.class);

            subquery.select(echeanceRoot.get("id"))
                    .where(criteriaBuilder.and(
                            // Link the subquery to the main query by contratLeasing ID
                            criteriaBuilder.equal(echeanceRoot.get("contratLeasing").get("id"), root.get("id")),
                            // Condition for unpaid installment (statutPaiement != PAYEE)
                            criteriaBuilder.notEqual(echeanceRoot.get("statutPaiement"), StatutEcheanceLeasing.PAYE)
                    ));

            // Combine both predicates
            return criteriaBuilder.and(completedIsFalse, criteriaBuilder.exists(subquery));
        };
    }

    /**
     * Specification to filter for contracts pending lien release.
     * Condition: completed = false AND ALL installments are paid.
     */
    public static Specification<ContratLeasing> getPendingLienReleaseContrats() {
        return (root, query, criteriaBuilder) -> {
            // Predicate 1: completed is false
            Predicate completedIsFalse = criteriaBuilder.isFalse(root.get("completed"));

            // Predicate 2: does NOT exist a subquery with unpaid installments
            Subquery<EcheanceLeasing> subquery = query.subquery(EcheanceLeasing.class);
            jakarta.persistence.criteria.Root<EcheanceLeasing> echeanceRoot = subquery.from(EcheanceLeasing.class);

            subquery.select(echeanceRoot.get("id"))
                    .where(criteriaBuilder.and(
                            // Link the subquery to the main query by contratLeasing ID
                            criteriaBuilder.equal(echeanceRoot.get("contratLeasing").get("id"), root.get("id")),
                            // Condition for unpaid installment
                            criteriaBuilder.notEqual(echeanceRoot.get("statutPaiement"), StatutEcheanceLeasing.PAYE)
                    ));

            // Combine both predicates
            return criteriaBuilder.and(completedIsFalse, criteriaBuilder.not(criteriaBuilder.exists(subquery)));
        };
    }

    /**
     * Specification to filter for inactive contracts.
     * Condition: completed = true.
     */
    public static Specification<ContratLeasing> getInactiveContrats() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("completed"));
    }
}