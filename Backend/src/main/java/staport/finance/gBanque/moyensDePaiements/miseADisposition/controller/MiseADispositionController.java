package staport.finance.gBanque.moyensDePaiements.miseADisposition.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.*;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.services.MiseADispositionService;

import java.time.LocalDate;

@Tag(name = "Mises à Disposition", description = "Gestion des mises à disposition — encaissements (saisis manuellement) et décaissements (synchronisation FunPaiement)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/misesADisposition")
@PreAuthorize(MiseADispositionController.FINANCE_OR_ADMIN)
public class MiseADispositionController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionController.class);
    private final MiseADispositionService miseADispositionService;

    public MiseADispositionController(MiseADispositionService miseADispositionService) {
        this.miseADispositionService = miseADispositionService;
    }

    @Operation(summary = "Créer une mise à disposition d'encaissement")
    @PostMapping
    public ResponseEntity<MiseADispositionResponseDto> createMiseAdisposition(@RequestBody MiseAdispositionRequestDto requestDto) {
        log.info("Réception d'une demande de création de mise à disposition : {}", requestDto);
        MiseADispositionResponseDto response = miseADispositionService.createEncaissementMiseAdisposition(requestDto);
        log.info("Mise à disposition créée avec succès. ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Modifier une mise à disposition d'encaissement")
    @PutMapping("/{id}")
    public ResponseEntity<MiseADispositionResponseDto> updateMiseAdisposition(@PathVariable Long id, @RequestBody MiseAdispositionRequestDto requestDto) {
        log.info("Réception d'une demande de modification de la mise à disposition ID: {} , dto : {}", id,requestDto);
        MiseADispositionResponseDto response = miseADispositionService.editEncaissementMiseAdisposition(id, requestDto);
        log.info("Mise à disposition ID: {} modifiée avec succès", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Consulter une mise à disposition par ID")
    @GetMapping("/{id}")
    public ResponseEntity<MiseADispositionResponseDto> getById(@PathVariable Long id) {
        log.debug("Récupération de la mise à disposition ID: {}", id);
        MiseADispositionResponseDto response = miseADispositionService.getMiseAdispositionById(id);
        log.debug("Mise à disposition ID: {} récupérée avec succès", id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Supprimer une mise à disposition")
    @PreAuthorize(SYSTEM_ADMIN)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMiseAdisposition(@PathVariable Long id) {
        log.info("Réception d'une demande de suppression de la mise à disposition ID: {}", id);
        miseADispositionService.deleteMiseAdispositionById(id);
        log.info("Mise à disposition ID: {} supprimée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les mises à disposition de décaissement")
    @GetMapping("/decaissements")
    public ResponseEntity<Page<MiseADispositionResponseDto>> getDecaissementMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des mises à disposition de décaissement - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<MiseADispositionResponseDto> result = miseADispositionService.getDecaissementForMiseAdisposition(searchDto, pageable);
        log.info("Nombre de mises à disposition de décaissement récupérées: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Lister les mises à disposition d'encaissement")
    @GetMapping("/encaissements")
    public ResponseEntity<Page<MiseADispositionResponseDto>> getEncaissementMisesADisposition(
            MiseADispositionSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        log.info("Récupération des mises à disposition d'encaissement - Page: {}, Taille: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<MiseADispositionResponseDto> result = miseADispositionService.getEncaissementForMiseAdisposition(searchDto, pageable);
        log.info("Nombre de mises à disposition d'encaissement récupérées: {} sur un total de {}", result.getNumberOfElements(), result.getTotalElements());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Basculer le statut d'une mise à disposition (ENCOURS ↔ REGLE)")
    @PreAuthorize(SYSTEM_ADMIN)
    @PatchMapping("/toggle/{id}")
    public ResponseEntity<MiseADispositionResponseDto> toggleMiseADispositionStatus(@PathVariable Long id) {
        log.info("Demande de basculement du statut de la mise à disposition ID: {}", id);
        MiseADispositionResponseDto updatedMiseADisposition = miseADispositionService.toggleMiseADispositionStatus(id);
        log.info("Statut de la mise à disposition ID: {} basculé avec succès vers: {}", id, updatedMiseADisposition.getStatut());
        return ResponseEntity.ok(updatedMiseADisposition);

    }

    @Operation(summary = "Exporter les mises à disposition de décaissement en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés.")
    @GetMapping("/decaissements/export/pdf")
    public ResponseEntity<byte[]> exportDecaissementsPdf(MiseADispositionSearchDto searchDto) {
        return pdfResponse(miseADispositionService.exportDecaissementsPdf(searchDto), "mises_a_disposition_decaissements");
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filenamePrefix) {
        String filename = filenamePrefix + "_" + LocalDate.now() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}