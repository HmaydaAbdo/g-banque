package staport.finance.gBanque.cmt.specifications;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeSearchDto;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe de spécification pour construire dynamiquement des requêtes
 * de recherche pour l'entité CreditMoyenTerme.
 * Elle utilise le Criteria API de JPA.
 */
public class CreditMoyenTermeSpecification {

    /**
     * Crée un objet Specification pour filtrer les crédits en fonction
     * des critères de recherche fournis dans le DTO.
     *
     * @param searchDto Le DTO de recherche contenant les critères de filtre.
     * @return Un objet Specification combinant tous les prédicats de recherche.
     */
    public static Specification<CreditMoyenTerme> searchByCriteria(CreditMoyenTermeSearchDto searchDto) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prédicat pour l'ID de la banque
            if (searchDto.banqueId() != null) {
                predicates.add(builder.equal(root.get("banque").get("id"), searchDto.banqueId()));
            }

            // Prédicat pour la désignation (recherche partielle, insensible à la casse)
            if (searchDto.designation() != null && !searchDto.designation().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("designation")), "%" + searchDto.designation().toLowerCase() + "%"));
            }

            // Nouveau prédicat pour le champ 'completed'
            if (searchDto.completed() != null) {
                predicates.add(builder.equal(root.get("completed"), searchDto.completed()));
            }

            // Prédicat pour la plage de date de début
            if (searchDto.dateDebutMin() != null && searchDto.dateDebutMax() != null) {
                predicates.add(builder.between(root.get("dateDebut"), searchDto.dateDebutMin(), searchDto.dateDebutMax()));
            } else if (searchDto.dateDebutMin() != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("dateDebut"), searchDto.dateDebutMin()));
            } else if (searchDto.dateDebutMax() != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("dateDebut"), searchDto.dateDebutMax()));
            }

            // Combine tous les prédicats avec un opérateur AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Specification to filter for active credits.
     * Condition: completed = false AND there is at least one unpaid installment.
     */
    public static Specification<CreditMoyenTerme> getActiveCredits() {
        return (root, query, builder) -> {
            // Predicate 1: completed is false
            Predicate completedIsFalse = builder.isFalse(root.get("completed"));

            // Predicate 2: exists a subquery with unpaid installments
            Subquery<EcheanceCMT> subquery = query.subquery(EcheanceCMT.class);
            jakarta.persistence.criteria.Root<EcheanceCMT> echeanceRoot = subquery.from(EcheanceCMT.class);

            subquery.select(echeanceRoot.get("id"))
                    .where(builder.and(
                            builder.equal(echeanceRoot.get("creditMoyenTerme").get("id"), root.get("id")),
                            builder.notEqual(echeanceRoot.get("statutPaiement"), StatutEcheanceCMT.PAYE)
                    ));

            return builder.and(completedIsFalse, builder.exists(subquery));
        };
    }

    /**
     * Specification to filter for credits pending closure.
     * Condition: completed = false AND ALL installments are paid.
     */
    public static Specification<CreditMoyenTerme> getPendingClosureCredits() {
        return (root, query, builder) -> {
            // Predicate 1: completed is false
            Predicate completedIsFalse = builder.isFalse(root.get("completed"));

            // Predicate 2: does NOT exist a subquery with unpaid installments
            Subquery<EcheanceCMT> subquery = query.subquery(EcheanceCMT.class);
            jakarta.persistence.criteria.Root<EcheanceCMT> echeanceRoot = subquery.from(EcheanceCMT.class);

            subquery.select(echeanceRoot.get("id"))
                    .where(builder.and(
                            builder.equal(echeanceRoot.get("creditMoyenTerme").get("id"), root.get("id")),
                            builder.notEqual(echeanceRoot.get("statutPaiement"), StatutEcheanceCMT.PAYE)
                    ));

            return builder.and(completedIsFalse, builder.not(builder.exists(subquery)));
        };
    }

    /**
     * Specification to filter for archived credits.
     * Condition: completed = true.
     */
    public static Specification<CreditMoyenTerme> getArchivedCredits() {
        return (root, query, builder) -> builder.isTrue(root.get("completed"));
    }
}