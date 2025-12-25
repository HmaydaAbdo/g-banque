package staport.finance.gBanque.moyensDePaiements.virements.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.virements.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.service.VirementService;

@RestController
@RequestMapping("/virements")
public class VirementController {


    private static final Logger log = LoggerFactory.getLogger(VirementController.class);
    private final VirementService virementService;

    public VirementController(VirementService virementService) {
        this.virementService = virementService;
    }


    @PostMapping
    public ResponseEntity<VirementResponseDto> createVirement(@Valid @RequestBody VirementRequestDto dto) {
        log.info("Réception d'une demande de création de virement ,  le dto est le suivant : {}", dto);
        VirementResponseDto response = virementService.createEncaissementVirement(dto);
        log.info("Virement créé avec succès. ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VirementResponseDto> updateVirement(@PathVariable Long id, @Valid @RequestBody VirementRequestDto dto) {
        log.info("Réception d'une demande de modification du virement ID  {} avec le dto suivant : {}", id,dto);
        VirementResponseDto response = virementService.editEncaissementVirement(id, dto);
        log.info("Virement ID: {} modifié avec succès", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VirementResponseDto> getVirementByid(@PathVariable Long id) {
        log.debug("Récupération du virement ID: {}", id);
        VirementResponseDto response = virementService.getVirementById(id);
        log.debug("Virement ID: {} récupéré avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVirement(@PathVariable Long id) {
        log.info("Réception d'une demande de suppression du virement ID: {}", id);
        virementService.deleteEncaissementVirement(id);
        log.info("Virement ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<VirementResponseDto>> getEncaissementVirements(VirementSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des virements d'encaissement - Page: {}, Taille: {},searchDto : {}", pageable.getPageNumber(), pageable.getPageSize(),searchDto);
        Page<VirementResponseDto> result = virementService.getEncaissementVirements(searchDto, pageable);
        log.info("Nombre de virements d'encaissement récupérés: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<VirementResponseDto>> getDecaissementVirements(VirementSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des virements de décaissement - Page: {}, Taille: {},search Dto {}", pageable.getPageNumber(), pageable.getPageSize(),searchDto);
        Page<VirementResponseDto> result = virementService.getDecaissementVirements(searchDto, pageable);
        log.info("Nombre de virements de décaissement récupérés: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }


    @PatchMapping("/toggle/{id}")
    public ResponseEntity<VirementResponseDto> toggleVirementStatus(@PathVariable Long id) {
        log.info("Demande de changement du statut du virement ID: {}", id);
        VirementResponseDto updatedVirement = virementService.toggleVirementStatus(id);
        log.info("Statut du virement ID: {} basculé avec succès", id);
        return ResponseEntity.ok(updatedVirement);

    }


    @GetMapping("/decaissement/encours/{banqueId}")
    public ResponseEntity<EncoursVirementResponseDto> getEncoursDecaissementForVirements(@PathVariable Long banqueId, EncoursVirementsSearchDto searchDto,
                                                                                         @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des encours de décaissement pour la banque ID: {} , searchDto : {}", banqueId,searchDto);
        EncoursVirementResponseDto result = virementService.getEncoursDecaissementForVirements(searchDto, banqueId, pageable);
        log.info("Encours de décaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }


    @GetMapping("/encaissement/encours/{banqueId}")
    public ResponseEntity<EncoursVirementResponseDto> getEncoursEncaissementForVirements(
            @PathVariable Long banqueId,
            EncoursVirementsSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour la banque ID: {} , searchDto : {}", banqueId,searchDto);
        EncoursVirementResponseDto result = virementService.getEncoursEncaissementForVirements(searchDto, banqueId, pageable);
        log.info("Encours d'encaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }
}