package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleChartData;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.LigneFonctionnelleSearchCriteria;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.dtos.SousLigneRequestDto;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieLcnLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service contract for the LigneFonctionnelle (générale) module.
 *
 * Controllers must inject this interface (DIP), never the concrete class.
 * The concrete implementation is {@code LigneFonctionnelleService}.
 */
public interface ILigneFonctionnelleService {

    /**
     * @param banque             résolue par le Controller ({@code IBanqueService.findOrThrow}) —
     *                           évite d'injecter {@code BanqueRepository} ici et le cycle de beans
     *                           avec {@code IBanqueService} (qui injecte déjà {@code ILigneFonctionnelleService}).
     * @param nature             résolue par le Controller ({@code INatureLigneFonctionnelleService.findOrThrow}),
     *                           même raison.
     * @param sousLigneNatures   natures des sous-lignes de {@code request.sousLignes()}, résolues par
     *                           le Controller et indexées par {@code natureLigneFonctionnelleId} ; vide
     *                           si la requête ne contient pas de sous-lignes.
     */
    LigneFonctionnelleResponse createLigneFonctionnelle(
            LigneFonctionnelleRequest request, Banque banque, NatureLigneFonctionnelle nature,
            Map<Long, NatureLigneFonctionnelle> sousLigneNatures);

    LigneFonctionnelleResponse getLigneFonctionnelleById(Long id);

    Page<LigneFonctionnelleResponse> getPaginatedLigneFonctionnelles(
            LigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByBanqueId(
            Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByMarcheId(
            Long marcheId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    /**
     * @param banque  résolue par le Controller — voir {@link #createLigneFonctionnelle}.
     * @param nature  résolue par le Controller — voir {@link #createLigneFonctionnelle}.
     */
    LigneFonctionnelleResponse updateLigneFonctionnelle(
            Long id, LigneFonctionnelleRequest request, Banque banque, NatureLigneFonctionnelle nature);

    /**
     * @param sousLigneNatures natures des sous-lignes résolues par le Controller, indexées par
     *                         {@code natureLigneFonctionnelleId} — voir {@link #createLigneFonctionnelle}.
     */
    LigneFonctionnelleResponse manageSousLignes(
            Long ligneMereId, List<SousLigneRequestDto> sousLignesRequests,
            Map<Long, NatureLigneFonctionnelle> sousLigneNatures);

    void deactivateLigneFonctionnelle(Long ligneId);

    void deleteLigneFonctionnelle(Long id);

    LigneFonctionnelleChartData getHistoricalChartData(Long banqueId, Long natureLigneFonctionnelleId);

    AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable);

    AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable);

    AgregatedSuivieFactoringLigneResponseDto getSuivieFactoringLigneResponse(Long lineId, Pageable pageable);

    AgregatedSuivieLcnLigneResponseDto getSuivieLcnLigneResponse(Long lineId, Pageable pageable);

    BigDecimal getPlafondFCInternal(Long banqueId, Long fcNatureId);

    Map<Long, BigDecimal> getActivePlafondsForBanks(List<Long> bankIds, String fcCode);

    /**
     * Indique si une banque possède au moins une ligne fonctionnelle active.
     * Utilisé comme guard de suppression dans {@code IBanqueService#deleteBanque}.
     */
    boolean hasActiveLignesForBanque(Long banqueId);

    /**
     * Indique si au moins une ligne fonctionnelle générale référence cette nature.
     * Utilisé comme guard de suppression dans {@code NatureLigneFonctionnelleService}.
     */
    boolean existsByNatureId(Long natureId);
}
