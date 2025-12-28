package staport.finance.gBanque.droitConstate.controller;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.droitConstate.dtos.*;
import staport.finance.gBanque.droitConstate.service.DroitConstateService;


/**
 * REST controller for managing Prefinancement records.
 * Provides a complete CRUD and search API.
 */
@RestController
@RequestMapping("/droitsConstate")
public class DroitConstateController {


    private static final Logger log = LoggerFactory.getLogger(DroitConstateController.class);
    private final DroitConstateService droitConstateService;

    public DroitConstateController(DroitConstateService droitConstateService) {
        this.droitConstateService = droitConstateService;
    }


    @PostMapping
    public ResponseEntity<DroitConstateResponseDto> createDroitConstate(@Valid @RequestBody DroitConstateRequestDto requestDTO) throws Exception {
        log.info("POST /droitsConstate - Réception d'une demande de création de droit constaté");
        log.debug("Données de la requête: {}", requestDTO);
        DroitConstateResponseDto responseDTO = droitConstateService.createDroitConstate(requestDTO);
        log.info("POST /droitsConstate - Droit constaté créé avec succès, ID: {}", responseDTO.getId());
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }


    @PutMapping("/{id}")
    public ResponseEntity<DroitConstateResponseDto> updateDroitConstate(@PathVariable Long id, @Valid @RequestBody DroitConstateRequestDto requestDTO) {
        log.info("PUT /droitsConstate/{} - Réception d'une demande de modification", id);
        log.debug("Nouvelles données: {}", requestDTO);

        DroitConstateResponseDto responseDTO = droitConstateService.updateDroitConstate(id, requestDTO);

        log.info("PUT /droitsConstate/{} - Droit constaté modifié avec succès", id);
        return ResponseEntity.ok(responseDTO);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDroitConstate(@PathVariable Long id) {
        log.info("DELETE /droitsConstate/{} - Réception d'une demande de suppression", id);

        droitConstateService.deleteDroitConstate(id);

        log.info("DELETE /droitsConstate/{} - Droit constaté supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}")
    public ResponseEntity<DroitConstateResponseDto> getDroitConstateById(@PathVariable Long id) {
        log.info("GET /droitsConstate/{} - Réception d'une demande de consultation", id);

        DroitConstateResponseDto responseDTO = droitConstateService.findById(id);

        log.debug("GET /droitsConstate/{} - Droit constaté trouvé: Marché {}, Montant {}",
                id, responseDTO.getMarcheName(), responseDTO.getMontant());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping
    public ResponseEntity<Page<DroitConstateResponseDto>> searchDroitConstates(DroitConstateSearchDto searchDTO, @PageableDefault Pageable pageable) {
        log.info("GET /droitsConstate - Réception d'une demande de recherche");
        log.debug("Critères de recherche: {}, Page: {}, Taille: {}",
                searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Page<DroitConstateResponseDto> page = droitConstateService.findAll(searchDTO, pageable);

        log.info("GET /droitsConstate - Recherche terminée: {} résultat(s) trouvé(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{id}/status/toggle")
    public ResponseEntity<DroitConstateResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /droitsConstate/{}/status/toggle - Réception d'une demande de changement de statut", id);

        DroitConstateResponseDto responseDTO = droitConstateService.toggleStatus(id);

        log.info("PATCH /droitsConstate/{}/status/toggle - Statut changé avec succès vers: {}",
                id, responseDTO.getStatus());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/remboursement/encours/{banqueId}")
    public ResponseEntity<EncoursDroitConstateDto> getEncoursForRemboursement(
            @PathVariable Long banqueId, EncoursDroitConstateSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("GET /droitsConstate/remboursement/encours/{} - Consultation des encours de remboursement", banqueId);
        log.debug("Critères de recherche encours remboursement: {}", searchDTO);

        EncoursDroitConstateDto result = droitConstateService.getRemboursementEncoursForBanque(searchDTO, banqueId, pageable);

        log.info("GET /droitsConstate/remboursement/encours/{} - Total encours: {}, Nombre d'éléments: {}", banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/deblocage/encours/{banqueId}")
    public ResponseEntity<EncoursDroitConstateDto> getEncoursForDeblocage(
            @PathVariable Long banqueId, EncoursDroitConstateSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("GET /droitsConstate/deblocage/encours/{} - Consultation des encours de déblocage", banqueId);
        log.debug("Critères de recherche encours déblocage: {}", searchDTO);

        EncoursDroitConstateDto result = droitConstateService.getDeblocageEncoursForBanque(searchDTO, banqueId, pageable);

        log.info("GET /droitsConstate/deblocage/encours/{} - Total encours: {}, Nombre d'éléments: {}", banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());

        return ResponseEntity.ok(result);
    }


}
