package staport.finance.gBanque.recapitulatif.services.interfaces;

import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.recapitulatif.dtos.request.RecapitulatifSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.EncoursPrevusDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.dtos.response.SourceRecapDto;
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
     * @param periode horizon de calcul (à ce jour, fin de mois ou fin de trimestre) — voir {@link PeriodeRecap}.
     */
    RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId, PeriodeRecap periode);

    /**
     * @param periode horizon de calcul (à ce jour, fin de mois ou fin de trimestre) — voir {@link PeriodeRecap}.
     */
    List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux(PeriodeRecap periode);

    /** Encours prévisionnels (+/-) d'une banque jusqu'à l'horizon demandé (fin de mois ou de trimestre). */
    EncoursPrevusDto getEncoursPrevus(Long banqueId, PeriodeRecap periode);

    /** Idem pour toutes les banques en une requête (banques sans encours absentes de la Map). */
    Map<Long, EncoursPrevusDto> getEncoursPrevusParBanque(PeriodeRecap periode);

    /**
     * Encours à valider de toutes les banques jusqu'à l'horizon (mêmes lignes que le plan de trésorerie), triés par
     * banque puis par date de référence — page globale de la Trésorerie prévisionnelle, en un seul appel.
     */
    List<RecaputilatifItem> getEncoursToutesBanques(PeriodeRecap periode);

    /**
     * Indicateurs du récapitulatif ventilés par source (nombre d'encours, débit, crédit, échues
     * à ce jour) pour une banque, avec EXACTEMENT les mêmes critères que
     * {@link #getRecapitulatifForCurrentMonthForGivenBanque} — les cartes par source de l'écran
     * suivent donc les filtres actifs. Triées par nombre d'encours décroissant.
     */
    List<SourceRecapDto> getRecapitulatifParSource(RecapitulatifSearchDto searchDto, Long banqueId);
}
