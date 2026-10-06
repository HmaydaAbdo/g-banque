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
 * Stratégie de calcul des commissions sur cautions pour la banque AWB.
 *
 * <p>Règles métier AWB :
 * <ul>
 *   <li>Commission générée pour le mois de référence lui-même (pas de décalage M+1)</li>
 *   <li>Date d'émission = dernier jour du mois de référence</li>
 *   <li>Libellé = {@code "caution " + nature.code + " N° " + numeroCaution}</li>
 *   <li>Motif = {@code "CAUTION"} (stocké dans {@code observation})</li>
 *   <li>Montant HT = {@code (montant × taux × nombreJours) / diviseur}
 *       où {@code taux} est en pourcentage (ex : 1.25 pour 1,25 %), {@code nombreJours} = nombre
 *       de jours calendaires du mois de référence, et {@code diviseur} est résolu via
 *       {@link ICautionCommissionConfigService} (base conventionnelle bancaire habituelle :
 *       36000)</li>
 *   <li>Arrondi : HALF_UP, 2 décimales</li>
 *   <li>Minimum : résolu via {@link ICautionCommissionConfigService} (paramétrable par banque,
 *       administrable via l'UI — {@code conventions/configuration.md})</li>
 *   <li>TVA non générée séparément (default genererTva = false)</li>
 *   <li>Prorata d'entrée géré par le <strong>batch</strong> (pas à la création) —
 *       {@code supporteProrataEntree() = false}</li>
 *   <li>Le jour de délivrance ({@code dateDePrise}) n'est <strong>pas facturé</strong> :
 *       le décompte des jours du premier mois commence le lendemain de la date de prise
 *       (prorata d'entrée et cas prise/main levée dans le même mois)</li>
 *   <li>Prorata de sortie actif — {@code supporteProrataSortie() = true}</li>
 * </ul>
 */
@Component
public class AwbCommissionCautionStrategie implements CommissionCautionStrategie {

    private static final String NOM_BANQUE = "AWB";

    private final ICautionCommissionConfigService commissionConfigService;

    public AwbCommissionCautionStrategie(ICautionCommissionConfigService commissionConfigService) {
        this.commissionConfigService = commissionConfigService;
    }

    @Override
    public String nomBanque() {
        return NOM_BANQUE;
    }

    /**
     * AWB gère le prorata d'entrée dans le batch (pas à la création de la caution).
     * Pas de gap possible car AWB facture le mois courant (pas M+1) :
     * le batch de fin de mois calcule directement le prorata pour le 1er mois.
     */
    // supporteProrataEntree() → false par défaut — prorata géré dans calculer()

    /** AWB applique le prorata sur le dernier mois (clôture). */
    @Override
    public boolean supporteProrataSortie() {
        return true;
    }

    @Override
    public Commission calculer(Caution caution, YearMonth moisReference) {
        long nombreJours;
        String suffixeLibelle;

        // Prorata entrée : 1er mois de la caution → du lendemain de dateDePrise à fin de mois
        // (AWB ne facture pas le jour de délivrance)
        if (caution.getDateDePrise() != null
                && YearMonth.from(caution.getDateDePrise()).equals(moisReference)) {
            nombreJours = ChronoUnit.DAYS.between(
                    caution.getDateDePrise().plusDays(1),     // jour de délivrance exclu
                    moisReference.atEndOfMonth().plusDays(1) // borne exclusive
            );
            suffixeLibelle = " — Prorata entrée : " + nombreJours + " j";
        } else {
            nombreJours    = moisReference.lengthOfMonth();
            suffixeLibelle = " — Mois entier : " + nombreJours + " j";
        }

        return buildCommission(caution, nombreJours, moisReference.atEndOfMonth(), suffixeLibelle);
    }

    @Override
    public Commission calculerMainLevee(Caution caution, LocalDate dateDeMainLevee) {
        YearMonth moisMainLevee = YearMonth.from(dateDeMainLevee);
        LocalDate dateEmission  = moisMainLevee.atEndOfMonth();

        long nombreJours;
        String suffixeLibelle;

        // Cas particulier : dateDePrise et dateDeMainLevee dans le même mois
        // → facturer uniquement les jours entre les deux dates :
        //   jour de délivrance exclu (règle AWB), jour de main levée inclus
        if (caution.getDateDePrise() != null
                && YearMonth.from(caution.getDateDePrise()).equals(moisMainLevee)) {
            nombreJours   = ChronoUnit.DAYS.between(
                    caution.getDateDePrise().plusDays(1), // jour de délivrance exclu
                    dateDeMainLevee.plusDays(1)          // borne exclusive
            );
            suffixeLibelle = " — Même mois (prise→clôture) : " + nombreJours + " j";
        } else {
            // Prorata sortie normal : du 1er du mois jusqu'à dateDeMainLevee (inclus)
            nombreJours   = ChronoUnit.DAYS.between(
                    moisMainLevee.atDay(1),
                    dateDeMainLevee.plusDays(1) // borne exclusive
            );
            suffixeLibelle = " — Prorata sortie : " + nombreJours + " j";
        }

        return buildCommission(caution, nombreJours, dateEmission, suffixeLibelle);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Formule commune : (montant × taux × jours) / diviseur, avec seuil minimum —
     * les deux paramètres sont résolus via {@link ICautionCommissionConfigService}.
     * Le suffixe précise le mode de calcul dans le libellé
     * (ex. " — Prorata entrée : 17 j", " — Mois entier : 31 j").
     */
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
