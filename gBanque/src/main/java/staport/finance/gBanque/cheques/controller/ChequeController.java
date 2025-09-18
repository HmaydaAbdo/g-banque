package staport.finance.gBanque.cheques.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cheques.dtos.ChequeResponseDto;
import staport.finance.gBanque.cheques.dtos.ChequeSearchDto;
import staport.finance.gBanque.cheques.dtos.EncoursChequeResponseDto;
import staport.finance.gBanque.cheques.dtos.EncoursChequeSearchDto;
import staport.finance.gBanque.cheques.services.ChequeService;


import java.util.NoSuchElementException;

@RestController
@RequestMapping("/cheques")
public class ChequeController {

    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    /**
     * Searches for cheques with pagination and filtering.
     *
     * @param searchDto The ChequeSearchDto populated from request parameters.
     * @param pageable  The Pageable object from the request.
     * @return A paginated list of ChequeResponseDto.
     */
    @GetMapping
    public ResponseEntity<Page<ChequeResponseDto>> searchCheques(
            ChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        Page<ChequeResponseDto> result = chequeService.searchCheques(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Toggles the active status of a cheque.
     *
     * PATCH /cheques/toggle/{numero}
     *
     * @param numero The numero of the cheque to toggle.
     * @return The updated ChequeResponseDto.
     */
    @PatchMapping("/toggle/{numero}")
    public ResponseEntity<ChequeResponseDto> toggleChequeStatus(@PathVariable String numero) {
        try {
            ChequeResponseDto updatedCheque = chequeService.toggleChequeStatus(numero);
            return ResponseEntity.ok(updatedCheque);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }


    /**
     * Retrieves a paginated list of active cheques for a specific bank that are due in the current month.
     * GET /cheques/encours/{banqueId}
     *
     * @param banqueId The ID of the bank.
     * @param pageable The pagination and sorting information.
     * @return A paginated list of ChequeResponseDto for the outstanding cheques.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getEncoursForCheques(searchDto,banqueId, pageable));
    }
}