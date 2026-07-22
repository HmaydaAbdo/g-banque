package staport.finance.gBanque.moyensDePaiements.virementInternational.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.service.VirementInternationalService;

import java.time.LocalDate;

@Tag(name = "Virements Internationaux", description = "Gestion des virements internationaux — encaissements et décaissements, saisis manuellement (aucune synchronisation FunPaiement pour ce module)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/virements-internationaux")
@PreAuthorize(VirementInternationalController.FINANCE_OR_ADMIN)
public class VirementInternationalController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalController.class);

    private final VirementInternationalService virementInternationalService;

    public VirementInternationalController(VirementInternationalService virementInternationalService) {
        this.virementInternationalService = virementInternationalService;
    }

    /* ===================== CREATE ===================== */

    @Operation(summary = "Créer un virement international (encaissement ou décaissement)")
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

    @Operation(summary = "Modifier un virement international")
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

    @Operation(summary = "Consulter un virement international par ID")
    @GetMapping("/{id}")
    public ResponseEntity<VirementInternationalResponseDto> getVirementInternationalById(
            @PathVariable Long id) {

        log.debug("Récupération du virement international ID={}", id);
        VirementInternationalResponseDto response =
                virementInternationalService.getVirementInternationalById(id);
        return ResponseEntity.ok(response);
    }

    /* ===================== LIST / SEARCH ===================== */

    @Operation(summary = "Lister/rechercher les virements internationaux")
    @GetMapping
    public ResponseEntity<Page<VirementInternationalResponseDto>> getVirementsInternationaux(
            VirementInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Recherche des virements internationaux - searchDto={}, page={}", searchDto, pageable);
        Page<VirementInternationalResponseDto> result =
                virementInternationalService.getVirementsInternationaux(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lister les virements internationaux d'encaissement")
    @GetMapping("/encaissements")
    public ResponseEntity<Page<VirementInternationalResponseDto>> getEncaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {

        log.info("Recherche des virements internationaux d'encaissement - searchDto={}", searchDto);
        Page<VirementInternationalResponseDto> result =
                virementInternationalService.getEncaissementVirementsInternationaux(searchDto, pageable);
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lister les virements internationaux de décaissement")
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

    @Operation(summary = "Basculer le statut d'un virement international (ENCOURS ↔ REGLE)")
    @PreAuthorize(SYSTEM_ADMIN)
    @PatchMapping("/toggle/{id}")
    public ResponseEntity<VirementInternationalResponseDto> toggleVirementInternationalStatus(
            @PathVariable Long id) {

        log.info("Demande de bascule du statut du virement international ID={}", id);
        VirementInternationalResponseDto response =
                virementInternationalService.toggleVirementStatus(id);
        return ResponseEntity.ok(response);
    }

    /* ===================== ENCOURS ===================== */

    @Operation(summary = "Encours de décaissement virement international pour une banque")
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

    @Operation(summary = "Encours d'encaissement virement international pour une banque")
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

    @Operation(summary = "Supprimer un virement international")
    @PreAuthorize(SYSTEM_ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVirementInternational(@PathVariable Long id) {

        log.info("Demande de suppression du virement international ID={}", id);
        virementInternationalService.deleteVirementInternational(id);
        log.info("Virement international ID={} supprimé avec succès", id);

        return ResponseEntity.noContent().build();
    }

    /* ===================== EXPORT PDF ===================== */

    @Operation(summary = "Exporter les virements internationaux en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(VirementInternationalSearchDto searchDto) {
        return pdfResponse(virementInternationalService.exportPdf(searchDto), "virements_internationaux");
    }

    @Operation(summary = "Exporter en PDF l'encours d'encaissement de virements internationaux d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/encaissement/encours/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportEncoursEncaissementPdf(@PathVariable Long banqueId, EncoursVirementsInternationalSearchDto searchDto) {
        return pdfResponse(virementInternationalService.exportEncoursEncaissementPdf(searchDto, banqueId), "virements_internationaux_encours_encaissement");
    }

    @Operation(summary = "Exporter en PDF l'encours de décaissement de virements internationaux d'une banque",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/decaissement/encours/{banqueId}/export/pdf")
    public ResponseEntity<byte[]> exportEncoursDecaissementPdf(@PathVariable Long banqueId, EncoursVirementsInternationalSearchDto searchDto) {
        return pdfResponse(virementInternationalService.exportEncoursDecaissementPdf(searchDto, banqueId), "virements_internationaux_encours_decaissement");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
