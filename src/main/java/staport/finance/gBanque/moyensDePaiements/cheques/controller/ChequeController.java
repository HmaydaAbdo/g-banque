package staport.finance.gBanque.moyensDePaiements.cheques.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.*;
import staport.finance.gBanque.moyensDePaiements.cheques.services.ChequeService;

/**
 * Contrôleur REST du module Chèque.
 *
 * <p>Thin controller — aucune logique métier, délègue à {@link ChequeService}.
 * La gestion des exceptions est déléguée à {@code ChequeExceptionHandler}.
 */
@Tag(name = "Chèques", description = "Gestion des chèques d'encaissement et de décaissement")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/cheques")
@PreAuthorize(ChequeController.FINANCE_OR_ADMIN)
public class ChequeController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";

    private final ChequeService chequeService;

    public ChequeController(ChequeService chequeService) {
        this.chequeService = chequeService;
    }

    @Operation(summary = "Créer un chèque d'encaissement")
    @PostMapping
    public ResponseEntity<ChequeResponseDto> createCheque(@RequestBody ChequeRequestDto requestDto) {
        return ResponseEntity.ok(chequeService.createEncaissementCheque(requestDto));
    }

    @Operation(summary = "Modifier un chèque d'encaissement")
    @PutMapping("/{id}")
    public ResponseEntity<ChequeResponseDto> updateCheque(@PathVariable Long id, @RequestBody ChequeRequestDto requestDto) {
        return ResponseEntity.ok(chequeService.editEncaissementCheque(id, requestDto));
    }

    @Operation(summary = "Récupérer un chèque par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ChequeResponseDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(chequeService.getChequeById(id));
    }

    @Operation(summary = "Supprimer un chèque", description = "Réservé SYSTEM_ADMIN. Bloqué si le chèque est déjà réglé.")
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteCheque(@PathVariable Long id) {
        chequeService.deleteCheque(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Lister les chèques de décaissement")
    @GetMapping("/decaissements")
    public ResponseEntity<Page<ChequeResponseDto>> getAllDecaissement(ChequeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getAllDecaissement(searchDto, pageable));
    }

    @Operation(summary = "Lister les chèques d'encaissement")
    @GetMapping("/encaissements")
    public ResponseEntity<Page<ChequeResponseDto>> getAllEncaissement(ChequeSearchDto searchDto, @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getAllEncaissement(searchDto, pageable));
    }

    @Operation(summary = "Basculer le statut d'un chèque", description = "ENCOURS ↔ REGLE. Impossible sur un chèque ANNULE.")
    @PreAuthorize(SYSTEM_ADMIN)
    @PatchMapping("/toggle/{id}")
    public ResponseEntity<ChequeResponseDto> toggleChequeStatus(@PathVariable long id) {
        return ResponseEntity.ok(chequeService.toggleChequeStatus(id));
    }

    @Operation(summary = "Encours de décaissement par banque")
    @GetMapping("/decaissements/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursDecaissementForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getEncoursDecaissementForCheques(searchDto, banqueId, pageable));
    }

    @Operation(summary = "Encours d'encaissement par banque")
    @GetMapping("/encaissements/encours/{banqueId}")
    public ResponseEntity<EncoursChequeResponseDto> getEncoursEncaissementForCheques(
            @PathVariable Long banqueId,
            EncoursChequeSearchDto searchDto,
            @PageableDefault(size = 10, sort = "dateEmission") Pageable pageable) {
        return ResponseEntity.ok(chequeService.getEncoursEncaissementForCheques(searchDto, banqueId, pageable));
    }
}
