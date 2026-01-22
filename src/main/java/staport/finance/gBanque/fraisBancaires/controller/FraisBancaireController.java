package staport.finance.gBanque.fraisBancaires.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.fraisBancaires.dtos.*;
import staport.finance.gBanque.fraisBancaires.service.FraisBancaireService;

@RestController
@RequestMapping("/frais-bancaire")
public class FraisBancaireController {

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireController.class);
    private final FraisBancaireService fraisBancaireService;

    public FraisBancaireController(FraisBancaireService fraisBancaireService) {
        this.fraisBancaireService = fraisBancaireService;
        log.info("Initialisation du contrôleur FraisBancaire");
    }

    @PostMapping
    public ResponseEntity<FraisBancaireResposneDto> createFraisBancaire(
            @Validated @RequestBody FraisBancaireRequestDto requestDto) {
        log.info("Requête REST pour créer un frais bancaire - Libellé: '{}', Montant: {}",
                requestDto.getLibelle(), requestDto.getMontant());

        FraisBancaireResposneDto response = fraisBancaireService.createFraisBancaire(requestDto);
        log.info("Frais bancaire créé avec succès - ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraisBancaireResposneDto> getById(@PathVariable Long id) {
        log.info("Requête REST pour récupérer le frais bancaire ID: {}", id);
        FraisBancaireResposneDto response = fraisBancaireService.getById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/search")
    public ResponseEntity<Page<FraisBancaireResposneDto>> searchFraisBancaire(
            @RequestBody FraisBancaireSearchDto searchDto, Pageable pageable) {
        log.info("Requête REST pour rechercher des frais bancaires - Critères: {}", searchDto);
        Page<FraisBancaireResposneDto> page = fraisBancaireService.searchFraisBancaire(searchDto, pageable);
        log.info("Recherche terminée - {} résultat(s)", page.getTotalElements());
        return ResponseEntity.ok(page);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FraisBancaireResposneDto> updateFraisBancaire(
            @PathVariable Long id, @Validated @RequestBody FraisBancaireRequestDto requestDto) {
        log.info("Requête REST pour mettre à jour le frais bancaire ID: {}", id);
        FraisBancaireResposneDto response = fraisBancaireService.updateFraisBancaire(id, requestDto);
        log.info("Frais bancaire ID: {} mis à jour avec succès", id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFraisBancaire(@PathVariable Long id) {
        log.info("Requête REST pour supprimer le frais bancaire ID: {}", id);
        fraisBancaireService.deleteFraisBancaire(id);
        log.info("Frais bancaire ID: {} supprimé avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/encours/{banqueId}")
    public ResponseEntity<EncoursFraisBancaireDto> getOutstandingFraisBancaire(
            @PathVariable Long banqueId,
            @ModelAttribute FraisBancaireSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Requête REST pour récupérer les frais bancaires en cours - Banque ID: {}", banqueId);
        searchDto.setBanqueId(banqueId);
        EncoursFraisBancaireDto result = fraisBancaireService.getOutstandingFraisBancaire(searchDto, pageable);
        log.info("Frais bancaires en cours récupérés - Total: {}", result.getTotalDesEncours());
        return ResponseEntity.ok(result);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<FraisBancaireResposneDto> toggleStatus(@PathVariable Long id) {
        log.info("Requête REST pour basculer le statut du frais bancaire ID: {}", id);
        FraisBancaireResposneDto response = fraisBancaireService.toggleStatus(id);
        log.info("Statut du frais bancaire ID: {} basculé vers {}", id, response.getStatus());
        return ResponseEntity.ok(response);
    }
}