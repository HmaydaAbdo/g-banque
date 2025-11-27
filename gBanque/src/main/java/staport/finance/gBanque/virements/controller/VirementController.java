package staport.finance.gBanque.virements.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.virements.dtos.*;
import staport.finance.gBanque.virements.service.VirementService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/virements")
public class VirementController {

    private final VirementService virementService;

    public VirementController(VirementService virementService) {
        this.virementService = virementService;
    }


    @PostMapping
    public ResponseEntity<VirementResponseDto> createVirement( @Valid @RequestBody VirementRequestDto dto) {
        return ResponseEntity.ok(virementService.createEncaissementVirement(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VirementResponseDto> updateVirement(@PathVariable Long id, @Valid @RequestBody VirementRequestDto dto) {
        return ResponseEntity.ok(virementService.editEncaissementVirement(id,dto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VirementResponseDto> getVirementByid(@PathVariable Long id){
        return ResponseEntity.ok(virementService.getVirementById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVirement(@PathVariable Long id) {
        virementService.deleteEncaissementVirement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<VirementResponseDto>> searchVirements(
            VirementSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<VirementResponseDto> result = virementService.searchVirements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }


    @PatchMapping("/toggle/{id}")
    public ResponseEntity<VirementResponseDto> toggleVirementStatus(@PathVariable Long id) {
        try {
            VirementResponseDto updatedVirement = virementService.toggleVirementStatus(id);
            return ResponseEntity.ok(updatedVirement);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }



    /**
     * Retrieves a paginated list of active virements for a specific bank that are due in the current month.
     * GET /virements/encours/{banqueId}
     *
     * @param banqueId The ID of the bank.
     * @param pageable The pagination and sorting information.
     * @return A paginated list of VirementResponseDto for the outstanding virements.
     */
    @GetMapping("/decaissement/encours/{banqueId}")
    public ResponseEntity<EncoursVirementResponseDto> getEncoursDecaissementForVirements(
            @PathVariable Long banqueId,
            EncoursVirementsSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

     EncoursVirementResponseDto result = virementService.getEncoursDecaissementForVirements(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }


    @GetMapping("/encaissement/encours/{banqueId}")
    public ResponseEntity<EncoursVirementResponseDto> getEncoursEncaissementForVirements(
            @PathVariable Long banqueId,
            EncoursVirementsSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        EncoursVirementResponseDto result = virementService.getEncoursEncaissementForVirements(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}