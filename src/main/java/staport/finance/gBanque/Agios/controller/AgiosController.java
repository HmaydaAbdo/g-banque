package staport.finance.gBanque.Agios.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.dtos.EncoursAgiosDto;
import staport.finance.gBanque.Agios.service.AgiosService;

@RestController
@RequestMapping("/agios")
public class AgiosController {

    private static final Logger log = LoggerFactory.getLogger(AgiosController.class);
    private final AgiosService agiosService;

    public AgiosController(AgiosService agiosService) {
        this.agiosService = agiosService;
        log.info("Initialisation du contrôleur Agios");
    }

    /**
     * Crée un nouvel agios
     *
     * @param requestDto Données de l'agios à créer
     * @return L'agios créé
     */
    @PostMapping
    public ResponseEntity<AgiosResponseDto> createAgios(@Validated @RequestBody AgiosRequestDto requestDto) {
        log.info("Requête REST pour créer un agios - Libellé: '{}', Montant: {}, Banque ID: {}",
                requestDto.getLibelle(), requestDto.getMontant(), requestDto.getBanqueId());

        AgiosResponseDto responseDto = agiosService.createAgios(requestDto);

        log.info("Agios créé avec succès - ID: {}, Statut: {}",
                responseDto.getId(), responseDto.getStatus());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Récupère un agios par son ID
     *
     * @param id Identifiant de l'agios
     * @return L'agios demandé
     */
    @GetMapping("/{id}")
    public ResponseEntity<AgiosResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer l'agios avec ID: {}", id);

        AgiosResponseDto responseDto = agiosService.getById(id);

        log.debug("Agios récupéré - ID: {}, Libellé: '{}', Montant: {}",
                responseDto.getId(), responseDto.getLibelle(), responseDto.getMontant());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Recherche des agios selon des critères avec pagination
     *
     * @param searchDto Critères de recherche
     * @param pageable Informations de pagination
     * @return Page d'agios correspondant aux critères
     */
    @PostMapping("/search")
    public ResponseEntity<Page<AgiosResponseDto>> searchAgios(
            @RequestBody AgiosSearchDto searchDto,
            Pageable pageable) {
        log.info("Requête REST pour rechercher des agios - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<AgiosResponseDto> page = agiosService.searchAgios(searchDto, pageable);

        log.info("Recherche terminée - {} agios trouvé(s) sur {} au total",
                page.getNumberOfElements(), page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    /**
     * Met à jour un agios existant
     *
     * @param id Identifiant de l'agios à modifier
     * @param requestDto Nouvelles données de l'agios
     * @return L'agios mis à jour
     */
    @PutMapping("/{id}")
    public ResponseEntity<AgiosResponseDto> updateAgios(
            @PathVariable Long id,
            @Validated @RequestBody AgiosRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour l'agios ID: {} - Nouveau libellé: '{}', Nouveau montant: {}",
                id, requestDto.getLibelle(), requestDto.getMontant());

        AgiosResponseDto responseDto = agiosService.updateAgios(id, requestDto);

        log.info("Agios ID: {} mis à jour avec succès", id);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * Supprime un agios
     *
     * @param id Identifiant de l'agios à supprimer
     * @return Réponse vide avec statut 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgios(@PathVariable Long id) {
        log.info("Requête REST pour supprimer l'agios ID: {}", id);

        agiosService.deleteAgios(id);

        log.info("Agios ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Récupère les agios en cours avec leur montant total pour une banque spécifique
     * Les agios en cours sont ceux avec un statut 'PREVU' et une date d'émission dans le passé
     *
     * @param banqueId ID de la banque pour filtrer les agios
     * @param searchDto Critères de recherche optionnels
     * @param pageable Informations de pagination et de tri
     * @return Un EncoursAgiosDto contenant la page d'agios et le montant total en cours
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursAgiosDto> getOutstandingAgios(
            @PathVariable Long banqueId,
            @ModelAttribute AgiosSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les agios en cours - Banque ID: {}, Page: {}, Taille: {}",
                banqueId, pageable.getPageNumber(), pageable.getPageSize());

        if (searchDto != null && searchDto.toString() != null) {
            log.debug("Critères de recherche additionnels: {}", searchDto);
        }

        searchDto.setBanqueId(banqueId);
        EncoursAgiosDto result = agiosService.getOutstandingAgios(searchDto, pageable);

        log.info("Agios en cours récupérés - Banque ID: {}, Total encours: {}, Nombre d'agios: {}",
                banqueId, result.getTotalDesEncours(),
                result.getPage() != null ? result.getPage().getTotalElements() : 0);

        return ResponseEntity.ok(result);
    }

    /**
     * Bascule le statut d'un agios entre PREVU et REALISE
     *
     * @param id Identifiant de l'agios
     * @return L'agios avec le statut basculé
     */
    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<AgiosResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut de l'agios ID: {}", id);

        AgiosResponseDto responseDto = agiosService.toggleStatus(id);

        log.info("Statut de l'agios ID: {} basculé avec succès vers {}",
                id, responseDto.getStatus());
        return ResponseEntity.ok(responseDto);
    }
}