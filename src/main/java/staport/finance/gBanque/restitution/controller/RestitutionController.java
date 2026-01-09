package staport.finance.gBanque.restitution.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.restitution.dtos.EncoursRestitution;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;
import staport.finance.gBanque.restitution.service.RestitutionService;

@RestController
@RequestMapping("/restitutions")
public class RestitutionController {

    private static final Logger log = LogManager.getLogger(RestitutionController.class);
    private final RestitutionService restitutionService;

    public RestitutionController(RestitutionService restitutionService) {
        this.restitutionService = restitutionService;
    }

    // --- Create ---
    @PostMapping
    public ResponseEntity<RestitutionResponseDto> createRestitution(@Validated @RequestBody RestitutionRequestDto requestDto) {
        log.info("Requête REST pour créer une restitution: {}", requestDto);
        RestitutionResponseDto responseDto = restitutionService.createRestitution(requestDto);
        log.info("Restitution créée avec succès: id={}", responseDto.getId());
        return ResponseEntity.ok(responseDto);
    }

    // --- Get by ID ---
    @GetMapping("/{id}")
    public ResponseEntity<RestitutionResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer la restitution avec l'id={}", id);
        RestitutionResponseDto responseDto = restitutionService.getById(id);
        log.info("Restitution récupérée avec succès: id={}", id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Search with pagination ---
    @PostMapping("/search")
    public ResponseEntity<Page<RestitutionResponseDto>> searchRestitutions(
            @RequestBody RestitutionSearchDto searchDto,
            Pageable pageable) {
        log.info("Requête REST pour rechercher des restitutions avec les critères: {}", searchDto);
        Page<RestitutionResponseDto> page = restitutionService.searchRestitutions(searchDto, pageable);
        log.info("Recherche terminée: {} restitutions trouvées sur {} page(s)",
                page.getTotalElements(), page.getTotalPages());
        return ResponseEntity.ok(page);
    }

    // --- Update ---
    @PutMapping("/{id}")
    public ResponseEntity<RestitutionResponseDto> updateRestitution(
            @PathVariable Long id,
            @Validated @RequestBody RestitutionRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour la restitution id={} avec les données: {}", id, requestDto);
        RestitutionResponseDto responseDto = restitutionService.updateRestitution(id, requestDto);
        log.info("Restitution mise à jour avec succès: id={}", id);
        return ResponseEntity.ok(responseDto);
    }

    // --- Delete ---
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRestitution(@PathVariable Long id) {
        log.info("Requête REST pour supprimer la restitution id={}", id);
        restitutionService.deleteRestitution(id);
        log.info("Restitution supprimée avec succès: id={}", id);
        return ResponseEntity.noContent().build();
    }

    // --- Get Outstanding Restitutions ---
    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursRestitution> getOutstandingRestitutions(
            @PathVariable Long banqueId,
            @ModelAttribute RestitutionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les encours de restitutions pour la banque id={}", banqueId);
        searchDto.setBanqueId(banqueId); // Définir l'ID de la banque depuis le path variable
        EncoursRestitution result = restitutionService.getOutstandingRestitutions(searchDto, pageable);
        log.info("Encours de restitutions récupérés: {} restitutions, total des encours={}",
                result.getPage().getTotalElements(), result.getTotalDesEncours());
        return ResponseEntity.ok(result);
    }
}