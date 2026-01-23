package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.specs;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.dtos.VirementErroneSearchDto;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.entities.VirementErrone;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

public class VirementErroneSpec {

    public static Specification<VirementErrone> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<VirementErrone> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }

    public static Specification<VirementErrone> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<VirementErrone> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<VirementErrone> buildSpecification(VirementErroneSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasStatus(dto.getStatus()));
    }

    /**
     * Creates a Specification to filter for virements erronés that are "outstanding."
     * Outstanding is defined as having a status of PREVU and an emission date that is in the past or present.
     *
     * @return A Specification for outstanding virements erronés.
     */
    public static Specification<VirementErrone> outstandingVirements() {
        return (root, query, cb) -> {
            // Condition 1 : statut = PREVU
            Specification<VirementErrone> hasStatusPrev = hasStatus(Status.PREVU);

            // Calcul de la fin du mois courant
            OffsetDateTime endOfMonth = OffsetDateTime.now()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999_999_999);

            // Condition 2 : dateEmission <= fin du mois courant
            Specification<VirementErrone> hasDateBeforeEndOfMonth = (rootDate, queryDate, cbDate) ->
                    cbDate.lessThanOrEqualTo(rootDate.get("dateEmission"), endOfMonth);

            // Combinaison des deux conditions
            return Specification
                    .where(hasStatusPrev)
                    .and(hasDateBeforeEndOfMonth)
                    .toPredicate(root, query, cb);
        };
    }
}