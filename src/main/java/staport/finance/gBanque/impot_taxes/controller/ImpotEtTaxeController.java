package staport.finance.gBanque.impot_taxes.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.impot_taxes.dtos.*;
import staport.finance.gBanque.impot_taxes.service.ImpotEtTaxeService;

@RestController
@RequestMapping("/impots-taxes")
public class ImpotEtTaxeController {

    private static final Logger log = LogManager.getLogger(ImpotEtTaxeController.class);
    private final ImpotEtTaxeService impotEtTaxeService;

    public ImpotEtTaxeController(ImpotEtTaxeService impotEtTaxeService) {
        this.impotEtTaxeService = impotEtTaxeService;
    }

    @PostMapping
    public ResponseEntity<ImpotEtTaxeResponseDto> createImpotEtTaxe(@Validated @RequestBody ImpotEtTaxeRequestDto requestDto) {
        log.info("Requête REST: Création d'un impôt/taxe: {}", requestDto);
        return ResponseEntity.ok(impotEtTaxeService.createImpotEtTaxe(requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImpotEtTaxeResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST: Récupération impôt/taxe id={}", id);
        return ResponseEntity.ok(impotEtTaxeService.getById(id));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ImpotEtTaxeResponseDto>> searchImpotEtTaxes(@RequestBody ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST: Recherche impôts/taxes - Critères: {}", searchDto);
        return ResponseEntity.ok(impotEtTaxeService.searchImpotEtTaxes(searchDto, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImpotEtTaxeResponseDto> updateImpotEtTaxe(@PathVariable Long id, @Validated @RequestBody ImpotEtTaxeRequestDto requestDto) {
        log.info("Requête REST: Mise à jour impôt/taxe id={}", id);
        return ResponseEntity.ok(impotEtTaxeService.updateImpotEtTaxe(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImpotEtTaxe(@PathVariable Long id) {
        log.info("Requête REST: Suppression impôt/taxe id={}", id);
        impotEtTaxeService.deleteImpotEtTaxe(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursImpotEtTaxeDto> getOutstandingImpotEtTaxes(@PathVariable Long banqueId, @ModelAttribute ImpotEtTaxeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST: Récupération encours pour banqueId={}", banqueId);
        searchDto.setBanqueId(banqueId);
        return ResponseEntity.ok(impotEtTaxeService.getOutstandingImpotEtTaxes(searchDto, pageable));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ImpotEtTaxeResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST: Basculement statut pour impôt/taxe ID: {}", id);
        ImpotEtTaxeResponseDto response = impotEtTaxeService.toggleStatus(id);
        log.info("Statut de l'ID {} mis à jour vers {}", id, response.getStatus());
        return ResponseEntity.ok(response);
    }
}