package staport.finance.gBanque.operationErrone.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.operationErrone.dtos.*;
import staport.finance.gBanque.operationErrone.services.OperationErroneService;

@RestController
@RequestMapping("/operation-errone")
public class OperationErroneController {

    private static final Logger log = LoggerFactory.getLogger(OperationErroneController.class);
    private final OperationErroneService operationErroneService;

    public OperationErroneController(OperationErroneService operationErroneService) {
        this.operationErroneService = operationErroneService;
        log.info("Initialisation du contrôleur OperationErrone");
    }

    @PostMapping
    public ResponseEntity<OperationErroneResponseDto> createOperationErrone(@Validated @RequestBody OperationErroneRequestDto requestDto) {
        log.info("Requête REST pour créer une opération erronée - Libellé: '{}', Montant: {}, Motif: {}",
                requestDto.getLibelle(), requestDto.getMontant(), requestDto.getMotif());

        OperationErroneResponseDto response = operationErroneService.createOperationErrone(requestDto);
        log.info("Opération erronée créée avec succès - ID: {}, Motif: {}", response.getId(), response.getMotif());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OperationErroneResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer l'opération erronée ID: {}", id);
        OperationErroneResponseDto response = operationErroneService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<OperationErroneResponseDto>> searchOperationErrone(@RequestBody OperationErroneSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des opérations erronées - Critères: {}", searchDto);
        Page<OperationErroneResponseDto> page = operationErroneService.searchOperationErrone(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OperationErroneResponseDto> updateOperationErrone(@PathVariable Long id, @Validated @RequestBody OperationErroneRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour l'opération erronée ID: {}", id);
        OperationErroneResponseDto response = operationErroneService.updateOperationErrone(id, requestDto);
        log.info("Opération erronée ID: {} mise à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOperationErrone(@PathVariable Long id) {
        log.info("Requête REST pour supprimer l'opération erronée ID: {}", id);
        operationErroneService.deleteOperationErrone(id);
        log.info("Opération erronée ID: {} supprimée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursOperationErroneDto> getOutstandingOperationErrone(
            @PathVariable Long banqueId,
            @ModelAttribute OperationErroneSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les opérations erronées en cours - Banque ID: {}", banqueId);
        searchDto.setBanqueId(banqueId);
        EncoursOperationErroneDto result = operationErroneService.getOutstandingOperationErrone(searchDto, pageable);
        log.info("Opérations erronées en cours récupérées - Total: {}", result.getTotalDesEncours());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<OperationErroneResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut de l'opération erronée ID: {}", id);
        OperationErroneResponseDto response = operationErroneService.toggleStatus(id);
        log.info("Statut de l'opération erronée ID: {} basculé vers {}", id, response.getStatus());
        return ResponseEntity.ok(response);
    }
}