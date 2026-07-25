package staport.finance.gBanque.moyensDePaiements.cheques.specs;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

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


    public static Specification<Cheque> hasEmetteur(String emetteur) {
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
     * Specification to filter Cheques by their  status.
     * @param statut The active status .
     * @return A Specification for the  status.
     */
    public static Specification<Cheque> hasStatut(StatutMoyenPaiement statut) {
        return (root, query, criteriaBuilder) -> {
            if (statut == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("statut"), statut);
        };
    }

    public static Specification<Cheque> notAnnule() {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.notEqual(root.get("statut"), StatutMoyenPaiement.ANNULE);
    }

    public static Specification<Cheque> hasType(Type type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    public static Specification<Cheque> hasMontant(BigDecimal montant) {
        return (root, query, criteriaBuilder) -> {
            if (montant == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("montant"), montant);
        };
    }



    public static Specification<Cheque> hasDateEmission(LocalDate dateEmission) {
        return (root, query, criteriaBuilder) -> {
            if (dateEmission == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("dateEmission"), dateEmission);
        };
    }

    /**
     * Filtre générique par plage sur un champ de type date (bornes inclusives).
     * Les enregistrements dont le champ est {@code null} sont exclus dès qu'une borne est fournie.
     */
    private static Specification<Cheque> byDateRange(String field, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            if (from != null && to != null) return cb.between(root.get(field), from, to);
            if (from != null) return cb.greaterThanOrEqualTo(root.get(field), from);
            return cb.lessThanOrEqualTo(root.get(field), to);
        };
    }

    /** Filtre par plage de dateEmission (remplace {@link #hasDateEmission} sur les listes principales). */
    public static Specification<Cheque> byDateEmissionRange(LocalDate from, LocalDate to) {
        return byDateRange("dateEmission", from, to);
    }

    /** Filtre par plage de dateEcheance. */
    public static Specification<Cheque> byDateEcheanceRange(LocalDate from, LocalDate to) {
        return byDateRange("dateEcheance", from, to);
    }

    /** Filtre par plage de dateOperation (renseignée lors du rapprochement). */
    public static Specification<Cheque> byDateOperationRange(LocalDate from, LocalDate to) {
        return byDateRange("dateOperation", from, to);
    }

    /** Filtre par plage de dateValeur (renseignée lors du rapprochement). */
    public static Specification<Cheque> byDateValeurRange(LocalDate from, LocalDate to) {
        return byDateRange("dateValeur", from, to);
    }

    public static Specification<Cheque> encoursDecaissements(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (banqueId == null) return criteriaBuilder.disjunction();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.DECAISSEMENT));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Cheque> encoursEncaissements(Long banqueId, LocalDate until) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (banqueId == null) return criteriaBuilder.disjunction();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("type"), Type.ENCAISSEMENT));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Cheque> outstandingBetween(Long banqueId, LocalDate from, LocalDate until, Type type) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (banqueId == null) return criteriaBuilder.disjunction();
            predicates.add(criteriaBuilder.equal(root.get("statut"), StatutMoyenPaiement.ENCOURS));
            predicates.add(criteriaBuilder.equal(root.get("type"), type));
            predicates.add(criteriaBuilder.equal(root.get("banque").get("id"), banqueId));
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dateEmission"), from));
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dateEmission"), until));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

}