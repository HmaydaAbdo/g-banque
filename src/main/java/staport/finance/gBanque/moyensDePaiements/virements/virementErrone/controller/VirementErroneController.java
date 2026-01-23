package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.controller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.services.VirementErroneService;

@RestController
@RequestMapping("/virement-errone")
public class VirementErroneController {

    private static final Logger log = LoggerFactory.getLogger(VirementErroneController.class);
    private final VirementErroneService virementErroneService;

    public VirementErroneController(VirementErroneService virementErroneService) {
        this.virementErroneService = virementErroneService;
        log.info("Initialisation du contrôleur VirementErrone");
    }

    @PostMapping
    public ResponseEntity<VirementErroneResponseDto> createVirementErrone(@Validated @RequestBody VirementErroneRequestDto requestDto) {
        log.info("Requête REST pour créer un virement erroné - Libellé: '{}', Montant: {}",
                requestDto.getLibelle(), requestDto.getMontant());

        VirementErroneResponseDto response = virementErroneService.createVirementErrone(requestDto);
        log.info("Virement erroné créé avec succès - ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VirementErroneResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer le virement erroné ID: {}", id);
        VirementErroneResponseDto response = virementErroneService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<VirementErroneResponseDto>> searchVirementErrone(@RequestBody VirementErroneSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des virements erronés - Critères: {}", searchDto);
        Page<VirementErroneResponseDto> page = virementErroneService.searchVirementErrone(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VirementErroneResponseDto> updateVirementErrone(@PathVariable Long id, @Validated @RequestBody VirementErroneRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour le virement erroné ID: {}", id);
        VirementErroneResponseDto response = virementErroneService.updateVirementErrone(id, requestDto);
        log.info("Virement erroné ID: {} mis à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVirementErrone(@PathVariable Long id) {
        log.info("Requête REST pour supprimer le virement erroné ID: {}", id);
        virementErroneService.deleteVirementErrone(id);
        log.info("Virement erroné ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursVirementErroneDto> getOutstandingVirementErrone(
            @PathVariable Long banqueId,
            @ModelAttribute VirementErroneSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les virements erronés en cours - Banque ID: {}", banqueId);
        searchDto.setBanqueId(banqueId);
        EncoursVirementErroneDto result = virementErroneService.getOutstandingVirementErrone(searchDto, pageable);
        log.info("Virements erronés en cours récupérés - Total: {}", result.getTotalDesEncours());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<VirementErroneResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut du virement erroné ID: {}", id);
        VirementErroneResponseDto response = virementErroneService.toggleStatus(id);
        log.info("Statut du virement erroné ID: {} basculé vers {}", id, response.getStatus());
        return ResponseEntity.ok(response);
    }
}