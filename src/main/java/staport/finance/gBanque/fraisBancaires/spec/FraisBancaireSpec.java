package staport.finance.gBanque.fraisBancaires.spec;

import org.springframework.data.jpa.domain.Specification;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

public class FraisBancaireSpec {


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


    public static Specification<FraisBancaire> hasStatus(Enum<?> status) {
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


    public static Specification<FraisBancaire> buildSpecification(FraisBancaireSearchDto dto) {
        return Specification
                .where(hasBanque(dto.getBanqueId()))
                .and(hasLibelle(dto.getLibelle()))
                .and(hasStatus(dto.getStatus()));
    }

    /**
     * Creates a Specification to filter for fraisBancaires that are "outstanding."
     * Outstanding is defined as having a status of PREVU and an emission date that is in the past or present.
     *
     * @return A Specification for outstanding frais bancaires.
     */
    public static Specification<FraisBancaire> outstandingFraisBancaires() {
        return (root, query, cb) -> {
            // Condition 1 : statut = PREVU
            Specification<FraisBancaire> hasStatusPrev = hasStatus(Status.PREVU);

            // Fin du mois courant : dernier jour à 23h59m59s999999999
            LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

            // Condition 2 : dateEmission <= fin du mois courant
            Specification<FraisBancaire> hasDateBeforeEndOfMonth = (rootDate, queryDate, cbDate) ->
                    cbDate.lessThanOrEqualTo(rootDate.get("dateEmission"), endOfMonth);

            // Combinaison des deux conditions avec un AND logique
            return Specification
                    .where(hasStatusPrev)
                    .and(hasDateBeforeEndOfMonth)
                    .toPredicate(root, query, cb);
        };
    }


}
