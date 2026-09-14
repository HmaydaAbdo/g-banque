package staport.finance.gBanque.leasing.echeancesLeasing.specification;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Spécifications JPA pour la recherche avancée des entités EcheanceLeasing.
 */
public class EcheanceLeasingSpecification {

    public static Specification<EcheanceLeasing> searchByCriteria(EcheanceLeasingSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (searchDTO.contratLeasingId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("contratLeasing").get("id"), searchDTO.contratLeasingId()));
            }

            if (searchDTO.numeroEcheance() != null) {
                predicates.add(criteriaBuilder.equal(root.get("numeroEcheance"), searchDTO.numeroEcheance()));
            }

            if (searchDTO.statutPaiement() != null) {
                predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), searchDTO.statutPaiement()));
            }

            // Prédicats pour les plages de dates
            addDateRangePredicate(predicates, criteriaBuilder, root.get("dateEcheance"), searchDTO.dateEcheanceDebut(), searchDTO.dateEcheanceFin());
            addDateRangePredicate(predicates, criteriaBuilder, root.get("dateOperation"), searchDTO.dateOperationDebut(), searchDTO.dateOperationFin());
            addDateRangePredicate(predicates, criteriaBuilder, root.get("dateValeur"), searchDTO.dateValeurDebut(), searchDTO.dateValeurFin());

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addDateRangePredicate(List<Predicate> predicates, CriteriaBuilder builder,
                                                Path<LocalDate> path, LocalDate debut, LocalDate fin) {
        if (debut != null && fin != null) {
            predicates.add(builder.between(path, debut, fin));
        } else if (debut != null) {
            predicates.add(builder.greaterThanOrEqualTo(path, debut));
        } else if (fin != null) {
            predicates.add(builder.lessThanOrEqualTo(path, fin));
        }
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
     * L'organisme n'est pas directement porté par {@code ContratLeasing} — il est atteint via
     * son {@code contratCadre} ({@code ContratLeasing.contratCadre.organisme}).
     *
     * @param organismeLeasingId L'ID de l'organisme de leasing.
     * @return Une Specification qui filtre par l'ID de l'organisme.
     */
    public static Specification<EcheanceLeasing> byContratOrganismeLeasingId(Long organismeLeasingId) {
        return (root, query, criteriaBuilder) -> {
            Join<EcheanceLeasing, ContratLeasing> contratLeasingJoin = root.join("contratLeasing");
            Join<ContratLeasing, ContratCadre> contratCadreJoin = contratLeasingJoin.join("contratCadre");
            return criteriaBuilder.equal(contratCadreJoin.get("organisme").get("id"), organismeLeasingId);
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
