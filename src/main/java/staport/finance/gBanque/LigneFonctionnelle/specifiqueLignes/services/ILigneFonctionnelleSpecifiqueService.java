package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueSearchCriteria;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;

/**
 * Façade publique du domaine LigneFonctionnelleSpecifique.
 * Le controller doit injecter cette interface, jamais {@code LigneFonctionnelleSpecifiqueService}
 * (architecture.md — DIP).
 */
public interface ILigneFonctionnelleSpecifiqueService {

    LigneFonctionnelleSpecifiqueResponse createLigneFonctionnelleSpecifique(LigneFonctionnelleSpecifiqueRequest request);

    LigneFonctionnelleSpecifiqueResponse getLigneFonctionnelleSpecifiqueById(Long id);

    Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiques(
            LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable);

    Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiquesByBanqueId(
            Long banqueId, LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable);

    LigneFonctionnelleSpecifiqueResponse updateLigneFonctionnelleSpecifique(Long id, LigneFonctionnelleSpecifiqueRequest request);

    void deactivateLigneFonctionnelleById(Long ligneId) throws Exception;

    void deleteLigneFonctionnelleSpecifique(Long id);

    Page<LigneFonctionnelleSpecifiqueResponse> getLinesByMarche(Long marcheId, Pageable pageable);

    AgregatedSuiviePrefiLigneResponseDto getSuiviePrefiLigneResponse(Long lineId, Pageable pageable);

    AgregatedSuivieDcLigneResponseDto getSuivieDcLigneResponse(Long lineId, Pageable pageable);

    AgregatedSuivieFactoringLigneResponseDto getSuivieFactoringLigneResponse(Long lineId, Pageable pageable);

    /**
     * Indique si au moins une ligne fonctionnelle spécifique référence cette nature.
     * Utilisé comme guard de suppression dans {@code NatureLigneFonctionnelleService}.
     */
    boolean existsByNatureId(Long natureId);
}
