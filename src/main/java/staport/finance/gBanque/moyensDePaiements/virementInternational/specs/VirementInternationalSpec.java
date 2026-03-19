package staport.finance.gBanque.moyensDePaiements.virementInternational.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.VirementInternationalSearchDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class VirementInternationalSpec {

    /**
     * Specification to filter VirementInternational by bank ID.
     * @param banqueId The bank ID to match.
     * @return A Specification for the bank ID.
     */
    public static Specification<VirementInternational> hasBanqueId(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            if (banqueId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("banque").get("id"), banqueId);
        };
    }

    /**
     * Specification to filter VirementInternational by beneficiary (case-insensitive).
     * @param beneficiaire The beneficiary name to match.
     * @return A Specification for the beneficiary.
     */
    public static Specification<VirementInternational> hasBeneficiaire(String beneficiaire) {
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
     * Specification to filter VirementInternational by emitter (case-insensitive).
     * @param emetteur The emitter name to match.
     * @return A Specification for the emitter.
     */
    public static Specification<VirementInternational> hasEmetteur(String emetteur) {
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
     * Specification to filter VirementInternational by amount.
     * @param montant The amount to match.
     * @return A Specification for the amount.
     */
    public static Specification<VirementInternational> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
        };
    }

    /**
     * Specification to filter VirementInternational by emission date.
     * @param dateEmission The emission date to match.
     * @return A Specification for the emission date.
     */
    public static Specification<VirementInternational> hasDateEmission(LocalDate dateEmission) {
        return (root, query, criteriaBuilder) -> {
            if (dateEmission == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEmission"), dateEmission);
        };
    }

    /**
     * Specification to filter VirementInternational by operation date.
     * @param dateOperation The operation date to match.
     * @return A Specification for the operation date.
     */
    public static Specification<VirementInternational> hasDateOperation(LocalDate dateOperation) {
        return (root, query, criteriaBuilder) -> {
            if (dateOperation == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateOperation"), dateOperation);
        };
    }

    /**
     * Specification to filter VirementInternational by value date.
     * @param dateValeur The value date to match.
     * @return A Specification for the value date.
     */
    public static Specification<VirementInternational> hasDateValeur(LocalDate dateValeur) {
        return (root, query, criteriaBuilder) -> {
            if (dateValeur == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateValeur"), dateValeur);
        };
    }

    /**
     * Specification to filter VirementInternational by libelle (case-insensitive).
     * @param libelle The libelle to match.
     * @return A Specification for the libelle.
     */
    public static Specification<VirementInternational> hasLibelle(String libelle) {
        return (root, query, criteriaBuilder) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("libelle")),
                    "%" + libelle.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter VirementInternational by observation (case-insensitive).
     * @param observation The observation to match.
     * @return A Specification for the observation.
     */
    public static Specification<VirementInternational> hasObservation(String observation) {
        return (root, query, criteriaBuilder) -> {
            if (observation == null || observation.trim().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("observation")),
                    "%" + observation.toLowerCase() + "%"
            );
        };
    }

    /**
     * Specification to filter VirementInternational by active status.
     * @param active The active status (true or false).
     * @return A Specification for the active status.
     */
    public static Specification<VirementInternational> isActive(Boolean active) {
        return (root, query, criteriaBuilder) -> {
            if (active == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("active"), active);
        };
    }

    /**
     * Specification to filter VirementInternational by type.
     * @param type The type to match.
     * @return A Specification for the type.
     */
    public static Specification<VirementInternational> hasType(Type type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    /**
     * Combines multiple specifications from a VirementInternationalSearchDto into a single Specification.
     * @param searchDto The DTO containing search criteria.
     * @return A combined Specification for the VirementInternational entity.
     */
    public static Specification<VirementInternational> bySearchDto(VirementInternationalSearchDto searchDto) {
        Specification<VirementInternational> spec = Specification.where(null);

        if (searchDto.getBanqueId() != null) {
            spec = spec.and(hasBanqueId(searchDto.getBanqueId()));
        }

        if (searchDto.getBeneficiaire() != null) {
            spec = spec.and(hasBeneficiaire(searchDto.getBeneficiaire()));
        }

        if (searchDto.getEmetteur() != null) {
            spec = spec.and(hasEmetteur(searchDto.getEmetteur()));
        }

        if (searchDto.getMontant() != null) {
            spec = spec.and(hasMontant(searchDto.getMontant()));
        }

        if (searchDto.getDateEmission() != null) {
            spec = spec.and(hasDateEmission(searchDto.getDateEmission()));
        }

        if (searchDto.getDateOperation() != null) {
            spec = spec.and(hasDateOperation(searchDto.getDateOperation()));
        }

        if (searchDto.getDateValeur() != null) {
            spec = spec.and(hasDateValeur(searchDto.getDateValeur()));
        }

        if (searchDto.getLibelle() != null) {
            spec = spec.and(hasLibelle(searchDto.getLibelle()));
        }

        if (searchDto.getObservation() != null) {
            spec = spec.and(hasObservation(searchDto.getObservation()));
        }

        if (searchDto.getActive() != null) {
            spec = spec.and(isActive(searchDto.getActive()));
        }

        if (searchDto.getType() != null) {
            spec = spec.and(hasType(searchDto.getType()));
        }

        return spec;
    }

    public static Specification<VirementInternational> encoursDecaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VirementInternational> encoursEncaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VirementInternational> outstandingBetween(Long banqueId, LocalDate from, LocalDate until, Type type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), type));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}