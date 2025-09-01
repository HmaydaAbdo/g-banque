package staport.finance.gBanque.cheques.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cheques.entities.Cheque;

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
}