package staport.finance.gBanque.moyensDePaiements.lcn.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.*;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnService;

@RestController
@RequestMapping("/lcns")
public class LcnController {

    private static final Logger log = LoggerFactory.getLogger(LcnController.class);
    private final LcnService lcnService;

    public LcnController(LcnService lcnService) {
        this.lcnService = lcnService;
    }

    @PostMapping()
    public ResponseEntity<LcnResponseDTO> createLcn(@RequestBody LcnRequestDto requestDto) {
        log.info("Réception d'une demande de création de LCN avec les données suivantes : {}", requestDto);
        LcnResponseDTO createdLcn = lcnService.createLcn(requestDto);
        log.info("LCN créé avec succès. ID: {}, Numéro: {}", createdLcn.getId(), createdLcn.getNumero());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLcn);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LcnResponseDTO> updateLcn(@PathVariable Long id, @RequestBody LcnRequestDto requestDto) {
        log.info("Réception d'une demande de modification du LCN ID: {}", id);
        LcnResponseDTO response = lcnService.editEncaissementLcn(id, requestDto);
        log.info("LCN ID: {} modifié avec succès", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LcnResponseDTO> getLcnById(@PathVariable Long id) {
        log.debug("Récupération du LCN ID: {}", id);
        LcnResponseDTO response = lcnService.getLcnById(id);
        log.debug("LCN ID: {} récupéré avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLcn(@PathVariable Long id) {
        log.info("Réception d'une demande de suppression du LCN ID: {}", id);
        lcnService.deleteLcn(id);
        log.info("LCN ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllDecaissements(LcnSearchDTO searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des LCN de décaissement - Page: {}, Taille: {}, searchDto:{}",
                pageable.getPageNumber(), pageable.getPageSize(),searchDto);

        Page<LcnResponseDTO> result = lcnService.getDecaissements(searchDto, pageable);
        log.info("Nombre de LCN de décaissement récupérés: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<LcnResponseDTO>> getAllEncaissements(LcnSearchDTO searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des LCN d'encaissement - Page: {}, Taille: {},searchDto:{}",
                pageable.getPageNumber(), pageable.getPageSize(),searchDto);

        Page<LcnResponseDTO> result = lcnService.getEncaissements(searchDto, pageable);

        log.info("Nombre de LCN d'encaissement récupérés: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/toggle/{id}")
    public ResponseEntity<LcnResponseDTO> toggleLcnStatus(@PathVariable Long id) {

        log.info("Demande de basculement du statut du LCN ID: {}", id);
        LcnResponseDTO updatedLcn = lcnService.toggleLcnStatus(id);
        log.info("Statut du LCN ID: {} basculé avec succès vers: {}", id, updatedLcn.isActive());
        return ResponseEntity.ok(updatedLcn);
    }

    @GetMapping("/decaissements/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursDecaissementsForLcn(@PathVariable Long banqueId, EncoursLcnSearchDto searchDto,
                                                                            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {

        log.info("Récupération des encours de décaissement pour les LCN de la banque ID: {} , et searchDto :", banqueId);
        EncoursLcnResponse result = lcnService.getEncoursDecaissementsForLcn(searchDto, banqueId, pageable);
        log.info("Encours de décaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements/encours/{banqueId}")
    public ResponseEntity<EncoursLcnResponse> getEncoursEncaissementsForLcn(
            @PathVariable Long banqueId,
            EncoursLcnSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEcheance") Pageable pageable) {
        log.info("Récupération des encours d'encaissement pour les LCN de la banque ID: {}", banqueId);
        EncoursLcnResponse result = lcnService.getEncoursEncaissementsForLcn(searchDto, banqueId, pageable);
        log.info("Encours d'encaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalEncours(), result.getPage().getTotalElements());
        return ResponseEntity.ok(result);
    }
}