package staport.finance.gBanque.leasing.contratLeasing.specification;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasingPhase;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.util.ArrayList;
import java.util.List;

public class ContratLeasingSpecification {

    public static Specification<ContratLeasing> searchByCriteria(ContratLeasingSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (searchDTO.numeroContrat() != null && !searchDTO.numeroContrat().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroContrat")), "%" + searchDTO.numeroContrat().toLowerCase() + "%"));
            }
            if (searchDTO.banqueDePrelevementId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("banqueDePrelevement").get("id"), searchDTO.banqueDePrelevementId()));
            }
            if (searchDTO.organismeLeasingId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("contratCadre").get("organisme").get("id"),
                        searchDTO.organismeLeasingId()));
            }
            if (searchDTO.immobilisation() != null && !searchDTO.immobilisation().isEmpty()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("immobilisation")), "%" + searchDTO.immobilisation().toLowerCase() + "%"));
            }
            if (searchDTO.typeContrat() != null) {
                predicates.add(criteriaBuilder.equal(root.get("typeContrat"), searchDTO.typeContrat()));
            }
            if (searchDTO.completed() != null) {
                predicates.add(criteriaBuilder.equal(root.get("completed"), searchDTO.completed()));
            }
            if (searchDTO.montantEcheance() != null) {
                Subquery<Long> phaseSubquery = query.subquery(Long.class);
                Root<ContratLeasingPhase> phaseRoot = phaseSubquery.from(ContratLeasingPhase.class);
                phaseSubquery.select(phaseRoot.get("id"))
                        .where(criteriaBuilder.and(
                                criteriaBuilder.equal(phaseRoot.get("contratLeasing").get("id"), root.get("id")),
                                criteriaBuilder.equal(phaseRoot.get("mensualiteTTC"), searchDTO.montantEcheance())
                        ));
                predicates.add(criteriaBuilder.exists(phaseSubquery));
            }
            if (searchDTO.dateDebutMin() != null && searchDTO.dateDebutMax() != null) {
                predicates.add(criteriaBuilder.between(root.get("dateDebut"), searchDTO.dateDebutMin(), searchDTO.dateDebutMax()));
            } else if (searchDTO.dateDebutMin() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateDebut"), searchDTO.dateDebutMin()));
            } else if (searchDTO.dateDebutMax() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateDebut"), searchDTO.dateDebutMax()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Contrats leasing encours : completed = false ET au moins une échéance impayée.
     */
    public static Specification<ContratLeasing> getActiveContrats() {
        return (root, query, criteriaBuilder) -> {
            Predicate completedIsFalse = criteriaBuilder.isFalse(root.get("completed"));

            Subquery<EcheanceLeasing> subquery = query.subquery(EcheanceLeasing.class);
            jakarta.persistence.criteria.Root<EcheanceLeasing> echeanceRoot = subquery.from(EcheanceLeasing.class);
            subquery.select(echeanceRoot.get("id"))
                    .where(criteriaBuilder.and(
                            criteriaBuilder.equal(echeanceRoot.get("contratLeasing").get("id"), root.get("id")),
                            criteriaBuilder.notEqual(echeanceRoot.get("statutPaiement"), StatutEcheanceLeasing.PAYE)
                    ));

            return criteriaBuilder.and(completedIsFalse, criteriaBuilder.exists(subquery));
        };
    }

    /**
     * Contrats leasing en attente de main levée : completed = false ET toutes les échéances payées.
     */
    public static Specification<ContratLeasing> getPendingLienReleaseContrats() {
        return (root, query, criteriaBuilder) -> {
            Predicate completedIsFalse = criteriaBuilder.isFalse(root.get("completed"));

            Subquery<EcheanceLeasing> subquery = query.subquery(EcheanceLeasing.class);
            jakarta.persistence.criteria.Root<EcheanceLeasing> echeanceRoot = subquery.from(EcheanceLeasing.class);
            subquery.select(echeanceRoot.get("id"))
                    .where(criteriaBuilder.and(
                            criteriaBuilder.equal(echeanceRoot.get("contratLeasing").get("id"), root.get("id")),
                            criteriaBuilder.notEqual(echeanceRoot.get("statutPaiement"), StatutEcheanceLeasing.PAYE)
                    ));

            return criteriaBuilder.and(completedIsFalse, criteriaBuilder.not(criteriaBuilder.exists(subquery)));
        };
    }

    /**
     * Contrats leasing archivés : completed = true.
     */
    public static Specification<ContratLeasing> getInactiveContrats() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isTrue(root.get("completed"));
    }
}
