package staport.finance.gBanque.moyensDePaiements.miseADisposition.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

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

    public static Specification<MiseADisposition> hasStatut(StatutMoyenPaiement statut) {
        return (root, query, criteriaBuilder) -> {
            if (statut == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("statut"), statut);
        };
    }

    public static Specification<MiseADisposition> notAnnule() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.notEqual(root.get("statut"), StatutMoyenPaiement.ANNULE);
    }

    public static Specification<MiseADisposition> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
        };
    }

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

        if (searchDto.getStatut() != null) {
            spec = spec.and(hasStatut(searchDto.getStatut()));
        }

        return spec;
    }


    public static Specification<MiseADisposition> encoursDecaissement(Long banqueId, LocalDate until) {
        return (root, query, cb) -> {
            if (banqueId == null) return cb.disjunction();
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(cb.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(cb.equal(root.get("banque").get("id"), banqueId));
            predicates.add(cb.lessThanOrEqualTo(root.get("dateEmission"), until));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<MiseADisposition> encoursEncaissement(Long banqueId, LocalDate until) {
        return (root, query, cb) -> {
            if (banqueId == null) return cb.disjunction();
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(cb.equal(root.get("type"), Type.ENCAISSEMENT));
            predicates.add(cb.equal(root.get("banque").get("id"), banqueId));
            predicates.add(cb.lessThanOrEqualTo(root.get("dateEmission"), until));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }


    public static Specification<MiseADisposition> outstandingBetween(Long banqueId, LocalDate from, LocalDate until, Type type) {
        return (root, query, cb) -> {
            if (banqueId == null) return cb.disjunction();
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(cb.equal(root.get("type"), type));
            predicates.add(cb.equal(root.get("banque").get("id"), banqueId));
            predicates.add(cb.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(cb.lessThanOrEqualTo(root.get("dateEmission"), until));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}