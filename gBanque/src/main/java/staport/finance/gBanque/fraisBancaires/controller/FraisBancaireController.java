package staport.finance.gBanque.fraisBancaires.controller;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResposneDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.service.FraisBancaireService;

@RestController
@RequestMapping("/frais-bancaire")
public class FraisBancaireController {

    private static final Logger log = LogManager.getLogger(FraisBancaireController.class);
    private final FraisBancaireService fraisBancaireService;

    public FraisBancaireController(FraisBancaireService fraisBancaireService) {
        this.fraisBancaireService = fraisBancaireService;
    }

    // --- Create ---
    @PostMapping
    public ResponseEntity<FraisBancaireResposneDto> createFraisBancaire(
            @Validated @RequestBody FraisBancaireRequestDto requestDto) {
        log.info("REST request to create frais bancaire: {}", requestDto);
        FraisBancaireResposneDto responseDto = fraisBancaireService.createFraisBancaire(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<FraisBancaireResposneDto> getById(@PathVariable Long id) {
        log.info("REST request to get frais bancaire with id={}", id);
        FraisBancaireResposneDto responseDto = fraisBancaireService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<FraisBancaireResposneDto>> searchFraisBancaire(
            @RequestBody FraisBancaireSearchDto searchDto,
            Pageable pageable) {
        log.info("REST request to search frais bancaires with criteria: {}", searchDto);
        Page<FraisBancaireResposneDto> page = fraisBancaireService.searchFraisBancaire(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<FraisBancaireResposneDto> updateFraisBancaire(
            @PathVariable Long id,
            @Validated @RequestBody FraisBancaireRequestDto requestDto) {
        log.info("REST request to update frais bancaire id={}", id);
        FraisBancaireResposneDto responseDto = fraisBancaireService.updateFraisBancaire(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFraisBancaire(@PathVariable Long id) {
        log.info("REST request to delete frais bancaire id={}", id);
        fraisBancaireService.deleteFraisBancaire(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves a paginated list of outstanding fraisBancaire and their total amount for a specific bank.
     *
     * @param banqueId  The ID of the bank to filter by.
     * @param searchDto Optional search criteria (e.g., for filtering by libelle, etc.).
     * @param pageable  Pagination and sorting information.
     * @return An EncoursFraisBancaireDto containing the page of frais bancaires and the total outstanding amount.
     */
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursFraisBancaireDto> getOutstandingFraisBancaire(
            @PathVariable Long banqueId,
            @ModelAttribute FraisBancaireSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding frais bancaire for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId); // Set the bank ID from the path variable
        EncoursFraisBancaireDto result = fraisBancaireService.getOutstandingFraisBancaire(searchDto, pageable);
        return ResponseEntity.ok(result);
    }
}

