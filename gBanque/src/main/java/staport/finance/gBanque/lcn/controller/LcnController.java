package staport.finance.gBanque.lcn.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.lcn.dtos.LcnResponseDTO;
import staport.finance.gBanque.lcn.dtos.LcnSearchDTO;
import staport.finance.gBanque.lcn.services.LcnService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/lcns")
public class LcnController {

    private final LcnService lcnService;

    public LcnController(LcnService lcnService) {
        this.lcnService = lcnService;
    }

    /**
     * Searches for LCNs with pagination and filtering.
     *
     * @param searchDto The LcnSearchDTO populated from request parameters.
     * @param pageable  The Pageable object from the request, with a default size and sorting.
     * @return A paginated list of LcnResponseDTO.
     */
    @GetMapping
    public ResponseEntity<Page<LcnResponseDTO>> searchLcns(
            LcnSearchDTO searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<LcnResponseDTO> result = lcnService.searchLcns(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Toggles the active status of an LCN.
     *
     * PATCH /lcns/toggle/{numero}
     *
     * @param numero The numero of the LCN to toggle.
     * @return The updated LcnResponseDTO.
     */
    @PatchMapping("/toggle/{numero}")
    public ResponseEntity<LcnResponseDTO> toggleLcnStatus(@PathVariable String numero) {
        try {
            LcnResponseDTO updatedLcn = lcnService.toggleLcnStatus(numero);
            return ResponseEntity.ok(updatedLcn);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }
}