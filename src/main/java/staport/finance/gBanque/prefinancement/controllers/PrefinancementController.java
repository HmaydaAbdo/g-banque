package staport.finance.gBanque.prefinancement.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.prefinancement.dtos.*;
import staport.finance.gBanque.prefinancement.services.PrefinancementService;

/**
 * REST controller for managing Prefinancement records.
 * Provides a complete CRUD and search API.
 */
@RestController
@RequestMapping("/prefinancements")
public class PrefinancementController {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementController.class);
    private final PrefinancementService prefinancementService;

    public PrefinancementController(PrefinancementService prefinancementService) {
        this.prefinancementService = prefinancementService;
    }

    /**
     * Creates a new Prefinancement record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return A ResponseEntity with the newly created record and HTTP status 201.
     */
    @PostMapping
    public ResponseEntity<PrefinancementResponseDTO> createPrefinancement(@Valid @RequestBody PrefinancementRequestDTO requestDTO) throws Exception {
        PrefinancementResponseDTO responseDTO = prefinancementService.createPrefinancement(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    /**
     * Updates an existing Prefinancement record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<PrefinancementResponseDTO> updatePrefinancement(@PathVariable Long id, @Valid @RequestBody PrefinancementRequestDTO requestDTO) {
        PrefinancementResponseDTO responseDTO = prefinancementService.updatePrefinancement(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Deletes a Prefinancement record by its ID.
     *
     * @param id The ID of the record to delete.
     * @return A ResponseEntity with HTTP status 204.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrefinancement(@PathVariable Long id) {
        prefinancementService.deletePrefinancement(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a Prefinancement record by its ID.
     *
     * @param id The ID of the record to retrieve.
     * @return A ResponseEntity with the found record and HTTP status 200.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PrefinancementResponseDTO> getPrefinancementById(@PathVariable Long id) {
        PrefinancementResponseDTO responseDTO = prefinancementService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }



    /**
     * Searches for Prefinancement records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of PrefinancementResponseDTOs matching the criteria.
     */
    @GetMapping
    public ResponseEntity<Page<PrefinancementResponseDTO>> searchPrefinancements(PrefinancementSearchDTO searchDTO, @PageableDefault Pageable pageable) {
        Page<PrefinancementResponseDTO> page = prefinancementService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Changes the status of a Prefinancement record to "PAID".
     *
     * @param id The ID of the record.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PatchMapping("/{id}/status/toggle")
    public ResponseEntity<PrefinancementResponseDTO> toggleStatus(@PathVariable Long id) {
        PrefinancementResponseDTO responseDTO = prefinancementService.toggleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/remboursement/encours/{banqueId}")
    public ResponseEntity<EncoursPrefinancementDto> getEncoursForRemboursement(
            @PathVariable Long banqueId,
            EncoursPrefinancementSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursPrefinancementDto result = prefinancementService.getRemboursementEncoursForBanque(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/deblocage/encours/{banqueId}")
    public ResponseEntity<EncoursPrefinancementDto> getEncoursForDeblocage(
            @PathVariable Long banqueId,
            EncoursPrefinancementSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursPrefinancementDto result = prefinancementService.getDeblocageEncoursForBanque(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}
