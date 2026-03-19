package staport.finance.gBanque.leasing.echeancesLeasing.specification;


import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Spécifications JPA pour la recherche avancée des entités EcheanceLeasing.
 */
public class EcheanceLeasingSpecification {

    public static Specification<EcheanceLeasing> buildSpecification(EcheanceLeasingSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDTO.getContratLeasingId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("contratLeasing").get("id"), searchDTO.getContratLeasingId()));
            }

            if (searchDTO.getNumeroEcheance() != null) {
                predicates.add(criteriaBuilder.equal(root.get("numeroEcheance"), searchDTO.getNumeroEcheance()));
            }

            if (searchDTO.getStatutPaiement() != null) {
                predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), searchDTO.getStatutPaiement()));
            }

            // Gestion du mois / année
            Integer year = searchDTO.getYear();
            Integer month = searchDTO.getMonth();

            if (year != null && month != null) {
                // Cas mois + année
                LocalDate start = LocalDate.now()
                        .withYear(year).withMonth(month)
                        .with(TemporalAdjusters.firstDayOfMonth())
                        ;

                LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth())
                        ;

                predicates.add(criteriaBuilder.between(root.get("dateEcheance"), start, end));

            } else if (year != null) {
                // Cas année seule
                LocalDate start = LocalDate.now()
                        .withYear(year)
                        .withMonth(1).with(TemporalAdjusters.firstDayOfMonth())
                        ;

                LocalDate end = LocalDate.now()
                        .withYear(year)
                        .withMonth(12).with(TemporalAdjusters.lastDayOfMonth())
                        ;

                predicates.add(criteriaBuilder.between(root.get("dateEcheance"), start, end));

            } else if (month != null) {
                Expression<Integer> monthExpr = criteriaBuilder.function("MONTH", Integer.class, root.get("dateEcheance"));

                predicates.add(criteriaBuilder.equal(monthExpr, month));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Les méthodes statiques spécifiques sont mises à jour ou conservées en fonction des besoins
    public static Specification<EcheanceLeasing> byContratId(Long contratId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("contratLeasing").get("id"), contratId);
    }
    /**
     * Creates a Specification to filter EcheanceLeasing entities by the contract number
     * associated with their parent contract.
     *
     * @param numeroContrat The contract number for filtering.
     * @return A Specification that filters by the contract number.
     */
    public static Specification<EcheanceLeasing> byContratNumeroContrat(String numeroContrat) {
        return (root, query, criteriaBuilder) -> {
            Join<EcheanceLeasing, ContratLeasing> contratLeasingJoin = root.join("contratLeasing");
            return criteriaBuilder.like(contratLeasingJoin.get("numeroContrat"), "%" + numeroContrat + "%");
        };
    }




    /**
     * Crée une Specification pour filtrer les entités EcheanceLeasing par l'ID de la banque
     * associée à leur contrat parent.
     *
     * @param banqueId L'ID de la banque pour le filtrage.
     * @return Une Specification qui filtre par l'ID de la banque.
     */
    public static Specification<EcheanceLeasing> byContratBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            // Effectue une jointure entre EcheanceLeasing et ContratLeasing
            Join<EcheanceLeasing, ContratLeasing> contratLeasingJoin = root.join("contratLeasing");
            // Accède à l'entité BanqueDePrelevement via le ContratLeasing joint
            return criteriaBuilder.equal(contratLeasingJoin.get("banqueDePrelevement").get("id"), banqueId);
        };
    }

    public static Specification<EcheanceLeasing> byMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("montantTTC"), montant);
    }


    /**
     * Crée une Specification pour filtrer les échéances par l'organisme de leasing du contrat.
     *
     * @param organismeLeasingId L'ID de l'organisme de leasing.
     * @return Une Specification qui filtre par l'ID de l'organisme.
     */
    public static Specification<EcheanceLeasing> byContratOrganismeLeasingId(Long organismeLeasingId) {
        return (root, query, criteriaBuilder) -> {
            Join<EcheanceLeasing, ContratLeasing> contratLeasingJoin = root.join("contratLeasing");
            return criteriaBuilder.equal(contratLeasingJoin.get("organismeLeasing").get("id"), organismeLeasingId);
        };
    }

    public static Specification<EcheanceLeasing> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.lessThanOrEqualTo(root.get("dateEcheance"), until),
                cb.equal(root.get("statutPaiement"), StatutEcheanceLeasing.PREVU)
        );
    }

    //  plage from → until
    public static Specification<EcheanceLeasing> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.greaterThanOrEqualTo(root.get("dateEcheance"), from),
                cb.lessThanOrEqualTo(root.get("dateEcheance"), until),
                cb.equal(root.get("statutPaiement"), StatutEcheanceLeasing.PREVU)
        );
    }

}
