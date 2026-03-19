package staport.finance.gBanque.cmt.echeances.specification;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTSearchDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;


import java.math.BigDecimal;
import java.time.LocalDate;
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
                LocalDate startOfMonth = LocalDate.of(year, month, 1)
                        .with(TemporalAdjusters.firstDayOfMonth());

                LocalDate endOfMonth = startOfMonth
                        .with(TemporalAdjusters.lastDayOfMonth());

                predicates.add(builder.between(root.get("dateEcheance"), startOfMonth, endOfMonth));

            } else if (year != null) {
                // Cas : Année seule
                LocalDate startOfYear = LocalDate.of(year, 1, 1);

                LocalDate endOfYear = startOfYear
                        .with(TemporalAdjusters.lastDayOfYear());

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

    public static Specification<EcheanceCMT> byMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null ) {
                return criteriaBuilder.conjunction(); // A predicate that is always true
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
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

    public static Specification<EcheanceCMT> outstandingUntil(LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), StatutEcheanceCMT.PREVU));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Nouveau — pour le recap (plage from → until)
    public static Specification<EcheanceCMT> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statutPaiement"), StatutEcheanceCMT.PREVU));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEcheance"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }



}
