package staport.finance.gBanque.cautions.commissions.strategy.impl;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;
import staport.finance.gBanque.cautions.commissions.config.services.interfaces.ICautionCommissionConfigService;
import staport.finance.gBanque.cautions.commissions.strategy.CommissionCautionStrategie;
import staport.finance.gBanque.cautions.entities.Caution;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.enums.TypeCommission;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.transactions.enums.TransactionSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

/**
 * Stratégie de calcul des commissions sur cautions pour la banque BP — première banque facturée
 * <strong>par trimestre civil</strong>.
 *
 * <p>Règles métier BP :
 * <ul>
 *   <li>Commission trimestrielle générée en fin de trimestre civil (31/03, 30/06, 30/09, 31/12),
 *       date d'émission = dernier jour du trimestre, type {@code TRIMESTRIELLE}</li>
 *   <li>Montant = {@code (montant × taux × nombreJours) / diviseur}, {@code nombreJours} = jours
 *       calendaires du trimestre (90 à 92), arrondi HALF_UP à 2 décimales</li>
 *   <li>Aucun minimum (ligne de configuration BP à 0)</li>
 *   <li>Prorata d'entrée généré par le batch en fin de trimestre (pas à la création) : du jour de
 *       prise (inclus) au dernier jour du trimestre ; {@code dateEmission = dateDePrise}</li>
 *   <li>Prorata de sortie : du premier jour du trimestre (ou du jour de prise s'il est postérieur)
 *       à la date de main levée incluse ; {@code dateEmission = dateDeMainLevee}</li>
 *   <li>Montant TTC : aucune TVA générée séparément</li>
 * </ul>
 */
@Component
public class BpCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "BP";
    private static final int MOIS_PAR_TRIMESTRE = 3;

    private final ICautionCommissionConfigService commissionConfigService;

    public BpCommissionCautionStrategie(ICautionCommissionConfigService commissionConfigService) {
        this.commissionConfigService = commissionConfigService;
    }

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    @Override
    public int periodiciteMois() {
        return MOIS_PAR_TRIMESTRE;
    }

    @Override
    public TypeCommission typeCommissionPeriodique() {
        return TypeCommission.TRIMESTRIELLE;
    }

    @Override
    public boolean supporteProrataEntree() {
        return true;
    }

    @Override
    public boolean supporteProrataSortie() {
        return true;
    }

    /** Commission du trimestre civil dont {@code moisReference} est le dernier mois. */
    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        LocalDate debut = moisReference.minusMonths(MOIS_PAR_TRIMESTRE - 1L).atDay(1);
        LocalDate fin = moisReference.atEndOfMonth();
        long nombreJours = ChronoUnit.DAYS.between(debut, fin.plusDays(1));
        return buildCommission(caution, nombreJours, fin, " — Trimestre : " + nombreJours + " j");
    }

    /** Du jour de prise (inclus) à la fin du trimestre de prise ; émise à la date de prise. */
    @Override
    public Commission calculerProrataEntree(Caution caution) {
        LocalDate dateDePrise = caution.getDateDePrise();
        long nombreJours = ChronoUnit.DAYS.between(dateDePrise, finDuTrimestre(dateDePrise).plusDays(1));
        return buildCommission(caution, nombreJours, dateDePrise, " — Prorata entrée : " + nombreJours + " j");
    }

    /** Du début du trimestre (ou du jour de prise, s'il est postérieur) à la main levée incluse ; émise à la main levée. */
    @Override
    public Commission calculerMainLevee(Caution caution, LocalDate dateDeMainLevee) {
        LocalDate debutTrimestre = debutDuTrimestre(dateDeMainLevee);
        LocalDate debut = caution.getDateDePrise() != null && caution.getDateDePrise().isAfter(debutTrimestre)
                ? caution.getDateDePrise()
                : debutTrimestre;
        long nombreJours = ChronoUnit.DAYS.between(debut, dateDeMainLevee.plusDays(1));
        return buildCommission(caution, nombreJours, dateDeMainLevee, " — Prorata sortie : " + nombreJours + " j");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static LocalDate debutDuTrimestre(LocalDate date) {
        int premierMois = ((date.getMonthValue() - 1) / MOIS_PAR_TRIMESTRE) * MOIS_PAR_TRIMESTRE + 1;
        return LocalDate.of(date.getYear(), premierMois, 1);
    }

    private static LocalDate finDuTrimestre(LocalDate date) {
        return YearMonth.from(debutDuTrimestre(date)).plusMonths(MOIS_PAR_TRIMESTRE - 1L).atEndOfMonth();
    }

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
