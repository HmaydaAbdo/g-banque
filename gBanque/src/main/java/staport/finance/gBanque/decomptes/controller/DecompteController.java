package staport.finance.gBanque.decomptes.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.decomptes.dto.*;
import staport.finance.gBanque.decomptes.service.DecompteService;


/**
 * REST controller for managing décomptes records.
 * Provides a complete CRUD and search API.
 */
@RestController
@RequestMapping("/decomptes")
public class DecompteController {

    private final DecompteService decompteService;


    public DecompteController(DecompteService decompteService) {
        this.decompteService = decompteService;
    }

    /**
     * Creates a new Décompte record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return A ResponseEntity with the newly created record and HTTP status 201.
     */
    @PostMapping
    public ResponseEntity<DecompteResponseDto> createDecompte(@Valid @RequestBody DecompteRequestDto requestDTO) {
        DecompteResponseDto responseDTO = decompteService.createDecompte(requestDTO);
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
        DecompteResponseDto responseDTO = decompteService.updateDecompte(id, requestDTO);
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
        decompteService.deleteDecompte(id);
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
        DecompteResponseDto responseDTO = decompteService.findById(id);
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
        Page<DecompteResponseDto> page = decompteService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Changes the status of a décompte record to "PAID".
     *
     * @param id The ID of the record.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PatchMapping("/{id}/status/paid")
    public ResponseEntity<DecompteResponseDto> toggleStatus(@PathVariable Long id) {
        DecompteResponseDto responseDTO = decompteService.toglleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }


    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursDecompteDto> getEncoursForDecomptes(
            @PathVariable Long banqueId,
            EncoursDecompteSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursDecompteDto result = decompteService.getDecomptesEncoursForBanque(searchDTO,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}
