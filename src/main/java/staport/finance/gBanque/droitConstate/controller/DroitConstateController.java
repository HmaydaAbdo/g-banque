package staport.finance.gBanque.droitConstate.controller;


import jakarta.validation.Valid;
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

    private final DroitConstateService droitConstateService;

    public DroitConstateController(DroitConstateService droitConstateService) {
        this.droitConstateService = droitConstateService;
    }

    /**
     * Creates a new DroitConstate record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return A ResponseEntity with the newly created record and HTTP status 201.
     */
    @PostMapping
    public ResponseEntity<DroitConstateResponseDto> createDroitConstate(@Valid @RequestBody DroitConstateRequestDto requestDTO) throws Exception {
        DroitConstateResponseDto responseDTO = droitConstateService.createDroitConstate(requestDTO);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    /**
     * Updates an existing Droit constate record.
     *
     * @param id The ID of the record to update.
     * @param requestDTO The DTO containing the updated data.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DroitConstateResponseDto> updateDroitConstate(@PathVariable Long id, @Valid @RequestBody DroitConstateRequestDto requestDTO) {
        DroitConstateResponseDto responseDTO = droitConstateService.updateDroitConstate(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Deletes a DroitConstate record by its ID.
     *
     * @param id The ID of the record to delete.
     * @return A ResponseEntity with HTTP status 204.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDroitConstate(@PathVariable Long id) {
        droitConstateService.deleteDroitConstate(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a DroitConstate record by its ID.
     *
     * @param id The ID of the record to retrieve.
     * @return A ResponseEntity with the found record and HTTP status 200.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DroitConstateResponseDto> getDroitConstateById(@PathVariable Long id) {
        DroitConstateResponseDto responseDTO = droitConstateService.findById(id);
        return ResponseEntity.ok(responseDTO);
    }



    /**
     * Searches for DC records based on criteria.
     *
     * @param searchDTO The DTO containing the search criteria.
     * @param pageable Pagination and sorting information.
     * @return A Page of DroitConstateResponseDto matching the criteria.
     */
    @GetMapping
    public ResponseEntity<Page<DroitConstateResponseDto>> searchDroitConstates(DroitConstateSearchDto searchDTO, @PageableDefault Pageable pageable) {
        Page<DroitConstateResponseDto> page = droitConstateService.findAll(searchDTO, pageable);
        return ResponseEntity.ok(page);
    }

    /**
     * Changes the status of a DC record to "PAID".
     *
     * @param id The ID of the record.
     * @return A ResponseEntity with the updated record and HTTP status 200.
     */
    @PatchMapping("/{id}/status/toggle")
    public ResponseEntity<DroitConstateResponseDto> toggleStatus(@PathVariable Long id) {
        DroitConstateResponseDto responseDTO = droitConstateService.toggleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/remboursement/encours/{banqueId}")
    public ResponseEntity<EncoursDroitConstateDto> getEncoursForRemboursement(
            @PathVariable Long banqueId,
            EncoursDroitConstateSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursDroitConstateDto result = droitConstateService.getRemboursementEncoursForBanque(searchDTO,banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/deblocage/encours/{banqueId}")
    public ResponseEntity<EncoursDroitConstateDto> getEncoursForDeblocage(
            @PathVariable Long banqueId,
            EncoursDroitConstateSearchDto searchDTO,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursDroitConstateDto result = droitConstateService.getDeblocageEncoursForBanque(searchDTO,banqueId, pageable);
        return ResponseEntity.ok(result);
    }


}
