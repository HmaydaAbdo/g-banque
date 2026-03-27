package staport.finance.gBanque.moyensDePaiements.lcn.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.NatureLcn;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class LcnSpecification {


    public static Specification<Lcn> hasLigneFonctionnelle(Long ligneFonctionnelleId) {
        return (root, query, criteriaBuilder) -> {
            if (ligneFonctionnelleId == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("ligneFonctionnelle").get("id"), ligneFonctionnelleId);
        };
    }


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

    public static Specification<Lcn> hasNatureLcn(NatureLcn natureLcn) {
        return (root, query, criteriaBuilder) -> {
            if (natureLcn == null ) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("natureLcn"), natureLcn);

        };
    }

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

    public static Specification<Lcn> hasDateEcheance(LocalDate dateEcheance) {
        return (root, query, criteriaBuilder) -> {
            if (dateEcheance == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEcheance"), dateEcheance);
        };
    }

    public static Specification<Lcn> hasDateEmission(LocalDate dateEmission) {
        return (root, query, criteriaBuilder) -> {
            if (dateEmission == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEmission"), dateEmission);
        };
    }

    public static Specification<Lcn> hasEmetteur(String emetteur) {
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

    public static Specification<Lcn> encoursDecaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (banqueId == null) return criteriaBuilder.disjunction();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Remplace encoursEncaissement — until en paramètre (le filtre date manquait dans l'original!)
    public static Specification<Lcn> encoursEncaissement(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            if (banqueId == null) return criteriaBuilder.disjunction();

            // 1. Filtres de base communs
            List<Predicate> basePredicates = new ArrayList<>();
            basePredicates.add(criteriaBuilder.isTrue(root.get("active")));
            basePredicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));
            basePredicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));

            // 2. Logique conditionnelle selon la Nature
            // Condition A : Nature ENCAISSEMENT -> Se base sur dateEcheance
            Predicate isEncaissementStandard = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("natureLcn"), NatureLcn.ENCAISSEMENT),
                    criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), until)
            );

            // Condition B : Nature ESCOMPTE -> Se base sur dateEmission
            Predicate isEscompte = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("natureLcn"), NatureLcn.ESCOMPTE),
                    criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until)
            );

            // On combine les filtres de base avec (Condition A OU Condition B)
            return criteriaBuilder.and(
                    criteriaBuilder.and(basePredicates.toArray(new Predicate[0])),
                    criteriaBuilder.or(isEncaissementStandard, isEscompte)
            );
        };
    }
    // Nouveau — pour le recap
    public static Specification<Lcn> outstandingBetween(Long banqueId, LocalDate from, LocalDate until, Type type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (banqueId == null) return criteriaBuilder.disjunction();
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("type"), type));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEcheance"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEcheance"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}