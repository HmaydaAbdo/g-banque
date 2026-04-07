package staport.finance.gBanque.cautions.commissions.service;

import java.time.YearMonth;

/**
 * Contrat de service pour la génération mensuelle des commissions sur cautions.
 *
 * <p>Le contrôleur ou le scheduler injecte uniquement cette interface (DIP).
 * La classe concrète {@link GenerationCommissionCautionServiceImpl} n'est jamais
 * référencée depuis l'extérieur.
 */
public interface IGenerationCommissionCautionService {

    /**
     * Génère les commissions du mois suivant pour toutes les cautions actives
     * dont la banque dispose d'une stratégie de calcul enregistrée.
     *
     * <p>Idempotente : si une commission avec le même triptyque
     * {@code (CAUTION, dateEmission, libelle)} existe déjà, elle est ignorée
     * (log WARN) et comptée dans {@code ignorees}.
     *
     * <p>Exemple : appeler {@code generer(YearMonth.of(2026, 3))} génère les
     * commissions avec {@code dateEmission = 30/04/2026}.
     *
     * @param moisReference mois X de référence (mois en cours) — les commissions ciblent X+1
     * @return résumé de l'exécution : nombre de commissions créées et ignorées
     */
    ResultatGenerationCommissionDto generer(YearMonth moisReference);
}
