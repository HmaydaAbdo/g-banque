package staport.finance.gBanque.virements.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.virements.dtos.EncoursVirementResponseDto;
import staport.finance.gBanque.virements.dtos.EncoursVirementsSearchDto;
import staport.finance.gBanque.virements.dtos.VirementResponseDto;
import staport.finance.gBanque.virements.dtos.VirementSearchDto;
import staport.finance.gBanque.virements.service.VirementService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/virements")
public class VirementController {

    private final VirementService virementService;

    public VirementController(VirementService virementService) {
        this.virementService = virementService;
    }

    /**
     * Searches for Virements with pagination and filtering.
     * <p>
     * Endpoint: GET /virements
     * Example: /virements?beneficiaire=John&active=true&page=0&size=20
     *
     * @param searchDto The VirementSearchDto populated from request parameters.
     * @param pageable  The Pageable object from the request, with a default size and sorting.
     * @return A paginated list of VirementResponseDto.
     */
    @GetMapping
    public ResponseEntity<Page<VirementResponseDto>> searchVirements(
            VirementSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<VirementResponseDto> result = virementService.searchVirements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Toggles the active status of a Virement.
     * <p>
     * Endpoint: PATCH /virements/toggle/{numero}
     *
     * @param numero The numero of the Virement to toggle.
     * @return The updated VirementResponseDto.
     */
    @PatchMapping("/toggle/{numero}")
    public ResponseEntity<VirementResponseDto> toggleVirementStatus(@PathVariable String numero) {
        try {
            VirementResponseDto updatedVirement = virementService.toggleVirementStatus(numero);
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
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursVirementResponseDto> getEncoursForVirements(
            @PathVariable Long banqueId,
            EncoursVirementsSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

     EncoursVirementResponseDto result = virementService.getEncoursForVirements(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}