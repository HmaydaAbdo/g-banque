package staport.finance.gBanque.virements.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.dtos.VirementSearchDto;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class VirementSpec {

    /**
     * Specification to filter Virements by their numero (case-insensitive).
     * @param numero The virement number to match.
     * @return A Specification for the virement number.
     */
    public static Specification<Virement> hasNumero(String numero) {
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
     * Specification to filter Virements by their beneficiary (case-insensitive).
     * @param beneficiaire The beneficiary name to match.
     * @return A Specification for the beneficiary.
     */
    public static Specification<Virement> hasBeneficiaire(String beneficiaire) {
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
     * Specification to filter Virements by the beneficiary's bank name (case-insensitive).
     * @param banqueBeneficiaire The beneficiary's bank name to match.
     * @return A Specification for the beneficiary's bank name.
     */
    public static Specification<Virement> hasBanqueBeneficiaire(String banqueBeneficiaire) {
        return (root, query, criteriaBuilder) -> {
            if (banqueBeneficiaire == null || banqueBeneficiaire.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("banqueBeneficiaire")),
                    "%" + banqueBeneficiaire.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Virements by the name of the associated emitting bank (case-insensitive).
     * @param nomBanqueEmettrice The emitting bank name to match.
     * @return A Specification for the emitting bank name.
     */
    public static Specification<Virement> hasNomBanqueEmettrice(String nomBanqueEmettrice) {
        return (root, query, criteriaBuilder) -> {
            if (nomBanqueEmettrice == null || nomBanqueEmettrice.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("banque").get("nom")),
                    "%" + nomBanqueEmettrice.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Virements by the beneficiary's RIB (case-insensitive).
     * @param ribBeneficiaire The beneficiary's RIB to match.
     * @return A Specification for the RIB.
     */
    public static Specification<Virement> hasRibBeneficiaire(String ribBeneficiaire) {
        return (root, query, criteriaBuilder) -> {
            if (ribBeneficiaire == null || ribBeneficiaire.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("ribBeneficiaire")),
                    "%" + ribBeneficiaire.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter Virements by their active status.
     * @param active The active status (true or false).
     * @return A Specification for the active status.
     */
    public static Specification<Virement> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    /**
     * Combines multiple specifications from a VirementSearchDto into a single Specification.
     * @param searchDto The DTO containing search criteria.
     * @return A combined Specification for the Virement entity.
     */
    public static Specification<Virement> bySearchDto(VirementSearchDto searchDto) {
        Specification<Virement> spec = Specification.where(null);

        if (searchDto.getNumero() != null) {
            spec = spec.and(hasNumero(searchDto.getNumero()));
        }

        if (searchDto.getBeneficiaire() != null) {
            spec = spec.and(hasBeneficiaire(searchDto.getBeneficiaire()));
        }

        if (searchDto.getBanqueBeneficiaire() != null) {
            spec = spec.and(hasBanqueBeneficiaire(searchDto.getBanqueBeneficiaire()));
        }

        if (searchDto.getNomBanqueEmettrice() != null) {
            spec = spec.and(hasNomBanqueEmettrice(searchDto.getNomBanqueEmettrice()));
        }

        if (searchDto.getRibBeneficiaire() != null) {
            spec = spec.and(hasRibBeneficiaire(searchDto.getRibBeneficiaire()));
        }

        if (searchDto.getActive() != null) {
            spec = spec.and(isActive(searchDto.getActive()));
        }

        return spec;
    }


    /**
     * Spécification pour filtrer les virements actifs d'une banque spécifique avec une date d'émission
     * qui est égale ou antérieure à la fin du mois en cours.
     *
     * @param banqueId L'ID de la banque pour laquelle les virements sont recherchés.
     * @return Une Specification qui combine les critères de filtrage.
     */
    public static Specification<Virement> activeForBankAndUntilEndOfMonth(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();

            // 1. Le statut du virement doit être actif.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            // 2. Le virement doit appartenir à la banque spécifiée par son ID.
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));


            // 3. La date d'émission du virement doit être égale ou antérieure à la fin du mois.
            OffsetDateTime endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23).withMinute(59).withSecond(59).withNano(999999999);
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}