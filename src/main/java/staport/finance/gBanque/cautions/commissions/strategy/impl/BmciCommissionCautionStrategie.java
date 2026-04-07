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

/**
 * Stratégie de calcul des commissions sur cautions pour la banque BMCI.
 *
 * <p>Règles métier BMCI :
 * <ul>
 *   <li>Commission générée le mois suivant le mois de référence (mois X → commission mois X+1)</li>
 *   <li>Date d'émission = dernier jour du mois X+1</li>
 *   <li>Libellé = {@code nature.label + " " + numeroCaution}</li>
 *   <li>Motif = {@code "CAUTION"} (stocké dans {@code observation})</li>
 *   <li>Montant = {@code (montant × taux × nombreJours) / 36000}
 *       où {@code taux} est en pourcentage (ex : 1.25 pour 1,25 %)
 *       et {@code nombreJours} = nombre de jours calendaires du mois X+1</li>
 *   <li>Arrondi : HALF_UP, 2 décimales</li>
 * </ul>
 */
@Component
public class BmciCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "BMCI";
    private static final BigDecimal MIN_COMMISSION = new BigDecimal("70");

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
        YearMonth moisCommission = moisReference.plusMonths(1);
        int nombreJours          = moisCommission.lengthOfMonth();
        LocalDate dateEmission   = moisCommission.atEndOfMonth();

        // Montant = (montant × taux × jours) / 36000
        BigDecimal montantCommission = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);

        // appliquer le minimum
        if (montantCommission.compareTo(MIN_COMMISSION) < 0) {
            montantCommission = MIN_COMMISSION;
        }

        // Libellé : "nature.code numeroCaution"
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
