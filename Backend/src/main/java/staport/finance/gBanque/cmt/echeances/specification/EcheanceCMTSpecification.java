package staport.finance.gBanque.cmt.echeances.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cmt.echeances.dtos.EcheanceCMTSearchDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe de spécification pour construire dynamiquement des requêtes
 * de recherche pour l'entité EcheanceCMT.
 * Elle utilise le Criteria API de JPA.
 */
public class EcheanceCMTSpecification {

    public static Specification<EcheanceCMT> searchByCriteria(EcheanceCMTSearchDto searchDto) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prédicat pour l'ID du crédit parent
            if (searchDto.creditMoyenTermeId() != null) {
                predicates.add(builder.equal(root.get("creditMoyenTerme").get("id"), searchDto.creditMoyenTermeId()));
            }

            // Prédicat pour le numéro d'échéance
            if (searchDto.numeroEcheance() != null) {
                predicates.add(builder.equal(root.get("numeroEcheance"), searchDto.numeroEcheance()));
            }

            // Prédicat pour le statut de paiement
            if (searchDto.statutPaiement() != null) {
                predicates.add(builder.equal(root.get("statutPaiement"), searchDto.statutPaiement()));
            }

            // Prédicats pour les plages de dates
            addDateRangePredicate(predicates, builder, root.get("dateEcheance"), searchDto.dateEcheanceDebut(), searchDto.dateEcheanceFin());
            addDateRangePredicate(predicates, builder, root.get("dateOperation"), searchDto.dateOperationDebut(), searchDto.dateOperationFin());
            addDateRangePredicate(predicates, builder, root.get("dateValeur"), searchDto.dateValeurDebut(), searchDto.dateValeurFin());

            // Combine tous les prédicats avec un opérateur AND
            return builder.and(predicates.toArray(new Predicate[0]));
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
