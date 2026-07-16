package staport.finance.gBanque.chargesFixes.config.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigRequestDTO;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigSearchCriteria;
import staport.finance.gBanque.chargesFixes.config.dtos.response.ChargeFixeConfigResponseDTO;
import staport.finance.gBanque.chargesFixes.config.services.interfaces.IChargeFixeConfigService;

/**
 * Endpoints d'administration des charges fixes bancaires (frais/impôts-taxes/agios/prélèvements
 * générés automatiquement chaque mois). Accès réservé à {@code SYSTEM_ADMIN}, cohérent avec
 * {@code /commission-config} et {@code /leasing-config}.
 */
@Tag(name = "Configuration Charges fixes", description = "Administration des charges fixes bancaires récurrentes")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/charge-fixe-config")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class ChargeFixeConfigController {

    private static final Logger log = LoggerFactory.getLogger(ChargeFixeConfigController.class);

    private final IChargeFixeConfigService service;

    public ChargeFixeConfigController(IChargeFixeConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Lister les charges fixes (paginé, triable, filtrable)")
    @GetMapping
    public ResponseEntity<Page<ChargeFixeConfigResponseDTO>> findAll(
            @ModelAttribute ChargeFixeConfigSearchCriteria criteria,
            @PageableDefault(size = 20, sort = {"banque.nom", "libelle"}) Pageable pageable) {
        log.info("GET /charge-fixe-config — page={}, size={}, sort={}, criteria={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(), criteria);
        return ResponseEntity.ok(service.findAll(criteria, pageable));
    }

    @Operation(summary = "Créer une charge fixe")
    @PostMapping
    public ResponseEntity<ChargeFixeConfigResponseDTO> create(
            @Valid @RequestBody ChargeFixeConfigRequestDTO dto) {
        log.info("POST /charge-fixe-config — banqueId={}, libelle={}", dto.banqueId(), dto.libelle());
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier une charge fixe")
    @PutMapping("/{id}")
    public ResponseEntity<ChargeFixeConfigResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody ChargeFixeConfigRequestDTO dto) {
        log.info("PUT /charge-fixe-config/{}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "Supprimer une charge fixe")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /charge-fixe-config/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
