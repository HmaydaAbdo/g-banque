package staport.finance.gBanque.recapitulatif.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.recapitulatif.dtos.request.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.util.List;

/**
 * Façade publique du module Récapitulatif — vue consolidée des décaissements/encaissements
 * prévisionnels du mois pour une banque, tous modules sources confondus.
 */
public interface IRecapitulatifService {

    AgregatedRecapDto getRecapitulatifEncaissementForCurrentMonthForGivenBanque(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId);

    AgregatedRecapDto getRecapitulatifDecaissementForCurrentMonthForGivenBanque(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId);

    /**
     * Exporte en PDF le récapitulatif des décaissements prévisionnels du mois pour une banque
     * (mêmes critères que {@link #getRecapitulatifDecaissementForCurrentMonthForGivenBanque}, sans pagination).
     */
    byte[] exportRecapitulatifDecaissementPdf(DecaissementSearchDto searchDto, Long banqueId);

    /**
     * Exporte en PDF le récapitulatif des encaissements prévisionnels du mois pour une banque
     * (mêmes critères que {@link #getRecapitulatifEncaissementForCurrentMonthForGivenBanque}, sans pagination).
     */
    byte[] exportRecapitulatifEncaissementPdf(DecaissementSearchDto searchDto, Long banqueId);

    /**
     * @param periode horizon de calcul (à ce jour, ou jusqu'à fin de mois) — voir {@link PeriodeRecap}.
     */
    RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId, PeriodeRecap periode);

    /**
     * @param periode horizon de calcul (à ce jour, ou jusqu'à fin de mois) — voir {@link PeriodeRecap}.
     */
    List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux(PeriodeRecap periode);
}
