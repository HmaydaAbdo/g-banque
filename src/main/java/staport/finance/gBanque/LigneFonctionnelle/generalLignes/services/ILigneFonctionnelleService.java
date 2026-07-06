package staport.finance.gBanque.LigneFonctionnelle.generalLignes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    LigneFonctionnelleResponse createLigneFonctionnelle(LigneFonctionnelleRequest request);

    LigneFonctionnelleResponse getLigneFonctionnelleById(Long id);

    Page<LigneFonctionnelleResponse> getPaginatedLigneFonctionnelles(
            LigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByBanqueId(
            Long banqueId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    Page<LigneFonctionnelleResponse> getPaginatedLignesFonctionnellesByMarcheId(
            Long marcheId, LigneFonctionnelleSearchCriteria criteria, Pageable pageable);

    LigneFonctionnelleResponse updateLigneFonctionnelle(Long id, LigneFonctionnelleRequest request);

    LigneFonctionnelleResponse manageSousLignes(Long ligneMereId, List<SousLigneRequestDto> sousLignesRequests);

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
