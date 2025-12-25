package staport.finance.gBanque.interets.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.interets.dtos.EncoursInteretDto;
import staport.finance.gBanque.interets.dtos.InteretRequestDto;
import staport.finance.gBanque.interets.dtos.InteretResponseDto;
import staport.finance.gBanque.interets.dtos.InteretSearchDto;
import staport.finance.gBanque.interets.service.InteretService;

@RestController
@RequestMapping("/interest")
public class InteretController {

    private static final Logger log = LogManager.getLogger(InteretController.class);
    private final InteretService interetService;

    public InteretController(InteretService interetService) {
        this.interetService = interetService;
    }


    // --- Create ---
    @PostMapping
    public ResponseEntity<InteretResponseDto> createInterest(@Validated @RequestBody InteretRequestDto requestDto) {
        log.info("REST request to create interest: {}", requestDto);
        InteretResponseDto responseDto = interetService.createInterest(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<InteretResponseDto> getById(@PathVariable Long id) {
        log.info("REST request to get interest with id={}", id);
        InteretResponseDto responseDto = interetService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<InteretResponseDto>> searchInterest(@RequestBody InteretSearchDto searchDto, Pageable pageable) {
        log.info("REST request to search interest with criteria: {}", searchDto);
        Page<InteretResponseDto> page = interetService.searchInterest(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<InteretResponseDto> updateInterest(@PathVariable Long id, @Validated @RequestBody InteretRequestDto requestDto) {
        log.info("REST request to update interest id={}", id);
        InteretResponseDto responseDto = interetService.updateInterest(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterest(@PathVariable Long id) {
        log.info("REST request to delete interest id={}", id);
        interetService.deleteInterest(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of outstanding interest and their total amount for a specific bank.
     *
     * @param banqueId  The ID of the bank to filter by.
     * @param searchDto Optional search criteria (e.g., for filtering by libelle, etc.).
     * @param pageable  Pagination and sorting information.
     * @return An EncoursInteretDto containing the page of interest and the total outstanding amount.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursInteretDto> getOutstandingInterest(@PathVariable Long banqueId, @ModelAttribute InteretSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding interest for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId); // Set the bank ID from the path variable
        EncoursInteretDto result = interetService.getOutstandingInterest(searchDto, pageable);
        return ResponseEntity.ok(result);
    }
}


