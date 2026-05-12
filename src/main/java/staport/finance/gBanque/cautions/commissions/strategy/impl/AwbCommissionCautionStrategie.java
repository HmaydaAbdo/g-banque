package staport.finance.gBanque.cautions.commissions.strategy.impl;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cautions.commissions.strategy.CommissionCautionStrategie;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;


/**
 * Stratégie de calcul des commissions sur cautions pour la banque AWB.
 *
 * <p>Règles métier AWB :
 * <ul>
 *   <li>Commission générée pour le mois de référence lui-même (pas de décalage M+1)</li>
 *   <li>Date d'émission = dernier jour du mois de référence</li>
 *   <li>Libellé = {@code "caution " + nature.code + " N° " + numeroCaution}</li>
 *   <li>Motif = {@code "CAUTION"} (stocké dans {@code observation})</li>
 *   <li>Montant HT = {@code (montant × taux × nombreJours) / 36000}
 *       où {@code taux} est en pourcentage (ex : 1.25 pour 1,25 %)
 *       et {@code nombreJours} = nombre de jours calendaires du mois de référence</li>
 *   <li>Arrondi : HALF_UP, 2 décimales</li>
 *   <li>Minimum : 20,00 MAD</li>
 *   <li>TVA non générée séparément (default genererTva = false)</li>
 * </ul>
 */
@Component
public class AwbCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "AWB";

    /** Minimum applicable sur le montant HT. */
    private static final BigDecimal MIN_COMMISSION_HT = new BigDecimal("20");

    /**
     * Diviseur = 360 × 100 — le facteur 360 est la base conventionnelle bancaire,
     * le facteur 100 convertit le taux de pourcentage vers décimal.
     */
    private static final BigDecimal DIVISEUR = new BigDecimal("36000");

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        LocalDate dateEmission = moisReference.atEndOfMonth();

        // Prorata entrée : 1er mois de la caution → de dateDePrise à fin de mois
        long nombreJours;
        if (caution.getDateDePrise() != null
                && YearMonth.from(caution.getDateDePrise()).equals(moisReference)) {
            nombreJours = ChronoUnit.DAYS.between(
                    caution.getDateDePrise(),
                    moisReference.atEndOfMonth().plusDays(1) // borne exclusive
            );
        } else {
            nombreJours = moisReference.lengthOfMonth();
        }

        return buildCommission(caution, nombreJours, dateEmission);
    }

    @Override
    public Commission calculerMainLevee(Caution caution, LocalDate dateDeMainLevee) {
        YearMonth moisMainLevee = YearMonth.from(dateDeMainLevee);
        LocalDate dateEmission  = dateDeMainLevee; // date effective de la main levée

        // Prorata sortie : du 1er du mois jusqu'à dateDeMainLevee (inclus)
        long nombreJours = ChronoUnit.DAYS.between(
                moisMainLevee.atDay(1),
                dateDeMainLevee.plusDays(1) // borne exclusive
        );

        return buildCommission(caution, nombreJours, dateEmission);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Formule commune : (montant × taux × jours) / 36000, minimum 20 MAD. */
    private Commission buildCommission(Caution caution, long nombreJours, LocalDate dateEmission) {
        BigDecimal montantCommission = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);

        if (montantCommission.compareTo(MIN_COMMISSION_HT) < 0) {
            montantCommission = MIN_COMMISSION_HT;
        }

        String libelle = "caution " + caution.getNature().getCode() + " N° " + caution.getNumeroCaution();

        Commission commission = new Commission();
        commission.setBanque(caution.getBanque());
        commission.setMontant(montantCommission);
        commission.setOperation(TransactionSource.CAUTION);
        commission.setDateEmission(dateEmission);
        commission.setLibelle(libelle);
        commission.setObservation("CAUTION");
        commission.setStatus(Status.PREVU);

        return commission;
    }
}
