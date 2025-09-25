package staport.finance.gBanque.commision.controller;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.commision.dtos.*;
import staport.finance.gBanque.commision.service.CommissionService;

@RestController
@RequestMapping("/commissions")
public class CommissionController {

    private static final Logger log = LogManager.getLogger(CommissionController.class);
    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    // --- Create ---
    @PostMapping
    public ResponseEntity<CommissionResponseDto> createCommission(
            @Validated @RequestBody CommissionRequestDto requestDto) {
        log.info("REST request to create commission: {}", requestDto);
        CommissionResponseDto responseDto = commissionService.createCommission(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<CommissionResponseDto> getById(@PathVariable Long id) {
        log.info("REST request to get commission with id={}", id);
        CommissionResponseDto responseDto = commissionService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<CommissionResponseDto>> searchCommissions(
            @RequestBody CommissionSearchDto searchDto,
            Pageable pageable) {
        log.info("REST request to search commissions with criteria: {}", searchDto);
        Page<CommissionResponseDto> page = commissionService.searchCommissions(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<CommissionResponseDto> updateCommission(
            @PathVariable Long id,
            @Validated @RequestBody CommissionRequestDto requestDto) {
        log.info("REST request to update commission id={}", id);
        CommissionResponseDto responseDto = commissionService.updateCommission(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCommission(@PathVariable Long id) {
        log.info("REST request to delete commission id={}", id);
        commissionService.deleteCommission(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of outstanding commissions and their total amount for a specific bank.
     *
     * @param banqueId  The ID of the bank to filter by.
     * @param searchDto Optional search criteria (e.g., for filtering by libelle, etc.).
     * @param pageable  Pagination and sorting information.
     * @return An EncoursCommissionDto containing the page of commissions and the total outstanding amount.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursCommissionDto> getOutstandingCommissions(
            @PathVariable Long banqueId,
            @ModelAttribute CommissionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding commissions for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId); // Set the bank ID from the path variable
        EncoursCommissionDto result = commissionService.getOutstandingCommissions(searchDto, pageable);
        return ResponseEntity.ok(result);
    }
}

