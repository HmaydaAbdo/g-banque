package staport.finance.gBanque.leasing.echeancesLeasing.specification;


import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.PeriodeEncours;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import java.time.OffsetDateTime;
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

            // --- LOGIQUE POUR LE MOIS ET L'ANNÉE ---
            if (searchDTO.getMonth() != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("MONTH", Integer.class, root.get("dateEcheance")),
                        searchDTO.getMonth()
                ));
            }
            if (searchDTO.getYear() != null) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.function("YEAR", Integer.class, root.get("dateEcheance")),
                        searchDTO.getYear()
                ));
            }

            if (searchDTO.getStatutPaiement() != null) {
                predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), searchDTO.getStatutPaiement()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Les méthodes statiques spécifiques sont mises à jour ou conservées en fonction des besoins
    public static Specification<EcheanceLeasing> byContratId(Long contratId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("contratLeasing").get("id"), contratId);
    }

    public static Specification<EcheanceLeasing> hasNumeroEcheance(Integer numeroEcheance) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("numeroEcheance"), numeroEcheance);
    }

    public static Specification<EcheanceLeasing> hasStatutPaiement(StatutEcheanceLeasing statut) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("statutPaiement"), statut);
    }

    public static Specification<EcheanceLeasing> hasMonth(Integer month) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.function("MONTH", Integer.class, root.get("dateEcheance")), month);
    }

    public static Specification<EcheanceLeasing> hasYear(Integer year) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(criteriaBuilder.function("YEAR", Integer.class, root.get("dateEcheance")), year);
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
    /**
     * Spécification pour les échéances non payées jusqu'à une période donnée.
     *
     * @param periode L'énumération de la période ("JOUR" ou "MOIS").
     * @param now La date et l'heure actuelles, utilisées pour les calculs.
     * @return Une Specification pour les échéances d'encours.
     */
    public static Specification<EcheanceLeasing> outstandingUntil(PeriodeEncours periode, OffsetDateTime now) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), StatutEcheanceLeasing.PREVU));

            OffsetDateTime dateButoir;
            if (PeriodeEncours.JOUR.equals(periode)) {
                dateButoir = now.withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            } else if (PeriodeEncours.MOIS.equals(periode)) {
                dateButoir = now.with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59);
            } else {
                // C'est un cas qui ne devrait pas arriver avec une énumération,
                // mais une gestion défensive est toujours une bonne idée.
                throw new IllegalArgumentException("La période d'encours spécifiée est invalide.");
            }

            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), dateButoir));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}