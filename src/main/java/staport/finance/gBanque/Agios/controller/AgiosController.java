package staport.finance.gBanque.Agios.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.dtos.EncoursAgiosDto;
import staport.finance.gBanque.Agios.service.AgiosService;


@RestController
@RequestMapping("/agios")
public class AgiosController {

    private static final Logger log = LogManager.getLogger(AgiosController.class);
    private final AgiosService agiosService;

    public AgiosController(AgiosService agiosService) {
        this.agiosService = agiosService;
    }


    // --- Create ---
    @PostMapping
    public ResponseEntity<AgiosResponseDto> createAgios(@Validated @RequestBody AgiosRequestDto requestDto) {
        log.info("REST request to create agios: {}", requestDto);
        AgiosResponseDto responseDto = agiosService.createAgios(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<AgiosResponseDto> getById(@PathVariable Long id) {
        log.info("REST request to get agios with id={}", id);
        AgiosResponseDto responseDto = agiosService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<AgiosResponseDto>> searchAgios(@RequestBody AgiosSearchDto searchDto, Pageable pageable) {
        log.info("REST request to search agios with criteria: {}", searchDto);
        Page<AgiosResponseDto> page = agiosService.searchAgios(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<AgiosResponseDto> updateAgios(@PathVariable Long id, @Validated @RequestBody AgiosRequestDto requestDto) {
        log.info("REST request to update agios id={}", id);
        AgiosResponseDto responseDto = agiosService.updateAgios(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAgios(@PathVariable Long id) {
        log.info("REST request to delete agios id={}", id);
        agiosService.deleteAgios(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of outstanding agios and their total amount for a specific bank.
     *
     * @param banqueId  The ID of the bank to filter by.
     * @param searchDto Optional search criteria (e.g., for filtering by libelle, etc.).
     * @param pageable  Pagination and sorting information.
     * @return An EncoursAgiosDto containing the page of agios and the total outstanding amount.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursAgiosDto> getOutstandingAgios(
            @PathVariable Long banqueId,
            @ModelAttribute AgiosSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding agios for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId); // Set the bank ID from the path variable
        EncoursAgiosDto result = agiosService.getOutstandingAgios(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

}
