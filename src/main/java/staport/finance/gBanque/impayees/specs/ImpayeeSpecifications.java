package staport.finance.gBanque.impayees.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.impayees.dtos.ImpayeeSearchDto;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

public class ImpayeeSpecifications {

    public static Specification<Impayee> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banqueEncaissement").get("id"), banqueId);
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

    public static Specification<Impayee> hasMotif(Enum<?> motif) {
        return (root, query, cb) -> {
            if (motif == null) {
                return null;
            }
            return cb.equal(root.get("motif"), motif);
        };
    }

    public static Specification<Impayee> hasStatus(Enum<?> status) {
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

    public static Specification<Impayee> buildSpecification(ImpayeeSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueEncaissementId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasMotif(dto.getMotif()))
                .and(hasStatus(dto.getStatus()));
    }

    /**
     * Creates a Specification to filter for impayees that are "outstanding."
     * Outstanding is defined as having a status of PREVU and an emission date that is in the past or present.
     *
     * @return A Specification for outstanding impayees.
     */
    public static Specification<Impayee> outstandingImpayees() {
        return (root, query, cb) -> {
            // Condition 1 : statut = PREVU
            Specification<Impayee> hasStatusPrev = hasStatus(Status.PREVU);

            // Calcul de la fin du mois courant (dernier jour à 23h59m59s999999999)
            OffsetDateTime endOfMonth = OffsetDateTime.now()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);

            // Condition 2 : dateEmission <= fin du mois courant
            Specification<Impayee> hasDateBeforeEndOfMonth = (rootDate, queryDate, cbDate) ->
                    cbDate.lessThanOrEqualTo(rootDate.get("dateEmission"), endOfMonth);

            // Combinaison des deux conditions avec AND logique
            return Specification
                    .where(hasStatusPrev)
                    .and(hasDateBeforeEndOfMonth)
                    .toPredicate(root, query, cb);
        };
    }
}