package staport.finance.gBanque.recapitulatif.services.interfaces;

import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleAllBanquesDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecapGeneralDetailleDto;

/**
 * Façade publique du module Récapitulatif général détaillé — vue synthétique par catégorie
 * (résumé du jour + décaissements/encaissements du reste du mois) pour une ou toutes les banques.
 */
public interface IRecapGeneralDetailleService {

    RecapGeneralDetailleDto getRecapForBanque(Long banqueId);

    RecapGeneralDetailleAllBanquesDto getRecapAllBanques();
}
