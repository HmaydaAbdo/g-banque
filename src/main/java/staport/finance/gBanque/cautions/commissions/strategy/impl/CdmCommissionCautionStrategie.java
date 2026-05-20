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
 * Stratégie de calcul des commissions sur cautions pour la banque CDM
 * (Crédit du Maroc).
 *
 * <p>Règles métier CDM :
 * <ul>
 *   <li>Commission générée pour le mois de référence lui-même (pas de décalage M+1)</li>
 *   <li>Date d'émission = <strong>premier jour</strong> du mois de référence</li>
 *   <li>Libellé = {@code "caution " + nature.code + " N° " + numeroCaution}</li>
 *   <li>Motif = {@code "CAUTION"} (stocké dans {@code observation})</li>
 *   <li>Montant HT = {@code (montant × taux × nombreJours) / 36000}
 *       où {@code taux} est en pourcentage (ex : 1.25 pour 1,25 %)
 *       et {@code nombreJours} = nombre de jours calendaires du mois de référence</li>
 *   <li>Arrondi : HALF_UP, 2 décimales</li>
 *   <li>Minimum : 70,00 MAD</li>
 *   <li>TVA non générée séparément (default genererTva = false)</li>
 *   <li>Exécution en début de mois (1er jour)</li>
 * </ul>
 */
@Component
public class CdmCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "CDM";

    /** Minimum applicable sur le montant HT. */
    private static final BigDecimal MIN_COMMISSION_HT = new BigDecimal("70");

    /**
     * Diviseur = 360 × 100 — base conventionnelle bancaire (360 jours),
     * facteur 100 pour convertir le taux de pourcentage en décimal.
     */
    private static final BigDecimal DIVISEUR = new BigDecimal("36000");

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    /**
     * CDM : commission émise le 1er jour du mois → exécution en début de mois.
     */
    @Override
    public boolean executerEnDebutMois() {
        return true;
    }

    /** CDM applique le prorata sur le premier mois de la caution. */
    @Override
    public boolean supporteProrataEntree() {
        return true;
    }

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        LocalDate dateEmission = moisReference.atDay(1);

        // Prorata entrée : 1er mois de la caution → de dateDePrise à fin de mois
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
            nombreJours    = moisReference.lengthOfMonth();
            suffixeLibelle = " — Mois entier : " + nombreJours + " j";
        }

        // Montant HT = (montant × taux × jours) / 36000
        BigDecimal montantCommission = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);

        // Appliquer le minimum CDM (70 MAD)
        if (montantCommission.compareTo(MIN_COMMISSION_HT) < 0) {
            montantCommission = MIN_COMMISSION_HT;
        }

        montantCommission = montantCommission.setScale(2, RoundingMode.HALF_UP);

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
