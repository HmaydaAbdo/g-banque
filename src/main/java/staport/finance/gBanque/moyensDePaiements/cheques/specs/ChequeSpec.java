package staport.finance.gBanque.moyensDePaiements.cheques.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class ChequeSpec {

    /**
     * Specification to filter Cheques by their numero (case-insensitive).
     * @param numero The cheque number to match.
     * @return A Specification for the cheque number.
     */
    public static Specification<Cheque> hasNumero(String numero) {
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

    /**
     * Specification to filter Cheques by their beneficiary (case-insensitive).
     * @param beneficiaire The beneficiary name to match.
     * @return A Specification for the beneficiary.
     */
    public static Specification<Cheque> hasBeneficiaire(String beneficiaire) {
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


    public static Specification<Cheque> hasEmetteur(String emetteur) {
        return (root, query, criteriaBuilder) -> {
            if (emetteur == null || emetteur.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("emetteur")),
                    "%" + emetteur.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Cheques by the name of the associated bank (case-insensitive).
     * @param nomBanque The bank name to match.
     * @return A Specification for the bank name.
     */
    public static Specification<Cheque> hasNomBanque(String nomBanque) {
        return (root, query, criteriaBuilder) -> {
            if (nomBanque == null || nomBanque.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("banque").get("nom")),
                    "%" + nomBanque.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Cheques by their active status.
     * @param active The active status (true or false).
     * @return A Specification for the active status.
     */
    public static Specification<Cheque> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    public static Specification<Cheque> hasType(Type type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    public static Specification<Cheque> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
        };
    }



    public static Specification<Cheque> hasDateEmission(LocalDate dateEmission) {
        return (root, query, criteriaBuilder) -> {
            if (dateEmission == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEmission"), dateEmission);
        };
    }


    public static Specification<Cheque> encoursDecaissements(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDate now = LocalDate.now();

            // 1. Check if the cheque is active.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));

            // 2. Check if the cheque belongs to the specified bank ID.
            if (banqueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            } else {
                // If banqueId is null, return a predicate that will always be false
                return criteriaBuilder.disjunction();
            }

            // 3. Check if the emission date is on or before the last day of the current month.
            LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());

            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Cheque> encoursEncaissements(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDate now = LocalDate.now();

            // 1. Check if the cheque is active.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));

            // 2. Check if the cheque belongs to the specified bank ID.
            if (banqueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            } else {
                // If banqueId is null, return a predicate that will always be false
                return criteriaBuilder.disjunction();
            }

            // 3. Check if the emission date is on or before the last day of the current month.
            LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());

            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}