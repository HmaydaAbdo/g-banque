package staport.finance.gBanque.impot_taxes.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.impot_taxes.dtos.*;
import staport.finance.gBanque.impot_taxes.service.ImpotEtTaxeService;

@RestController
@RequestMapping("/impots-taxes")
public class ImpotEtTaxeController {

    private static final Logger log = LogManager.getLogger(ImpotEtTaxeController.class);
    private final ImpotEtTaxeService impotEtTaxeService;

    public ImpotEtTaxeController(ImpotEtTaxeService impotEtTaxeService) {
        this.impotEtTaxeService = impotEtTaxeService;
    }

    // --- Create ---
    @PostMapping
    public ResponseEntity<ImpotEtTaxeResponseDto> createImpotEtTaxe(@Validated @RequestBody ImpotEtTaxeRequestDto requestDto) {
        log.info("REST request to create impot et taxe: {}", requestDto);
        ImpotEtTaxeResponseDto responseDto = impotEtTaxeService.createImpotEtTaxe(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<ImpotEtTaxeResponseDto> getById(@PathVariable Long id) {
        log.info("REST request to get impot et taxe with id={}", id);
        ImpotEtTaxeResponseDto responseDto = impotEtTaxeService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<ImpotEtTaxeResponseDto>> searchImpotEtTaxes(@RequestBody ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("REST request to search impots et taxes with criteria: {}", searchDto);
        Page<ImpotEtTaxeResponseDto> page = impotEtTaxeService.searchImpotEtTaxes(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<ImpotEtTaxeResponseDto> updateImpotEtTaxe(@PathVariable Long id, @Validated @RequestBody ImpotEtTaxeRequestDto requestDto) {
        log.info("REST request to update impot et taxe id={}", id);
        ImpotEtTaxeResponseDto responseDto = impotEtTaxeService.updateImpotEtTaxe(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImpotEtTaxe(@PathVariable Long id) {
        log.info("REST request to delete impot et taxe id={}", id);
        impotEtTaxeService.deleteImpotEtTaxe(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of outstanding impots et taxes and their total amount for a specific bank.
     *
     * @param banqueId  The ID of the bank to filter by.
     * @param searchDto Optional search criteria (e.g., for filtering by libelle, etc.).
     * @param pageable  Pagination and sorting information.
     * @return An EncoursImpotEtTaxeDto containing the page of impots et taxes and the total outstanding amount.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursImpotEtTaxeDto> getOutstandingImpotEtTaxes(@PathVariable Long banqueId, @ModelAttribute ImpotEtTaxeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding impots et taxes for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId);
        EncoursImpotEtTaxeDto result = impotEtTaxeService.getOutstandingImpotEtTaxes(searchDto, pageable);
        return ResponseEntity.ok(result);
    }
}