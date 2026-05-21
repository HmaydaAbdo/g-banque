package staport.finance.gBanque.moyensDePaiements.miseADisposition.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.*;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.services.MiseADispositionService;

@RestController
@RequestMapping("/misesADisposition")
public class MiseADispositionController {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionController.class);
    private final MiseADispositionService miseADispositionService;

    public MiseADispositionController(MiseADispositionService miseADispositionService) {
        this.miseADispositionService = miseADispositionService;
    }

    @PostMapping
    public ResponseEntity<MiseADispositionResponseDto> createMiseAdisposition(@RequestBody MiseAdispositionRequestDto requestDto) {
        log.info("Réception d'une demande de création de mise à disposition : {}", requestDto);
        MiseADispositionResponseDto response = miseADispositionService.createEncaissementMiseAdisposition(requestDto);
        log.info("Mise à disposition créée avec succès. ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MiseADispositionResponseDto> updateMiseAdisposition(@PathVariable Long id, @RequestBody MiseAdispositionRequestDto requestDto) {
        log.info("Réception d'une demande de modification de la mise à disposition ID: {} , dto : {}", id,requestDto);
        MiseADispositionResponseDto response = miseADispositionService.editEncaissementMiseAdisposition(id, requestDto);
        log.info("Mise à disposition ID: {} modifiée avec succès", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MiseADispositionResponseDto> getById(@PathVariable Long id) {
        log.debug("Récupération de la mise à disposition ID: {}", id);
        MiseADispositionResponseDto response = miseADispositionService.getMiseAdispositionById(id);
        log.debug("Mise à disposition ID: {} récupérée avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMiseAdisposition(@PathVariable Long id) {
        log.info("Réception d'une demande de suppression de la mise à disposition ID: {}", id);
        miseADispositionService.deleteMiseAdispositionById(id);
        log.info("Mise à disposition ID: {} supprimée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<MiseADispositionResponseDto>> getDecaissementMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des mises à disposition de décaissement - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<MiseADispositionResponseDto> result = miseADispositionService.getDecaissementForMiseAdisposition(searchDto, pageable);
        log.info("Nombre de mises à disposition de décaissement récupérées: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<MiseADispositionResponseDto>> getEncaissementMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des mises à disposition d'encaissement - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<MiseADispositionResponseDto> result = miseADispositionService.getEncaissementForMiseAdisposition(searchDto, pageable);
        log.info("Nombre de mises à disposition d'encaissement récupérées: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/toggle/{id}")
    public ResponseEntity<MiseADispositionResponseDto> toggleMiseADispositionStatus(@PathVariable Long id) {
        log.info("Demande de basculement du statut de la mise à disposition ID: {}", id);
        MiseADispositionResponseDto updatedMiseADisposition = miseADispositionService.toggleMiseADispositionStatus(id);
        log.info("Statut de la mise à disposition ID: {} basculé avec succès vers: {}", id, updatedMiseADisposition.getStatut());
        return ResponseEntity.ok(updatedMiseADisposition);

    }

    @GetMapping("/encours/decaissements/{banqueId}")
    public ResponseEntity<EncoursMiseAdisposition> getEncoursDecaissementsForMiseADispositionByBank(
            @PathVariable Long banqueId,
            EncoursMiseAdispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des encours de décaissement pour les mises à disposition de la banque ID: {}", banqueId);
        EncoursMiseAdisposition result = miseADispositionService.getEncoursDecaissementsForMiseADispositionByBank(searchDto, banqueId, pageable);
        log.info("Encours de décaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encours/encaissements/{banqueId}")
    public ResponseEntity<EncoursMiseAdisposition> getEncoursEncaissementsForMiseADispositionByBank(
            @PathVariable Long banqueId,
            EncoursMiseAdispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour les mises à disposition de la banque ID: {}", banqueId);
        EncoursMiseAdisposition result = miseADispositionService.getEncoursEncaissementsForMiseADispositionByBank(searchDto, banqueId, pageable);
        log.info("Encours d'encaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }
}