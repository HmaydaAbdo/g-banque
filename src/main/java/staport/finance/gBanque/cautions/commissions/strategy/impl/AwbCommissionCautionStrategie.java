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
    public boolean appliquerProrata() {
        return true;
    }

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        // AWB : commission pour le mois de référence (pas de décalage)
        LocalDate dateEmission = moisReference.atEndOfMonth();

        // Prorata : si c'est le mois de création de la caution, on ne facture
        // que les jours réels entre dateDePrise (inclus) et fin de mois (inclus).
        // Les mois suivants → mois complet.
        long nombreJours;
        if (caution.getDateDePrise() != null
                && YearMonth.from(caution.getDateDePrise()).equals(moisReference)) {
            // borne exclusive → plusDays(1) pour inclure le dernier jour du mois
            nombreJours = ChronoUnit.DAYS.between(
                    caution.getDateDePrise(),
                    moisReference.atEndOfMonth().plusDays(1)
            );
        } else {
            nombreJours = moisReference.lengthOfMonth();
        }

        // Montant HT = (montant × taux × jours) / 36000
        // nombreJours = jours réels du mois (prorata 1er mois) ou lengthOfMonth()
        BigDecimal montantCommission = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);

        // Appliquer le minimum AWB (20 MAD)
        if (montantCommission.compareTo(MIN_COMMISSION_HT) < 0) {
            montantCommission = MIN_COMMISSION_HT;
        }

        montantCommission = montantCommission.setScale(2, RoundingMode.HALF_UP);

        // Libellé : "caution nature.code N° numeroCaution"
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
