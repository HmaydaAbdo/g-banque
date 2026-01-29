package staport.finance.gBanque.moyensDePaiements.virementInternational.controller;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.service.VirementInternationalService;

@RestController
@RequestMapping("/virements-internationaux")
public class VirementInternationalController {

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalController.class);

    private final VirementInternationalService virementInternationalService;

    public VirementInternationalController(VirementInternationalService virementInternationalService) {
        this.virementInternationalService = virementInternationalService;
    }

    /* ===================== CREATE ===================== */

    @PostMapping
    public ResponseEntity<VirementInternationalResponseDto> createVirementInternational(
            @Valid @RequestBody VirementInternationalRequestDto dto) {

        log.info("Demande de création d'un virement international : {}", dto);

        VirementInternationalResponseDto response =
                virementInternationalService.createVirementInternational(dto);

        log.info("Virement international créé avec succès. ID={}", response.getId());
        return ResponseEntity.ok(response);
    }

    /* ===================== UPDATE ===================== */

    @PutMapping("/{id}")
    public ResponseEntity<VirementInternationalResponseDto> updateVirementInternational(
            @PathVariable Long id,
            @Valid @RequestBody VirementInternationalRequestDto dto) {

        log.info("Demande de modification du virement international ID={} avec dto={}", id, dto);
        VirementInternationalResponseDto response =
                virementInternationalService.editVirementInternational(id, dto);
        log.info("Virement international ID={} modifié avec succès", id);
        return ResponseEntity.ok(response);
    }

    /* ===================== GET BY ID ===================== */

    @GetMapping("/{id}")
    public ResponseEntity<VirementInternationalResponseDto> getVirementInternationalById(
            @PathVariable Long id) {

        log.debug("Récupération du virement international ID={}", id);
        VirementInternationalResponseDto response =
                virementInternationalService.getVirementInternationalById(id);
        return ResponseEntity.ok(response);
    }

    /* ===================== LIST / SEARCH ===================== */

    @GetMapping
    public ResponseEntity<Page<VirementInternationalResponseDto>> getVirementsInternationaux(
            VirementInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Recherche des virements internationaux - searchDto={}, page={}", searchDto, pageable);
        Page<VirementInternationalResponseDto> result =
                virementInternationalService.getVirementsInternationaux(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissements")
    public ResponseEntity<Page<VirementInternationalResponseDto>> getEncaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Recherche des virements internationaux d'encaissement - searchDto={}", searchDto);
        Page<VirementInternationalResponseDto> result =
                virementInternationalService.getEncaissementVirementsInternationaux(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/decaissements")
    public ResponseEntity<Page<VirementInternationalResponseDto>> getDecaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Recherche des virements internationaux de décaissement - searchDto={}", searchDto);
        Page<VirementInternationalResponseDto> result =
                virementInternationalService.getDecaissementVirementsInternationaux(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    /* ===================== TOGGLE STATUS ===================== */

    @PatchMapping("/toggle/{id}")
    public ResponseEntity<VirementInternationalResponseDto> toggleVirementInternationalStatus(
            @PathVariable Long id) {

        log.info("Demande de bascule du statut du virement international ID={}", id);
        VirementInternationalResponseDto response =
                virementInternationalService.toggleVirementStatus(id);
        return ResponseEntity.ok(response);
    }

    /* ===================== ENCOURS ===================== */

    @GetMapping("/decaissement/encours/{banqueId}")
    public ResponseEntity<EncoursVirementInternationalResponseDto> getEncoursDecaissement(
            @PathVariable Long banqueId,
            EncoursVirementsInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des encours de décaissement internationaux - banqueId={}, searchDto={}",
                banqueId, searchDto);

        EncoursVirementInternationalResponseDto result =
                virementInternationalService.getEncoursDecaissement(searchDto, banqueId, pageable);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/encaissement/encours/{banqueId}")
    public ResponseEntity<EncoursVirementInternationalResponseDto> getEncoursEncaissement(
            @PathVariable Long banqueId,
            EncoursVirementsInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Récupération des encours d'encaissement internationaux - banqueId={}, searchDto={}",
                banqueId, searchDto);

        EncoursVirementInternationalResponseDto result =
                virementInternationalService.getEncoursEncaissement(searchDto, banqueId, pageable);

        return ResponseEntity.ok(result);
    }

    /* ===================== DELETE ===================== */

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVirementInternational(@PathVariable Long id) {

        log.info("Demande de suppression du virement international ID={}", id);
        virementInternationalService.deleteVirementInternational(id);
        log.info("Virement international ID={} supprimé avec succès", id);

        return ResponseEntity.noContent().build();
    }
}
