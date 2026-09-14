package staport.finance.gBanque.cautions.commissions.strategy.impl;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;
import staport.finance.gBanque.cautions.commissions.config.services.interfaces.ICautionCommissionConfigService;
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

    private final ICautionCommissionConfigService commissionConfigService;

    public SahamBankCommissionCautionStrategie(ICautionCommissionConfigService commissionConfigService) {
        this.commissionConfigService = commissionConfigService;
    }

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    /** SAHAM BANK applique le prorata sur le premier mois de la caution (à la création). */
    @Override
    public boolean supporteProrataEntree() {
        return true;
    }

    /**
     * SAHAM BANK facture le mois suivant (M+1) :
     * la commission batch émise fin M couvre les jours du mois M+1.
     */
    @Override
    public boolean facturerMoisSuivant() {
        return true;
    }

    // supporteProrataSortie() → false par défaut (mois entier à la clôture)
    // genererTva()            → false par défaut (pas de TVA)
    // executerEnDebutMois()   → false par défaut (génération fin de mois)

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        // Toujours M+1 — le prorata d'entrée est géré à la création via calculerProrataEntree()
        YearMonth moisCommission = moisReference.plusMonths(1);
        long nombreJours         = moisCommission.lengthOfMonth();
        String suffixeLibelle    = " — Mois entier : " + nombreJours + " j";

        return buildCommission(caution, nombreJours, moisReference.atEndOfMonth(), suffixeLibelle);
    }

    /**
     * Prorata d'entrée SAHAM BANK — calculé à la création de la caution.
     * <p>Jours : de {@code dateDePrise} jusqu'à la fin du mois d'entrée (incluse).
     * {@code dateEmission = dateDePrise} — différent du 31/M du batch, pas de conflit.
     */
    @Override
    public Commission calculerProrataEntree(Caution caution) {
        LocalDate dateDePrise = caution.getDateDePrise();
        YearMonth moisEntree  = YearMonth.from(dateDePrise);
        long nombreJours = ChronoUnit.DAYS.between(
                dateDePrise,
                moisEntree.atEndOfMonth().plusDays(1) // borne exclusive
        );
        String suffixeLibelle = " — Prorata entrée : " + nombreJours + " j";
        return buildCommission(caution, nombreJours, dateDePrise, suffixeLibelle);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Commission buildCommission(Caution caution, long nombreJours,
                                       LocalDate dateEmission, String suffixeLibelle) {
        CautionCommissionConfigResponseDTO config =
                commissionConfigService.resolveOuLeverException(caution.getBanque().getNom());

        BigDecimal montantCommission = caution.getMontant()
                .multiply(caution.getTaux())
                .multiply(BigDecimal.valueOf(nombreJours))
                .divide(config.diviseur(), 2, RoundingMode.HALF_UP);

        if (montantCommission.compareTo(config.montantMinimum()) < 0) {
            montantCommission = config.montantMinimum();
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
