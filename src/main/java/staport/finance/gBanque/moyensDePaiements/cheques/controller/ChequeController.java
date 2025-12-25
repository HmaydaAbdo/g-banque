package staport.finance.gBanque.moyensDePaiements.cheques.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.*;
import staport.finance.gBanque.moyensDePaiements.cheques.services.ChequeService;

@RestController
@RequestMapping("/cheques")
public class ChequeController {

    private static final Logger log = LoggerFactory.getLogger(ChequeController.class);
    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    @PostMapping
    public ResponseEntity<ChequeResponseDto> createCheque(@RequestBody ChequeRequestDto requestDto) {
        log.info("Réception d'une demande de création de chèque , dto : {}", requestDto);
        ChequeResponseDto response = chequeService.createEncaissementCheque(requestDto);
        log.info("Chèque créé avec succès. ID: {}, Numéro: {}", response.getId(), response.getNumero());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ChequeResponseDto> updateCheque(@PathVariable Long id, @RequestBody ChequeRequestDto requestDto) {
        log.info("Réception d'une demande de modification du chèque ID: {}", id);
        ChequeResponseDto response = chequeService.editEncaissementCheque(id, requestDto);
        log.info("Chèque ID: {} modifié avec succès", id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChequeResponseDto> getById(@PathVariable Long id) {
        log.debug("Récupération du chèque  ID: {}", id);
        ChequeResponseDto response = chequeService.getChequeById(id);
        log.debug("Chèque ID: {} récupéré avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCheque(@PathVariable Long id) {
        log.info("Réception d'une demande de suppression du chèque ID: {}", id);
        chequeService.deleteCheque(id);
        log.info("Chèque ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<ChequeResponseDto>> getAllDecaissement(ChequeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des chèques de décaissement - Page: {}, Taille: {} , searchDto : {}",
                pageable.getPageNumber(), pageable.getPageSize(),searchDto);

        Page<ChequeResponseDto> result = chequeService.getAllDecaissement(searchDto, pageable);

        log.info("Nombre de chèques de décaissement récupérés: {} sur un total de {}",
                result.getNumberOfElements(), result.getTotalElements());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<ChequeResponseDto>> getAllEncaissement(ChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des chèques d'encaissement - Page: {}, Taille: {},searchDto : {}",
                pageable.getPageNumber(), pageable.getPageSize(),searchDto);

        Page<ChequeResponseDto> result = chequeService.getAllEncaissement(searchDto, pageable);

        log.info("Nombre de chèques d'encaissement récupérés: {} sur un total de {}",
                result.getNumberOfElements(), result.getTotalElements());

        return ResponseEntity.ok(result);
    }

    @PatchMapping("/toggle/{id}")
    public ResponseEntity<ChequeResponseDto> toggleChequeStatus(@PathVariable long id) {
        log.info("Demande de basculement du statut du chèque ID: {}", id);
        ChequeResponseDto updatedCheque = chequeService.toggleChequeStatus(id);
        log.info("Statut du chèque ID: {} basculé avec succès vers: {}", id, updatedCheque.isActive());
        return ResponseEntity.ok(updatedCheque);

    }

    @GetMapping("/decaissements/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursDecaissementForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des encours de décaissement pour les chèques de la banque ID: {} les filtres suivants : {}",
                banqueId,searchDto);

        EncoursChequeResponseDto result = chequeService.getEncoursDecaissementForCheques(searchDto, banqueId, pageable);

        log.info("Encours de décaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursEncaissementForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des encours d'encaissement pour les chèques de la banque ID: {} , les filtres : {}", banqueId,searchDto);

        EncoursChequeResponseDto result = chequeService.getEncoursEncaissementForCheques(searchDto, banqueId, pageable);

        log.info("Encours d'encaissement récupérés pour la banque ID: {} - Montant total: {}, Nombre: {}",
                banqueId, result.getTotalDesEncours(), result.getPage().getTotalElements());

        return ResponseEntity.ok(result);
    }
}