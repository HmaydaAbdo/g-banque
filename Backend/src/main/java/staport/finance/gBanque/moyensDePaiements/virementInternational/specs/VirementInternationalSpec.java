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
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

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
     * Filtre générique par plage sur un champ de type date (bornes inclusives).
     * Les enregistrements dont le champ est {@code null} sont exclus dès qu'une borne est fournie.
     */
    private static Specification<VirementInternational> byDateRange(String field, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get(field), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get(field), from);
            return cb.lessThanOrEqualTo(root.get(field), to);
        };
    }

    /** Filtre par plage de dateEmission (remplace {@link #hasDateEmission} sur la liste principale). */
    public static Specification<VirementInternational> byDateEmissionRange(LocalDate from, LocalDate to) {
        return byDateRange("dateEmission", from, to);
    }

    /** Filtre par plage de dateOperation (renseignée lors du rapprochement). */
    public static Specification<VirementInternational> byDateOperationRange(LocalDate from, LocalDate to) {
        return byDateRange("dateOperation", from, to);
    }

    /** Filtre par plage de dateValeur (renseignée lors du rapprochement). */
    public static Specification<VirementInternational> byDateValeurRange(LocalDate from, LocalDate to) {
        return byDateRange("dateValeur", from, to);
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
     * Specification to filter VirementInternational by  status.
     * @return A Specification for the  status.
     */
    public static Specification<VirementInternational> notAnnule() {
        return (root, query, cb) ->
            cb.notEqual(root.get("statut"), StatutMoyenPaiement.ANNULE);
    }

    public static Specification<VirementInternational> hasStatut(StatutMoyenPaiement statut) {
        return (root, query, criteriaBuilder) -> {
            if (statut == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("statut"), statut);
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

        if (searchDto.banqueId() != null) {
            spec = spec.and(hasBanqueId(searchDto.banqueId()));
        }

        if (searchDto.beneficiaire() != null) {
            spec = spec.and(hasBeneficiaire(searchDto.beneficiaire()));
        }

        if (searchDto.emetteur() != null) {
            spec = spec.and(hasEmetteur(searchDto.emetteur()));
        }

        if (searchDto.montant() != null) {
            spec = spec.and(hasMontant(searchDto.montant()));
        }

        spec = spec.and(byDateEmissionRange(searchDto.dateEmissionFrom(), searchDto.dateEmissionTo()));
        spec = spec.and(byDateOperationRange(searchDto.dateOperationFrom(), searchDto.dateOperationTo()));
        spec = spec.and(byDateValeurRange(searchDto.dateValeurFrom(), searchDto.dateValeurTo()));

        if (searchDto.libelle() != null) {
            spec = spec.and(hasLibelle(searchDto.libelle()));
        }

        if (searchDto.observation() != null) {
            spec = spec.and(hasObservation(searchDto.observation()));
        }

        if (searchDto.statut() != null) {
            spec = spec.and(hasStatut(searchDto.statut()));
        }

        if (searchDto.type() != null) {
            spec = spec.and(hasType(searchDto.type()));
        }

        return spec;
    }

    public static Specification<VirementInternational> encoursDecaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VirementInternational> encoursEncaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<VirementInternational> outstandingBetween(Long banqueId, LocalDate from, LocalDate until, Type type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.equal(root.get("type"), type));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}