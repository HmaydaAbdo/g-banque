package staport.finance.gBanque.cmt.echeances.specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTSearchDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;


import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de spécification pour construire dynamiquement des requêtes
 * de recherche pour l'entité EcheanceCMT.
 * Elle utilise le Criteria API de JPA.
 */
public class EcheanceCMTSpecification {

    public static Specification<EcheanceCMT> withSearchCriteria(EcheanceCMTSearchDto searchDto) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prédicat pour l'ID du crédit parent
            if (searchDto.getCreditMoyenTermeId() != null) {
                predicates.add(builder.equal(root.get("creditMoyenTerme").get("id"), searchDto.getCreditMoyenTermeId()));
            }

            // Prédicat pour le numéro d'échéance
            if (searchDto.getNumeroEcheance() != null) {
                predicates.add(builder.equal(root.get("numeroEcheance"), searchDto.getNumeroEcheance()));
            }

            // Prédicat pour le statut de paiement
            if (searchDto.getStatutPaiement() != null) {
                predicates.add(builder.equal(root.get("statutPaiement"), searchDto.getStatutPaiement()));
            }

            // Prédicats pour le mois et l'année en utilisant une plage de dates pour plus de robustesse.
            Integer year = searchDto.getYear();
            Integer month = searchDto.getMonth();

            if (year != null && month != null) {
                // Cas : Année + Mois
                OffsetDateTime startOfMonth = OffsetDateTime.now()
                        .withYear(year)
                        .withMonth(month)
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);

                OffsetDateTime endOfMonth = startOfMonth
                        .with(TemporalAdjusters.lastDayOfMonth())
                        .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

                predicates.add(builder.between(root.get("dateEcheance"), startOfMonth, endOfMonth));

            } else if (year != null) {
                // Cas : Année seule
                OffsetDateTime startOfYear = OffsetDateTime.now()
                        .withYear(year)
                        .with(TemporalAdjusters.firstDayOfYear())
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);

                OffsetDateTime endOfYear = startOfYear
                        .with(TemporalAdjusters.lastDayOfYear())
                        .withHour(23).withMinute(59).withSecond(59).withNano(999999999);

                predicates.add(builder.between(root.get("dateEcheance"), startOfYear, endOfYear));

            } else if (month != null) {
                // Cas : Mois seul (toutes les années)
                Expression<Integer> monthExpr = builder.function("MONTH", Integer.class, root.get("dateEcheance"));
                predicates.add(builder.equal(monthExpr, month));
            }


            // Combine tous les prédicats avec un opérateur AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Crée une Specification pour filtrer les échéances par l'ID de la banque
     * associée au crédit à moyen terme parent.
     *
     * @param banqueId L'ID de la banque pour le filtrage.
     * @return Une Specification qui filtre par l'ID de la banque.
     */
    public static Specification<EcheanceCMT> byCreditBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            // Effectue une jointure entre EcheanceCMT et CreditMoyenTerme
            Join<EcheanceCMT, CreditMoyenTerme> creditJoin = root.join("creditMoyenTerme");
            // Accède à l'entité Banque via le CreditMoyenTerme joint
            return criteriaBuilder.equal(creditJoin.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<EcheanceCMT> byCreditDesignationLike(String designation) {
        return (root, query, criteriaBuilder) -> {
            // Handle null or blank input to avoid errors
            if (designation == null || designation.isBlank()) {
                return criteriaBuilder.conjunction(); // A predicate that is always true
            }

            // Join to the parent entity CreditMoyenTerme
            Join<EcheanceCMT, CreditMoyenTerme> creditJoin = root.join("creditMoyenTerme");

            // Create a case-insensitive LIKE predicate
            return criteriaBuilder.like(
                    criteriaBuilder.lower(creditJoin.get("designation")),
                    "%" + designation.toLowerCase() + "%"
            );
        };
    }

    /**
     * Crée une Specification pour filtrer les échéances non payées jusqu'à
     * une période donnée (fin du jour ou fin du mois).
     * @return Une Specification pour les échéances d'encours.
     */
    public static Specification<EcheanceCMT> outstandingUntil() {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Filtre les échéances dont le statut est "PREVU" (non payé)
            predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), StatutEcheanceCMT.PREVU));

            OffsetDateTime dateButoir;
            // Calcule la date butoir à la fin du dernier jour du mois
            dateButoir = OffsetDateTime.now().with(TemporalAdjusters.lastDayOfMonth()).withHour(23).withMinute(59).withSecond(59).withNano(999999999);

            // Ajoute la condition de date
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), dateButoir));

            // Combine tous les prédicats avec un opérateur AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}
