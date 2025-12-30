package staport.finance.gBanque.factoring.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.factoring.dtos.*;
import staport.finance.gBanque.factoring.service.FactoringService;


@RestController
@RequestMapping("/factoring")
public class FactoringController {


    private static final Logger log = LoggerFactory.getLogger(FactoringController.class);
    private final FactoringService factoringService;

    public FactoringController(FactoringService factoringService) {
        this.factoringService = factoringService;
        log.info("FactoringController initialisé");
    }


    /**
     * Creates a new Factoring record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return A ResponseEntity with the newly created record and HTTP status 201.
     */
    @PostMapping
    public ResponseEntity<FactoringResponseDto> createFactoring(@Valid @RequestBody FactoringRequestDto requestDTO) throws Exception {
        log.info("POST /factoring - Réception d'une demande de création de factoring");
        log.debug("Données de la requête: {}", requestDTO);

        FactoringResponseDto responseDTO = factoringService.createFactoring(requestDTO);

        log.info("POST /factoring - Factoring créé avec succès, ID: {}", responseDTO.getId());
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    /**
     * Updates an existing Factoring record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FactoringResponseDto> updateFactoring(@PathVariable Long id, @Valid @RequestBody FactoringRequestDto requestDTO) throws Exception {
        log.info("PUT /factoring/{} - Réception d'une demande de modification", id);
        log.debug("Nouvelles données: {}", requestDTO);

        FactoringResponseDto responseDTO = factoringService.updateFactoring(id, requestDTO);

        log.info("PUT /factoring/{} - Factoring modifié avec succès", id);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Deletes a Factoring record by its ID.
     *
     * @param id The ID of the record to delete.
     * @return A ResponseEntity with HTTP status 204.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFactoring(@PathVariable Long id) {
        log.info("DELETE /factoring/{} - Réception d'une demande de suppression", id);

        factoringService.deleteFactoring(id);

        log.info("DELETE /factoring/{} - Factoring supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a Factoring record by its ID.
     *
     * @param id The ID of the record to retrieve.
     * @return A ResponseEntity with the found record and HTTP status 200.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FactoringResponseDto> getFactoringById(@PathVariable Long id) {
        log.info("GET /factoring/{} - Réception d'une demande de consultation", id);

        FactoringResponseDto responseDTO = factoringService.findById(id);

        log.debug("GET /factoring/{} - Factoring trouvé: Marché {}, Montant {}",
                id, responseDTO.getMarcheName(), responseDTO.getMontant());
        return ResponseEntity.ok(responseDTO);
    }



    /**
     * Searches for Factoring records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of FactoringResponseDto matching the criteria.
     */
    @GetMapping
    public ResponseEntity<Page<FactoringResponseDto>> searchFactoring(FactoringSearchDto searchDTO, @PageableDefault Pageable pageable) {
        log.info("GET /factoring - Réception d'une demande de recherche");
        log.debug("Critères de recherche: {}, Page: {}, Taille: {}",
                searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Page<FactoringResponseDto> page = factoringService.findAll(searchDTO, pageable);

        log.info("GET /factoring - Recherche terminée: {} résultat(s) trouvé(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    /**
     * Toggle the status of a Factoring record
     *
     * @param id The ID of the record.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PatchMapping("/{id}/toggleStatus")
    public ResponseEntity<FactoringResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /factoring/{}/toggleStatus - Réception d'une demande de changement de statut", id);

        FactoringResponseDto responseDTO = factoringService.toggleStatus(id);

        log.info("PATCH /factoring/{}/toggleStatus - Statut changé avec succès vers: {}",
                id, responseDTO.getStatus());
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/remboursement/encours/{banqueId}")
    public ResponseEntity<EncoursFactoringDto> getEncoursForRemboursement(
            @PathVariable Long banqueId,
            EncoursFactoringSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("GET /factoring/remboursement/encours/{} - Consultation des encours de remboursement", banqueId);
        log.debug("Critères de recherche encours remboursement: {}", searchDto);

        EncoursFactoringDto result = factoringService.getRemboursementEncoursForBanque(searchDto, banqueId, pageable);

        log.info("GET /factoring/remboursement/encours/{} - Total encours: {}, Nombre d'éléments: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/deblocage/encours/{banqueId}")
    public ResponseEntity<EncoursFactoringDto> getEncoursForDeblocage(
            @PathVariable Long banqueId,
            EncoursFactoringSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("GET /factoring/deblocage/encours/{} - Consultation des encours de déblocage", banqueId);
        log.debug("Critères de recherche encours déblocage: {}", searchDto);

        EncoursFactoringDto result = factoringService.getDeblocageEncoursForBanque(searchDto, banqueId, pageable);

        log.info("GET /factoring/deblocage/encours/{} - Total encours: {}, Nombre d'éléments: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }
}