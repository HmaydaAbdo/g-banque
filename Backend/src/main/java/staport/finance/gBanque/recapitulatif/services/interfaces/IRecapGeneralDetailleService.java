package staport.finance.gBanque.recapitulatif.services.interfaces;

import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

/**
 * Façade publique du module Récapitulatif général détaillé — vue synthétique par catégorie
 * (résumé du jour + décaissements/encaissements du lendemain jusqu'à l'horizon choisi, fin de mois
 * ou fin de trimestre) pour une ou toutes les banques.
 */
public interface IRecapGeneralDetailleService {

    RecapGeneralDetailleDto getRecapForBanque(Long banqueId, PeriodeRecap periode);

    RecapGeneralDetailleAllBanquesDto getRecapAllBanques(PeriodeRecap periode);
}
