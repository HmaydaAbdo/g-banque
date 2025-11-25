package staport.finance.gBanque.prelevements.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prelevements.dtos.PrelevementSearchDto;
import staport.finance.gBanque.prelevements.entitie.Prelevement;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;

public class PrelevementSpec {

    public static Specification<Prelevement> hasBanque(Long banqueId) {
        return (root, query, cb) -> {
            if (banqueId == null) {
                return null;
            }
            return cb.equal(root.get("banque").get("id"), banqueId);
        };
    }

    public static Specification<Prelevement> hasLibelle(String libelle) {
        return (root, query, cb) -> {
            if (libelle == null || libelle.trim().isEmpty()) {
                return null;
            }
            return cb.like(cb.lower(root.get("libelle")), "%" + libelle.toLowerCase() + "%");
        };
    }


    public static Specification<Prelevement> hasStatus(Enum<?> status) {
        return (root, query, cb) -> {
            if (status == null) {
                return null;
            }
            return cb.equal(root.get("status"), status);
        };
    }

    public static Specification<Prelevement> hasMontant(BigDecimal montant) {
        return (root, query, cb) -> {
            if (montant == null) {
                return null;
            }
            return cb.equal(root.get("montant"), montant);
        };
    }

    public static Specification<Prelevement> buildSpecification(PrelevementSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasStatus(dto.getStatus()));
    }

    public static Specification<Prelevement> outstandingPrelevement() {
        return (root, query, cb) -> {
            // Condition 1 : statut = PREVU
            Specification<Prelevement> hasStatusPrev = hasStatus(Status.PREVU);

            // Fin du mois courant (dernier jour à 23h59m59s999999999)
            OffsetDateTime endOfMonth = OffsetDateTime.now()
                    .with(TemporalAdjusters.lastDayOfMonth())
                    .withHour(23)
                    .withMinute(59)
                    .withSecond(59)
                    .withNano(999_999_999);

            // Condition 2 : dateEmission <= fin du mois courant
            Specification<Prelevement> hasDateBeforeEndOfMonth = (rootDate, queryDate, cbDate) ->
                    cbDate.lessThanOrEqualTo(rootDate.get("dateEmission"), endOfMonth);

            // Combinaison des deux conditions avec un AND logique
            return Specification
                    .where(hasStatusPrev)
                    .and(hasDateBeforeEndOfMonth)
                    .toPredicate(root, query, cb);
        };
    }
}
