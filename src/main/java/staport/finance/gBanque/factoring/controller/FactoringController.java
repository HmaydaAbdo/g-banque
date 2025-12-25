package staport.finance.gBanque.factoring.controller;

import jakarta.validation.Valid;
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

    private final FactoringService factoringService;

    public FactoringController(FactoringService factoringService) {
        this.factoringService = factoringService;
    }


    /**
     * Creates a new Factoring record.
     *
     * @param requestDTO The DTO containing the data for the new record.
     * @return A ResponseEntity with the newly created record and HTTP status 201.
     */
    @PostMapping
    public ResponseEntity<FactoringResponseDto> createFactoring(@Valid @RequestBody FactoringRequestDto requestDTO) throws Exception {
        FactoringResponseDto responseDTO = factoringService.createFactoring(requestDTO);
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
    public ResponseEntity<FactoringResponseDto> updateFactoring(@PathVariable Long id, @Valid @RequestBody FactoringRequestDto requestDTO) {
        FactoringResponseDto responseDTO = factoringService.updateFactoring(id, requestDTO);
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
        factoringService.deleteFactoring(id);
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
        FactoringResponseDto responseDTO = factoringService.findById(id);
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
        Page<FactoringResponseDto> page = factoringService.findAll(searchDTO, pageable);
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
        FactoringResponseDto responseDTO = factoringService.toggleStatus(id);
        return ResponseEntity.ok(responseDTO);
    }

    @GetMapping("/remboursement/encours/{banqueId}")
    public ResponseEntity<EncoursFactoringDto> getEncoursForRemboursement(
            @PathVariable Long banqueId,
            EncoursFactoringSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursFactoringDto result = factoringService.getRemboursementEncoursForBanque(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/deblocage/encours/{banqueId}")
    public ResponseEntity<EncoursFactoringDto> getEncoursForDeblocage(
            @PathVariable Long banqueId,
            EncoursFactoringSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        EncoursFactoringDto result = factoringService.getDeblocageEncoursForBanque(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}
