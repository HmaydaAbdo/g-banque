package staport.finance.gBanque.recapitulatif.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.recapitulatif.dtos.request.RecapitulatifSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.EncoursFinDeMoisDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.util.List;
import java.util.Map;

/**
 * Façade publique du module Récapitulatif — vue consolidée des décaissements/encaissements
 * prévisionnels du mois pour une banque, tous modules sources confondus.
 */
public interface IRecapitulatifService {

    /**
     * Récapitulatif des encours prévisionnels du mois pour une banque, paginé, trié et filtré.
     *
     * <p>Le sens fait partie des critères portés par {@code searchDto} : {@code searchDto.sens()}
     * à {@code null} retourne les encaissements et les décaissements entrelacés (comportement par
     * défaut de l'écran, « Tous »), une valeur précise ne retourne que ce sens.</p>
     *
     * <p>Les deux totaux du {@link AgregatedRecapDto} suivent toujours les critères appliqués :
     * lorsqu'un sens précis est demandé, le total de l'autre sens vaut zéro.</p>
     */
    AgregatedRecapDto getRecapitulatifForCurrentMonthForGivenBanque(
            Pageable pageable, RecapitulatifSearchDto searchDto, Long banqueId);

    /**
     * Exporte en PDF le récapitulatif des encours prévisionnels du mois pour une banque
     * (mêmes critères que {@link #getRecapitulatifForCurrentMonthForGivenBanque}, sans pagination).
     */
    byte[] exportRecapitulatifPdf(RecapitulatifSearchDto searchDto, Long banqueId);

    /**
     * @param periode horizon de calcul (à ce jour, ou jusqu'à fin de mois) — voir {@link PeriodeRecap}.
     */
    RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId, PeriodeRecap periode);

    /**
     * @param periode horizon de calcul (à ce jour, ou jusqu'à fin de mois) — voir {@link PeriodeRecap}.
     */
    List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux(PeriodeRecap periode);

    /** Encours prévisionnels (+/-) d'une banque jusqu'à la fin du mois en cours. */
    EncoursFinDeMoisDto getEncoursFinDeMois(Long banqueId);

    /** Idem pour toutes les banques en une requête (banques sans encours absentes de la Map). */
    Map<Long, EncoursFinDeMoisDto> getEncoursFinDeMoisParBanque();
}
