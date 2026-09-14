package staport.finance.gBanque.impayees.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.impayees.dtos.ImpayeeSearchDto;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ImpayeeSpecifications {

    public static Specification<Impayee> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            // Updated field name here
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Impayee> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<Impayee> hasMotif(TransactionSource motif) {
        return (root, query, cb) -> {
            if (motif == null) {
                return null;
            }
            return cb.equal(root.get("motif"), motif);
        };
    }

    public static Specification<Impayee> hasNumero(String numero) {
        return (root, query, cb) -> {
            if (numero == null || numero.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("numero")), "%" + numero.toLowerCase() + "%");
        };
    }

    public static Specification<Impayee> hasStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Impayee> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Impayee> hasType(Type type) {
        return (root, query, cb) -> {
            if (type == null) {
                return null;
            }
            return cb.equal(root.get("type"), type);
        };
    }

    public static Specification<Impayee> buildSpecification(ImpayeeSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasMotif(dto.motif()))
                .and(hasNumero(dto.numero()))
                .and(hasType(dto.type()))
                .and(hasStatus(dto.status()))
                .and(byDateEmissionRange(dto.dateEmissionDebut(), dto.dateEmissionFin()))
                .and(byDateOperationRange(dto.dateOperationDebut(), dto.dateOperationFin()))
                .and(byDateValeurRange(dto.dateValeurDebut(), dto.dateValeurFin()));
    }

    /** Filtre générique par plage de dates sur un champ {@link LocalDate} de l'entité. */
    private static Specification<Impayee> byDateRange(String field, LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut != null && fin != null) return cb.between(root.get(field), debut, fin);
            if (debut != null) return cb.greaterThanOrEqualTo(root.get(field), debut);
            if (fin != null) return cb.lessThanOrEqualTo(root.get(field), fin);
            return null;
        };
    }

    public static Specification<Impayee> byDateEmissionRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateEmission", debut, fin);
    }

    public static Specification<Impayee> byDateOperationRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateOperation", debut, fin);
    }

    public static Specification<Impayee> byDateValeurRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateValeur", debut, fin);
    }

    public static Specification<Impayee> outstandingUntil(LocalDate until, Type type) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.equal(root.get("type"), type),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<Impayee> outstandingBetween(LocalDate from, LocalDate until, Type type) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.equal(root.get("type"), type),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}
