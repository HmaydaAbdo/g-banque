package staport.finance.gBanque.interets.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.interets.dtos.*;
import staport.finance.gBanque.interets.service.InteretService;

@RestController
@RequestMapping("/interest")
public class InteretController {

    private static final Logger log = LoggerFactory.getLogger(InteretController.class);
    private final InteretService interetService;

    public InteretController(InteretService interetService) {
        this.interetService = interetService;
        log.info("Initialisation du contrôleur Interet");
    }

    @PostMapping
    public ResponseEntity<InteretResponseDto> createInterest(@Validated @RequestBody InteretRequestDto requestDto) {
        log.info("Requête REST pour créer un intérêt - Libellé: '{}', Montant: {}",
                requestDto.getLibelle(), requestDto.getMontant());

        InteretResponseDto response = interetService.createInterest(requestDto);
        log.info("Intérêt créé avec succès - ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InteretResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer l'intérêt ID: {}", id);
        InteretResponseDto response = interetService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<InteretResponseDto>> searchInterest(@RequestBody InteretSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des intérêts - Critères: {}", searchDto);
        Page<InteretResponseDto> page = interetService.searchInterest(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InteretResponseDto> updateInterest(@PathVariable Long id, @Validated @RequestBody InteretRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour l'intérêt ID: {}", id);
        InteretResponseDto response = interetService.updateInterest(id, requestDto);
        log.info("Intérêt ID: {} mis à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInterest(@PathVariable Long id) {
        log.info("Requête REST pour supprimer l'intérêt ID: {}", id);
        interetService.deleteInterest(id);
        log.info("Intérêt ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursInteretDto> getOutstandingInterest(
            @PathVariable Long banqueId,
            @ModelAttribute InteretSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les intérêts en cours - Banque ID: {}", banqueId);
        searchDto.setBanqueId(banqueId);
        EncoursInteretDto result = interetService.getOutstandingInterest(searchDto, pageable);
        log.info("Intérêts en cours récupérés - Total: {}", result.getTotalDesEncours());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<InteretResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut de l'intérêt ID: {}", id);
        InteretResponseDto response = interetService.toggleStatus(id);
        log.info("Statut de l'intérêt ID: {} basculé vers {}", id, response.getStatus());
        return ResponseEntity.ok(response);
    }
}