package staport.finance.gBanque.moyensDePaiements.miseADisposition.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

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

    public static Specification<MiseADisposition> hasType(Type type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null ) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);

        };
    }
    public static Specification<MiseADisposition> hasDateEmission(LocalDate dateEmission) {
        return (root, query, criteriaBuilder) -> {
            if (dateEmission == null ) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEmission"), dateEmission);

        };
    }




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

    public static Specification<MiseADisposition> hasEmetteur(String emetteur) {
        return (root, query, criteriaBuilder) -> {
            if (emetteur == null ) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("emetteur")),
                    "%" + emetteur.toLowerCase() + "%"
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

    public static Specification<MiseADisposition> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
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

        if(searchDto.getEmetteur()!=null){
            spec = spec.and(hasEmetteur(searchDto.getEmetteur()));
        }

        if (searchDto.getActive() != null) {
            spec = spec.and(isActive(searchDto.getActive()));
        }

        return spec;
    }




    /**
     * Spécification pour filtrer les mises à disposition actives d'une banque spécifique avec une date d'émission
     * qui est égale ou antérieure à la fin du mois en cours.
     *
     * @param banqueId L'ID de la banque pour laquelle les mises à disposition sont recherchés.
     * @return Une Specification qui combine les critères de filtrage.
     */
    public static Specification<MiseADisposition> encoursDecaissementForMiseAdispo(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDate now = LocalDate.now();

            // 1. Le statut de la mise à disposition doit être actif.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));

            // 2. La mise à disposition doit appartenir à la banque spécifiée par son ID.
            if (banqueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            } else {
                return criteriaBuilder.disjunction();
            }

            // 3. La date d'émission doit être égale ou antérieure à la fin du mois.
            LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<MiseADisposition> encoursEncaissementForMiseAdispo(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            LocalDate now = LocalDate.now();

            // 1. Le statut de la mise à disposition doit être actif.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));

            // 2. La mise à disposition doit appartenir à la banque spécifiée par son ID.
            if (banqueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            } else {
                return criteriaBuilder.disjunction();
            }

            // 3. La date d'émission doit être égale ou antérieure à la fin du mois.
            LocalDate endOfMonth = now.with(TemporalAdjusters.lastDayOfMonth());
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), endOfMonth));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}