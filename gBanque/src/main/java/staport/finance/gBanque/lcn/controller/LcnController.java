package staport.finance.gBanque.lcn.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.lcn.dtos.EncoursLcnResponse;
import staport.finance.gBanque.lcn.dtos.EncoursLcnSearchDto;
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


    @GetMapping("/decaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllDecaissements(
            LcnSearchDTO searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<LcnResponseDTO> result = lcnService.getDecaissements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllEncaissements(
            LcnSearchDTO searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<LcnResponseDTO> result = lcnService.getEncaissements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }


    @PatchMapping("/toggle/{id}")
    public ResponseEntity<LcnResponseDTO> toggleLcnStatus(@PathVariable Long id) {
        try {
            LcnResponseDTO updatedLcn = lcnService.toggleLcnStatus(id);
            return ResponseEntity.ok(updatedLcn);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    /**
     * Retrieves a paginated list of active LCNs for a specific bank that are due in the current month.
     * GET /lcns/encours/{banqueId}
     *
     * @param banqueId The ID of the bank.
     * @param pageable The pagination and sorting information.
     * @return A paginated list of LcnResponseDTO for the outstanding LCN.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursForLcn(
            @PathVariable Long banqueId,
            EncoursLcnSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {

        EncoursLcnResponse result = lcnService.getEncoursForLcn(searchDto,banqueId, pageable);
        return ResponseEntity.ok(result);
    }
}