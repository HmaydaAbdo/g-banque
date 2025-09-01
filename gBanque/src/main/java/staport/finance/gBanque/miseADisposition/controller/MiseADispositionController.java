package staport.finance.gBanque.miseADisposition.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionResponseDto;
import staport.finance.gBanque.miseADisposition.dtos.MiseADispositionSearchDto;
import staport.finance.gBanque.miseADisposition.services.MiseADispositionService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/misesADisposition")
public class MiseADispositionController {

    private final MiseADispositionService miseADispositionService;

    public MiseADispositionController(MiseADispositionService miseADispositionService) {
        this.miseADispositionService = miseADispositionService;
    }

    /**
     * Searches for MiseADisposition entities with pagination and filtering.
     * <p>
     * Endpoint: GET /misesADisposition
     * Example: /misesADisposition?cinBeneficiaire=A123456&active=true&page=0&size=20
     *
     * @param searchDto The MiseADispositionSearchDto populated from request parameters.
     * @param pageable The Pageable object from the request, with a default size and sorting.
     * @return A paginated list of MiseADispositionResponseDto.
     */
    @GetMapping
    public ResponseEntity<Page<MiseADispositionResponseDto>> searchMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        Page<MiseADispositionResponseDto> result = miseADispositionService.searchMisesADisposition(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Toggles the active status of a MiseADisposition.
     * <p>
     * Endpoint: PATCH /misesADisposition/toggle/{numero}
     *
     * @param numero The numero of the MiseADisposition to toggle.
     * @return The updated MiseADispositionResponseDto.
     */
    @PatchMapping("/toggle/{numero}")
    public ResponseEntity<MiseADispositionResponseDto> toggleMiseADispositionStatus(@PathVariable String numero) {
        try {
            MiseADispositionResponseDto updatedMiseADisposition = miseADispositionService.toggleMiseADispositionStatus(numero);
            return ResponseEntity.ok(updatedMiseADisposition);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}
