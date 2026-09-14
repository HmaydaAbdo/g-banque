package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueRequest;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueResponse;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.LigneFonctionnelleSpecifiqueSearchCriteria;
import staport.finance.gBanque.droitConstate.dtos.response.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.response.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.response.AgregatedSuiviePrefiLigneResponseDto;

/**
 * Façade publique du domaine LigneFonctionnelleSpecifique.
 * Le controller doit injecter cette interface, jamais {@code LigneFonctionnelleSpecifiqueService}
 * (architecture.md — DIP).
 */
public interface ILigneFonctionnelleSpecifiqueService {

    /**
     * @param nature résolue par le Controller ({@code INatureLigneFonctionnelleService.findOrThrow}) —
     *               évite d'injecter {@code NatureLigneFonctionnelleRepository} ici et le cycle de
     *               beans avec {@code NatureLigneFonctionnelleService} (qui injecte déjà
     *               {@code ILigneFonctionnelleSpecifiqueService}).
     */
    LigneFonctionnelleSpecifiqueResponse createLigneFonctionnelleSpecifique(
            LigneFonctionnelleSpecifiqueRequest request, NatureLigneFonctionnelle nature);

    LigneFonctionnelleSpecifiqueResponse getLigneFonctionnelleSpecifiqueById(Long id);

    Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiques(
            LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable);

    Page<LigneFonctionnelleSpecifiqueResponse> getLignesFonctionnellesSpecifiquesByBanqueId(
            Long banqueId, LigneFonctionnelleSpecifiqueSearchCriteria criteria, Pageable pageable);

    /**
     * @param nature résolue par le Controller si {@code request.natureLigneFonctionnelleId()} est
     *               fourni, {@code null} sinon (conserve la nature existante) — voir
     *               {@link #createLigneFonctionnelleSpecifique}.
     */
    LigneFonctionnelleSpecifiqueResponse updateLigneFonctionnelleSpecifique(
            Long id, LigneFonctionnelleSpecifiqueRequest request, NatureLigneFonctionnelle nature);

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

    /**
     * Exporte en PDF l'intégralité des lignes fonctionnelles spécifiques correspondant aux filtres
     * fournis (mêmes critères que {@link #getLignesFonctionnellesSpecifiques}) — sans pagination.
     * Réutilisé par les écrans encours ({@code criteria.actif() == true}) et archivées
     * ({@code criteria.actif() == false}).
     */
    byte[] exportPdf(LigneFonctionnelleSpecifiqueSearchCriteria criteria);
}
