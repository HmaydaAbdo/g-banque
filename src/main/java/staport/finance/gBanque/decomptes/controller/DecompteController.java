package staport.finance.gBanque.decomptes.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import staport.finance.gBanque.decomptes.dto.*;
import staport.finance.gBanque.decomptes.service.DecompteService;


/**
 * REST controller for managing décomptes records.
 * Provides a complete CRUD and search API.
 */
@RestController
@RequestMapping("/decomptes")
public class DecompteController {


    private static final Logger log = LoggerFactory.getLogger(DecompteController.class);
    private final DecompteService decompteService;


    public DecompteController(DecompteService decompteService) {
        this.decompteService = decompteService;
        log.info("DecompteController initialisé");
    }

    /**
     * Creates a new Décompte record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return A ResponseEntity with the newly created record and HTTP status 201.
     */
    @PostMapping
    public ResponseEntity<DecompteResponseDto> createDecompte(@Valid @RequestBody DecompteRequestDto requestDTO) {
        log.info("POST /decomptes - Réception d'une demande de création de décompte");
        log.debug("Données de la requête: {}", requestDTO);

        DecompteResponseDto responseDTO = decompteService.createDecompte(requestDTO);

        log.info("POST /decomptes - Décompte créé avec succès, ID: {}", responseDTO.getId());
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    /**
     * Updates an existing Décompte record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DecompteResponseDto> updateDecompte(@PathVariable Long id, @Valid @RequestBody DecompteRequestDto requestDTO) {
        log.info("PUT /decomptes/{} - Réception d'une demande de modification", id);
        log.debug("Nouvelles données: {}", requestDTO);

        DecompteResponseDto responseDTO = decompteService.updateDecompte(id, requestDTO);

        log.info("PUT /decomptes/{} - Décompte modifié avec succès", id);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Deletes a Décompte record by its ID.
     *
     * @param id The ID of the record to delete.
     * @return A ResponseEntity with HTTP status 204.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDecompte(@PathVariable Long id) {
        log.info("DELETE /decomptes/{} - Réception d'une demande de suppression", id);

        decompteService.deleteDecompte(id);

        log.info("DELETE /decomptes/{} - Décompte supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }


    /**
     * Retrieves a Décompte record by its ID.
     *
     * @param id The ID of the record to retrieve.
     * @return A ResponseEntity with the found record and HTTP status 200.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DecompteResponseDto> getDecompteById(@PathVariable Long id) {
        log.info("GET /decomptes/{} - Réception d'une demande de consultation", id);

        DecompteResponseDto responseDTO = decompteService.findById(id);

        log.debug("GET /decomptes/{} - Décompte trouvé: Marché {}, Montant {}",
                id, responseDTO.getMarcheName(), responseDTO.getMontant());
        return ResponseEntity.ok(responseDTO);
    }



    /**
     * Searches for décomptes records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of DecompteResponseDto matching the criteria.
     */
    @GetMapping
    public ResponseEntity<Page<DecompteResponseDto>> searchDecomptes(DecompteSearchDto searchDTO, @PageableDefault Pageable pageable) {
        log.info("GET /decomptes - Réception d'une demande de recherche");
        log.debug("Critères de recherche: {}, Page: {}, Taille: {}",
                searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Page<DecompteResponseDto> page = decompteService.findAll(searchDTO, pageable);

        log.info("GET /decomptes - Recherche terminée: {} résultat(s) trouvé(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }


    @PatchMapping("/{id}/toggle")
    public ResponseEntity<DecompteResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("PATCH /decomptes/{}/toggle - Réception d'une demande de changement de statut", id);

        DecompteResponseDto responseDTO = decompteService.toglleStatus(id);

        log.info("PATCH /decomptes/{}/toggle - Statut changé avec succès vers: {}",
                id, responseDTO.getStatus());
        return ResponseEntity.ok(responseDTO);
    }


    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(DecompteSearchDto searchDTO) {
        log.info("GET /decomptes/export/pdf - Génération export PDF");
        byte[] pdf = decompteService.exportToPdf(searchDTO);
        String filename = "decomptes_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursDecompteDto> getEncoursForDecomptes(
            @PathVariable Long banqueId,
            EncoursDecompteSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("GET /decomptes/encours/{} - Consultation des encours de décomptes", banqueId);
        log.debug("Critères de recherche encours: {}", searchDTO);

        EncoursDecompteDto result = decompteService.getDecomptesEncoursForBanque(searchDTO, banqueId, pageable);

        log.info("GET /decomptes/encours/{} - Total encours: {}, Nombre d'éléments: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }
}