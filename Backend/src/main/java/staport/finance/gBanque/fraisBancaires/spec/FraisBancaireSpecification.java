package staport.finance.gBanque.fraisBancaires.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;

public class FraisBancaireSpecification {

    public static Specification<FraisBancaire> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<FraisBancaire> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<FraisBancaire> hasStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<FraisBancaire> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<FraisBancaire> byDateRange(String field, LocalDate debut, LocalDate fin) {
        return (root, query, cb) -> {
            if (debut == null && fin == null) {
                return cb.conjunction();
            }
            if (debut != null && fin != null) {
                return cb.between(root.get(field), debut, fin);
            } else if (debut != null) {
                return cb.greaterThanOrEqualTo(root.get(field), debut);
            } else {
                return cb.lessThanOrEqualTo(root.get(field), fin);
            }
        };
    }

    public static Specification<FraisBancaire> byDateEmissionRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateEmission", debut, fin);
    }

    public static Specification<FraisBancaire> byDateOperationRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateOperation", debut, fin);
    }

    public static Specification<FraisBancaire> byDateValeurRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateValeur", debut, fin);
    }

    public static Specification<FraisBancaire> searchByCriteria(FraisBancaireSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasStatus(dto.status()))
                .and(byDateEmissionRange(dto.dateEmissionDebut(), dto.dateEmissionFin()))
                .and(byDateOperationRange(dto.dateOperationDebut(), dto.dateOperationFin()))
                .and(byDateValeurRange(dto.dateValeurDebut(), dto.dateValeurFin()));
    }

    public static Specification<FraisBancaire> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }

    public static Specification<FraisBancaire> outstandingBetween(LocalDate from, LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.greaterThanOrEqualTo(root.get("dateEmission"), from),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}
