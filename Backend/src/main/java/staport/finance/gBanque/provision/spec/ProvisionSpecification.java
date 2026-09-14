package staport.finance.gBanque.provision.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.provision.dtos.ProvisionSearchDto;
import staport.finance.gBanque.provision.entities.Provision;
import staport.finance.gBanque.transactions.enums.SensOperation;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Specification de recherche pour le module Provision
 */
public class ProvisionSpecification {

    public static Specification<Provision> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Provision> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<Provision> hasStatus(Status status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Provision> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Provision> hasMotif(TransactionSource motif) {
        return (root, query, cb) -> {
            if (motif == null) {
                return null;
            }
            return cb.equal(root.get("motif"), motif);
        };
    }

    public static Specification<Provision> hasSens(SensOperation sens) {
        return (root, query, cb) -> {
            if (sens == null) {
                return null;
            }
            return cb.equal(root.get("sens"), sens);
        };
    }

    public static Specification<Provision> byDateRange(String field, LocalDate debut, LocalDate fin) {
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

    public static Specification<Provision> byDateEmissionRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateEmission", debut, fin);
    }

    public static Specification<Provision> byDateOperationRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateOperation", debut, fin);
    }

    public static Specification<Provision> byDateValeurRange(LocalDate debut, LocalDate fin) {
        return byDateRange("dateValeur", debut, fin);
    }

    public static Specification<Provision> searchByCriteria(ProvisionSearchDto dto) {
        return Specification
                .where(hasBanque(dto.banqueId()))
                .and(hasLibelle(dto.libelle()))
                .and(hasStatus(dto.status()))
                .and(hasMotif(dto.motif()))
                .and(hasSens(dto.sens()))
                .and(byDateEmissionRange(dto.dateEmissionDebut(), dto.dateEmissionFin()))
                .and(byDateOperationRange(dto.dateOperationDebut(), dto.dateOperationFin()))
                .and(byDateValeurRange(dto.dateValeurDebut(), dto.dateValeurFin()));
    }

    public static Specification<Provision> outstandingUntil(LocalDate until) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("status"), Status.PREVU),
                cb.lessThanOrEqualTo(root.get("dateEmission"), until)
        );
    }
}
