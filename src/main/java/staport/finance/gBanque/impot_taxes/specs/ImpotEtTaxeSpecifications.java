package staport.finance.gBanque.impot_taxes.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeSearchDto;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ImpotEtTaxeSpecifications {

    private ImpotEtTaxeSpecifications() {}

    public static Specification<ImpotEtTaxe> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<ImpotEtTaxe> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<ImpotEtTaxe> hasMotif(Enum<?> motif) {
        return (root, query, cb) -> {
            if (motif == null) {
                return null;
            }
            return cb.equal(root.get("motif"), motif);
        };
    }

    public static Specification<ImpotEtTaxe> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<ImpotEtTaxe> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<ImpotEtTaxe> searchByCriteria(ImpotEtTaxeSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasMotif(dto.motif()))
                .and(hasMontant(dto.montant()))
                .and(hasStatus(dto.status()))
                .and(byDateEmissionRange(dto.dateEmissionDebut(), dto.dateEmissionFin()))
                .and(byDateOperationRange(dto.dateOperationDebut(), dto.dateOperationFin()))
                .and(byDateValeurRange(dto.dateValeurDebut(), dto.dateValeurFin()));
    }

    /** Filtre générique par plage de dates sur un champ {@link LocalDate} de l'entité. */
    private static Specification<ImpotEtTaxe> byDateRange(String field, LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut != null && fin != null) return cb.between(root.get(field), debut, fin);
            if (debut != null) return cb.greaterThanOrEqualTo(root.get(field), debut);
            if (fin != null) return cb.lessThanOrEqualTo(root.get(field), fin);
            return null;
        };
    }

    public static Specification<ImpotEtTaxe> byDateEmissionRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateEmission", debut, fin);
    }

    public static Specification<ImpotEtTaxe> byDateOperationRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateOperation", debut, fin);
    }

    public static Specification<ImpotEtTaxe> byDateValeurRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateValeur", debut, fin);
    }

    public static Specification<ImpotEtTaxe> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<ImpotEtTaxe> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}
