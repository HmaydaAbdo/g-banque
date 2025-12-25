package staport.finance.gBanque.moyensDePaiements.lcn.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.NatureLcn;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public final class LcnSpecification {

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

    public static Specification<Lcn> hasDateEcheance(OffsetDateTime dateEcheance) {
        return (root, query, criteriaBuilder) -> {
            if (dateEcheance == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEcheance"), dateEcheance);
        };
    }

    public static Specification<Lcn> hasDateEmission(OffsetDateTime dateEmission) {
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

    public static Specification<Lcn> encoursDecaissement(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            OffsetDateTime now = OffsetDateTime.now();

            // 1. Le statut du LCN doit être actif.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));

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
    public static Specification<Lcn> encoursEncaissement(Long banqueId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Le statut du LCN doit être actif.
            predicates.add(criteriaBuilder.isTrue(root.get("active")));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));

            // 2. Le LCN doit appartenir à la banque spécifiée par son ID.
            if (banqueId != null) {
                predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            } else {
                return criteriaBuilder.disjunction();
            }



            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}