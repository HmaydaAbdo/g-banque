package staport.finance.gBanque.miseADisposition.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;

public final class MiseADispositionSpec {

    /**
     * Specification to filter MiseADisposition by numero (case-insensitive).
     * @param numero The numero to match.
     * @return A Specification for the numero.
     */
    public static Specification<MiseADisposition> hasNumero(String numero) {
        return (root, query, criteriaBuilder) -> {
            if (numero == null || numero.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("numero")),
                    "%" + numero.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter MiseADisposition by beneficiaire (case-insensitive).
     * @param beneficiaire The beneficiaire name to match.
     * @return A Specification for the beneficiaire.
     */
    public static Specification<MiseADisposition> hasBeneficiaire(String beneficiaire) {
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
     * Specification to filter MiseADisposition by the beneficiary's CIN (case-insensitive).
     * @param cinBeneficiaire The beneficiary's CIN to match.
     * @return A Specification for the CIN.
     */
    public static Specification<MiseADisposition> hasCinBeneficiaire(String cinBeneficiaire) {
        return (root, query, criteriaBuilder) -> {
            if (cinBeneficiaire == null || cinBeneficiaire.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("cinBeneficiaire")),
                    "%" + cinBeneficiaire.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter MiseADisposition by the associated bank's name (case-insensitive).
     * @param banqueName The bank's name to match.
     * @return A Specification for the bank name.
     */
    public static Specification<MiseADisposition> hasBanqueName(String banqueName) {
        return (root, query, criteriaBuilder) -> {
            if (banqueName == null || banqueName.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("banque").get("nom")),
                    "%" + banqueName.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter MiseADisposition by its active status.
     * @param active The active status (true or false).
     * @return A Specification for the active status.
     */
    public static Specification<MiseADisposition> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    /**
     * Combines multiple specifications from a MiseADispositionSearchDto into a single Specification.
     * @param searchDto The DTO containing search criteria.
     * @return A combined Specification for the MiseADisposition entity.
     */
    public static Specification<MiseADisposition> bySearchDto(MiseADispositionSearchDto searchDto) {
        Specification<MiseADisposition> spec = Specification.where(null);

        if (searchDto.getNumero() != null) {
            spec = spec.and(hasNumero(searchDto.getNumero()));
        }

        if (searchDto.getBeneficiaire() != null) {
            spec = spec.and(hasBeneficiaire(searchDto.getBeneficiaire()));
        }

        if (searchDto.getCinBeneficiaire() != null) {
            spec = spec.and(hasCinBeneficiaire(searchDto.getCinBeneficiaire()));
        }

        if (searchDto.getBanqueName() != null) {
            spec = spec.and(hasBanqueName(searchDto.getBanqueName()));
        }

        if (searchDto.getActive() != null) {
            spec = spec.and(isActive(searchDto.getActive()));
        }

        return spec;
    }
}