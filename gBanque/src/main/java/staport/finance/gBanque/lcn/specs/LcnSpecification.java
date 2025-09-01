package staport.finance.gBanque.lcn.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.lcn.entities.Lcn;

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
}