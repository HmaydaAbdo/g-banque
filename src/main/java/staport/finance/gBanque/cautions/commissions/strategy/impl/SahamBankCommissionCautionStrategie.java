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
 * Stratégie de calcul des commissions sur cautions pour SAHAM BANK.
 *
 * <p>Règles métier SAHAM BANK :
 * <ul>
 *   <li>Génération en <strong>fin de mois</strong> (dernier jour du mois de référence)</li>
 *   <li>Commission émise pour le <strong>mois suivant</strong> (mois X → commission mois X+1),
 *       sauf premier mois (prorata entrée)</li>
 *   <li>Date d'émission = dernier jour du mois de référence</li>
 *   <li>Prorata à l'entrée : premier mois calculé sur les jours réels de moisReference</li>
 *   <li>Pas de prorata à la clôture : mois entier du mois de la main levée</li>
 *   <li>Minimum : 55,00 MAD</li>
 *   <li>Formule : {@code (montant × taux × nombreJours) / 36 000}</li>
 *   <li>Arrondi : HALF_UP, 2 décimales</li>
 *   <li>Pas de TVA générée séparément</li>
 * </ul>
 */
@Component
public class SahamBankCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "SAHAM BANK";

    private static final BigDecimal MIN_COMMISSION = new BigDecimal("55");
    private static final BigDecimal DIVISEUR       = new BigDecimal("36000");

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    /** SAHAM BANK applique le prorata sur le premier mois de la caution. */
    @Override
    public boolean supporteProrataEntree() {
        return true;
    }

    // supporteProrataSortie() → false par défaut (mois entier à la clôture)
    // genererTva()            → false par défaut (pas de TVA)
    // executerEnDebutMois()   → false par défaut (génération fin de mois)

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        LocalDate dateEmission = moisReference.atEndOfMonth();

        // Prorata entrée : 1er mois → jours réels de moisReference
        // Mois suivants  : jours de moisReference+1 (décalage M+1)
        long nombreJours;
        String suffixeLibelle;
        if (caution.getDateDePrise() != null
                && YearMonth.from(caution.getDateDePrise()).equals(moisReference)) {
            nombreJours = ChronoUnit.DAYS.between(
                    caution.getDateDePrise(),
                    moisReference.atEndOfMonth().plusDays(1) // borne exclusive
            );
            suffixeLibelle = " — Prorata entrée : " + nombreJours + " j";
        } else {
            YearMonth moisCommission = moisReference.plusMonths(1);
            nombreJours    = moisCommission.lengthOfMonth();
            suffixeLibelle = " — Mois entier : " + nombreJours + " j";
        }

        BigDecimal montantCommission = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);

        if (montantCommission.compareTo(MIN_COMMISSION) < 0) {
            montantCommission = MIN_COMMISSION;
        }

        String libelle = "caution " + caution.getNature().getCode()
                + " N° " + caution.getNumeroCaution() + suffixeLibelle;

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
