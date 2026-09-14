package staport.finance.gBanque.cautions.commissions.service;

import java.time.YearMonth;

/**
 * Résultat de la génération mensuelle des commissions sur cautions.
 *
 * @param generees      nombre de commissions créées et persistées
 * @param ignorees      nombre de cautions ignorées (pas de stratégie ou déjà générée)
 * @param moisReference mois X passé en paramètre — les commissions ciblent le mois X+1
 */
public record ResultatGenerationCommissionDto(
        int generees,
        int ignorees,
        YearMonth moisReference
) {}
