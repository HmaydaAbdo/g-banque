package staport.finance.gBanque.prelevements.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.prelevements.dtos.EncoursPrelevement;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementSearchDto;
import staport.finance.gBanque.prelevements.service.PrelevementService;

@RestController
@RequestMapping("/prelevements")
public class PrelevementController {

    private static final Logger log = LogManager.getLogger(PrelevementController.class);
    private final PrelevementService prelevementService;

    public PrelevementController(PrelevementService prelevementService) {
        this.prelevementService = prelevementService;
    }


    // --- Create ---
    @PostMapping
    public ResponseEntity<PrelevementResponseDto> createPrelevement(@Validated @RequestBody PrelevementRequestDto requestDto) {
        log.info("REST request to create prelevement: {}", requestDto);
        PrelevementResponseDto responseDto = prelevementService.createPrelevement(requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<PrelevementResponseDto> getById(@PathVariable Long id) {
        log.info("REST request to get prelevement with id={}", id);
        PrelevementResponseDto responseDto = prelevementService.getById(id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<PrelevementResponseDto>> searchPrelevements(@RequestBody PrelevementSearchDto searchDto, Pageable pageable) {
        log.info("REST request to search prelevements with criteria: {}", searchDto);
        Page<PrelevementResponseDto> page = prelevementService.searchPrelevements(searchDto, pageable);
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<PrelevementResponseDto> updatePrelevement(@PathVariable Long id, @Validated @RequestBody PrelevementRequestDto requestDto) {
        log.info("REST request to update prelevement id={}", id);
        PrelevementResponseDto responseDto = prelevementService.updatePrelevement(id, requestDto);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrelevement(@PathVariable Long id) {
        log.info("REST request to delete prelevement id={}", id);
        prelevementService.deletePrelevement(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursPrelevement> getOutstandingPrelevements(
            @PathVariable Long banqueId,
            @ModelAttribute PrelevementSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("REST request to get outstanding prelevements for banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId); // Set the bank ID from the path variable
        EncoursPrelevement result = prelevementService.getOutstandingPrelevements(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

}

