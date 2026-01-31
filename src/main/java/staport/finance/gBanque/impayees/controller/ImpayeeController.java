package staport.finance.gBanque.impayees.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.impayees.dtos.*;
import staport.finance.gBanque.impayees.service.ImpayeeService;

@RestController
@RequestMapping("/impayees")
public class ImpayeeController {

    private static final Logger log = LogManager.getLogger(ImpayeeController.class);
    private final ImpayeeService impayeeService;

    public ImpayeeController(ImpayeeService impayeeService) {
        this.impayeeService = impayeeService;
    }

    @PostMapping
    public ResponseEntity<ImpayeeResponseDto> createImpayee(@Validated @RequestBody ImpayeeRequestDto requestDto) {
        log.info("Requête REST: Création d'une impayée: {}", requestDto);
        return ResponseEntity.ok(impayeeService.createImpayee(requestDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImpayeeResponseDto> getById(@PathVariable Long id) {
        log.info("Requête REST: Récupération impayée id={}", id);
        return ResponseEntity.ok(impayeeService.getById(id));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<ImpayeeResponseDto>> searchImpayees(@RequestBody ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST: Recherche impayées - Critères: {}", searchDto);
        return ResponseEntity.ok(impayeeService.searchImpayees(searchDto, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ImpayeeResponseDto> updateImpayee(@PathVariable Long id, @Validated @RequestBody ImpayeeRequestDto requestDto) {
        log.info("Requête REST: Mise à jour impayée id={}", id);
        return ResponseEntity.ok(impayeeService.updateImpayee(id, requestDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteImpayee(@PathVariable Long id) {
        log.info("Requête REST: Suppression impayée id={}", id);
        impayeeService.deleteImpayee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueEncaissementId}")
    public ResponseEntity<EncoursImpayeeDto> getOutstandingImpayees(@PathVariable Long banqueEncaissementId, @ModelAttribute ImpayeeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST: Récupération encours pour banqueEncaissementId={}", banqueEncaissementId);
        searchDto.setBanqueEncaissementId(banqueEncaissementId);
        return ResponseEntity.ok(impayeeService.getOutstandingImpayees(searchDto, pageable));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ImpayeeResponseDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST: Basculement statut pour impayée ID: {}", id);
        ImpayeeResponseDto response = impayeeService.toggleStatus(id);
        log.info("Statut de l'ID {} mis à jour vers {}", id, response.getStatus());
        return ResponseEntity.ok(response);
    }
}