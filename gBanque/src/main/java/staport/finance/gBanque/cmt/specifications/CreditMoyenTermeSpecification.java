package staport.finance.gBanque.cmt.specifications;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cmt.dtos.CreditMoyenTermeSearchDto;
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
    public static Specification<CreditMoyenTerme> withSearchCriteria(CreditMoyenTermeSearchDto searchDto) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Prédicat pour l'ID de la banque
            if (searchDto.getBanqueId() != null) {
                predicates.add(builder.equal(root.get("banque").get("id"), searchDto.getBanqueId()));
            }

            // Prédicat pour la désignation (recherche partielle, insensible à la casse)
            if (searchDto.getDesignation() != null && !searchDto.getDesignation().isBlank()) {
                predicates.add(builder.like(builder.lower(root.get("designation")), "%" + searchDto.getDesignation().toLowerCase() + "%"));
            }

            // Nouveau prédicat pour le champ 'completed'
            // Le type Boolean permet à la valeur d'être null (non spécifiée), true ou false.
            if (searchDto.getCompleted() != null) {
                predicates.add(builder.equal(root.get("completed"), searchDto.getCompleted()));
            }

            // Combine tous les prédicats avec un opérateur AND
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }
}