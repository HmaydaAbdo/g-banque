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
 * Stratégie de calcul des commissions sur cautions pour la banque CDM
 * (Crédit du Maroc).
 *
 * <p>Règles métier CDM :
 * <ul>
 *   <li>Commission générée pour le mois de référence lui-même (pas de décalage M+1)</li>
 *   <li>Date d'émission = <strong>premier jour</strong> du mois de référence</li>
 *   <li>Libellé = {@code "caution " + nature.code + " N° " + numeroCaution}</li>
 *   <li>Motif = {@code "CAUTION"} (stocké dans {@code observation})</li>
 *   <li>Montant = {@code (montant × taux × nombreJours) / 36000}
 *       où {@code taux} est en pourcentage (ex : 1.25 pour 1,25 %)
 *       et {@code nombreJours} = nombre de jours calendaires du mois de référence</li>
 *   <li>Arrondi : HALF_UP, 2 décimales</li>
 *   <li>Minimum : 70,00 MAD</li>
 * </ul>
 *
 * <p>Différence clé vs BMCI : CDM émet la commission au <em>début</em> du mois
 * (1er jour) alors que BMCI émet à la fin du mois suivant.
 */
@Component
public class CdmCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "CDM";

    /** Minimum applicable sur le montant HT avant ajout de la TVA. */
    private static final BigDecimal MIN_COMMISSION_HT = new BigDecimal("70");

    /**
     * Diviseur = 360 × 100 — base conventionnelle bancaire (360 jours),
     * facteur 100 pour convertir le taux de pourcentage en décimal.
     */
    private static final BigDecimal DIVISEUR = new BigDecimal("36000");

    /**
     * Facteur TTC : 1 + taux TVA (10 %).
     * Le montant de la commission CDM est stocké TTC — aucune TVA séparée n'est générée.
     */
    private static final BigDecimal FACTEUR_TTC = new BigDecimal("1.10");

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    /**
     * CDM : le montant de la commission inclut déjà la TVA.
     * Aucune entrée ImpotEtTaxe ne doit être générée séparément.
     */
    @Override
    public boolean genererTva() {
        return false;
    }

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        // CDM : commission pour le mois de référence, émise le 1er du mois
        int nombreJours        = moisReference.lengthOfMonth();
        LocalDate dateEmission = moisReference.atEndOfMonth();

        // Montant HT = (montant × taux × jours) / 36000
        BigDecimal montantHt = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(DIVISEUR, 2, RoundingMode.HALF_UP);

        // Appliquer le minimum CDM (70 MAD) sur le HT
        if (montantHt.compareTo(MIN_COMMISSION_HT) < 0) {
            montantHt = MIN_COMMISSION_HT;
        }

        // Montant TTC = HT × 1.10 — la TVA est incluse dans la commission CDM
        BigDecimal montantCommission = montantHt
                .multiply(FACTEUR_TTC)
                .setScale(2, RoundingMode.HALF_UP);

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
