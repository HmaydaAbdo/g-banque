package staport.finance.gBanque.prelevements.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.prelevements.dtos.EncoursPrelevement;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementSearchDto;
import staport.finance.gBanque.prelevements.service.PrelevementService;

@RestController
@RequestMapping("/prelevements")
public class PrelevementController {

    private static final Logger log = LoggerFactory.getLogger(PrelevementController.class);
    private final PrelevementService prelevementService;

    public PrelevementController(PrelevementService prelevementService) {
        this.prelevementService = prelevementService;
        log.info("Initialisation du contrôleur Prelevement");
    }

    /**
     * Crée un nouveau prélèvement
     *
     * @param requestDto Données du prélèvement à créer
     * @return Le prélèvement créé
     */
    @PostMapping
    public ResponseEntity<PrelevementResponseDto> createPrelevement(
            @Validated @RequestBody PrelevementRequestDto requestDto) {
        log.info("Requête REST pour créer un prélèvement - Libellé: '{}', Montant: {}, Banque ID: {}",
                requestDto.getLibelle(), requestDto.getMontant(), requestDto.getBanqueId());

        PrelevementResponseDto responseDto = prelevementService.createPrelevement(requestDto);

        log.info("Prélèvement créé avec succès - ID: {}, Statut: {}",
                responseDto.getId(), responseDto.getStatus());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Récupère un prélèvement par son ID
     *
     * @param id Identifiant du prélèvement
     * @return Le prélèvement demandé
     */
    @GetMapping("/{id}")
    public ResponseEntity<PrelevementResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer le prélèvement avec ID: {}", id);

        PrelevementResponseDto responseDto = prelevementService.getById(id);

        log.debug("Prélèvement récupéré - ID: {}, Libellé: '{}', Montant: {}",
                responseDto.getId(), responseDto.getLibelle(), responseDto.getMontant());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Recherche des prélèvements selon des critères avec pagination
     *
     * @param searchDto Critères de recherche
     * @param pageable Informations de pagination
     * @return Page de prélèvements correspondant aux critères
     */
    @PostMapping("/search")
    public ResponseEntity<Page<PrelevementResponseDto>> searchPrelevements(
            @RequestBody PrelevementSearchDto searchDto,
            Pageable pageable) {
        log.info("Requête REST pour rechercher des prélèvements - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<PrelevementResponseDto> page = prelevementService.searchPrelevements(searchDto, pageable);

        log.info("Recherche terminée - {} prélèvement(s) trouvé(s) sur {} au total",
                page.getNumberOfElements(), page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    /**
     * Met à jour un prélèvement existant
     *
     * @param id Identifiant du prélèvement à modifier
     * @param requestDto Nouvelles données du prélèvement
     * @return Le prélèvement mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<PrelevementResponseDto> updatePrelevement(
            @PathVariable Long id,
            @Validated @RequestBody PrelevementRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour le prélèvement ID: {} - Nouveau libellé: '{}', Nouveau montant: {}",
                id, requestDto.getLibelle(), requestDto.getMontant());

        PrelevementResponseDto responseDto = prelevementService.updatePrelevement(id, requestDto);

        log.info("Prélèvement ID: {} mis à jour avec succès", id);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Supprime un prélèvement
     *
     * @param id Identifiant du prélèvement à supprimer
     * @return Réponse vide avec statut 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrelevement(@PathVariable Long id) {
        log.info("Requête REST pour supprimer le prélèvement ID: {}", id);

        prelevementService.deletePrelevement(id);

        log.info("Prélèvement ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les prélèvements en cours avec leur montant total pour une banque spécifique
     * Les prélèvements en cours sont ceux avec un statut 'PREVU' et une date d'émission dans le passé
     *
     * @param banqueId ID de la banque pour filtrer les prélèvements
     * @param searchDto Critères de recherche optionnels
     * @param pageable Informations de pagination et de tri
     * @return Un EncoursPrelevement contenant la page de prélèvements et le montant total en cours
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursPrelevement> getOutstandingPrelevements(
            @PathVariable Long banqueId,
            @ModelAttribute PrelevementSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les prélèvements en cours - Banque ID: {}, Page: {}, Taille: {}",
                banqueId, pageable.getPageNumber(), pageable.getPageSize());

        if (searchDto != null && searchDto.toString() != null) {
            log.debug("Critères de recherche additionnels: {}", searchDto);
        }

        searchDto.setBanqueId(banqueId);
        EncoursPrelevement result = prelevementService.getOutstandingPrelevements(searchDto, pageable);

        log.info("Prélèvements en cours récupérés - Banque ID: {}, Total encours: {}, Nombre de prélèvements: {}",
                banqueId, result.getTotalDesEncours(),
                result.getPage() != null ? result.getPage().getTotalElements() : 0);

        return ResponseEntity.ok(result);
    }


    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<PrelevementResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut du prélèvement ID: {}", id);

        PrelevementResponseDto responseDto = prelevementService.toggleStatus(id);

        log.info("Statut du prélèvement ID: {} basculé avec succès vers {}",
                id, responseDto.getStatus());
        return ResponseEntity.ok(responseDto);
    }
}