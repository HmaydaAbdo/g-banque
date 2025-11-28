package staport.finance.gBanque.lcn.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class LcnSpecification {

    /**
     * Specification to filter Lcns by their numero (case-insensitive).
     * @param numero The Lcn number to match.
     * @return A Specification for the Lcn number.
     */
    public static Specification<Lcn> hasNumero(String numero) {
        return (root, query, criteriaBuilder) -> {
            if (numero == null || numero.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // A no-op predicate
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("numero")),
                    "%" + numero.toLowerCase() + "%"
            );
        };
    }

    public static Specification<Lcn> hasType(Type type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null ) {
                return criteriaBuilder.conjunction(); // A no-op predicate
            }
            return criteriaBuilder.equal(root.get("type"), type);

        };
    }


    /**
     * Specification to filter Lcns by their beneficiary (case-insensitive).
     * @param beneficiaire The beneficiary name to match.
     * @return A Specification for the beneficiary.
     */
    public static Specification<Lcn> hasBeneficiaire(String beneficiaire) {
        return (root, query, criteriaBuilder) -> {
            if (beneficiaire == null || beneficiaire.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("beneficiaire")),
                    "%" + beneficiaire.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Lcns by the name of the associated bank (case-insensitive).
     * @param nomBanque The bank name to match.
     * @return A Specification for the bank name.
     */
    public static Specification<Lcn> hasBanqueNom(String nomBanque) {
        return (root, query, criteriaBuilder) -> {
            if (nomBanque == null || nomBanque.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.join("banque").get("nom")),
                    "%" + nomBanque.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Lcns by their active status.
     * @param active The active status (true or false).
     * @return A Specification for the active status.
     */
    public static Specification<Lcn> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    public static Specification<Lcn> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
        };
    }

    /**
     * Spécification pour filtrer les LCN actifs d'une banque spécifique avec une date d'échéance
     * qui est égale ou antérieure à la fin du mois en cours.
     *
     * @param banqueId L'ID de la banque pour laquelle les LCN sont recherchés.
     * @return Une Specification qui combine les critères de filtrage.
     */
    public static Specification<Lcn> activeForBankAndUntilEndOfMonth(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();

            // 1. Le statut du LCN doit être actif.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            // 2. Le LCN doit appartenir à la banque spécifiée par son ID.
            if (banqueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            } else {
                return criteriaBuilder.disjunction();
            }

            // 3. La date d'échéance du LCN doit être égale ou antérieure à la fin du mois.
            OffsetDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), endOfMonth));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}