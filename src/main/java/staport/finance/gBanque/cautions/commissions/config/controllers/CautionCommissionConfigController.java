package staport.finance.gBanque.cautions.commissions.config.controllers;

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
import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigRequestDTO;
import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigSearchCriteria;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;
import staport.finance.gBanque.cautions.commissions.config.services.interfaces.ICautionCommissionConfigService;

/**
 * Endpoints d'administration des paramètres de commission caution (seuil minimum, diviseur
 * du prorata journalier, par banque). Accès réservé à {@code SYSTEM_ADMIN}, cohérent avec
 * {@code /commission-config} (module {@code commision}).
 */
@Tag(name = "Configuration commission caution", description = "Seuils minimums et diviseur du prorata journalier par banque")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/cautions/commission-config")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class CautionCommissionConfigController {

    private static final Logger log = LoggerFactory.getLogger(CautionCommissionConfigController.class);

    private final ICautionCommissionConfigService service;

    public CautionCommissionConfigController(ICautionCommissionConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Lister les configurations de commission caution (paginé, triable, filtrable)")
    @GetMapping
    public ResponseEntity<Page<CautionCommissionConfigResponseDTO>> findAll(
            @ModelAttribute CautionCommissionConfigSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "banque.nom") Pageable pageable) {
        log.info("GET /cautions/commission-config — page={}, size={}, sort={}, criteria={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(), criteria);
        return ResponseEntity.ok(service.findAll(criteria, pageable));
    }

    @Operation(summary = "Créer une configuration de commission caution")
    @PostMapping
    public ResponseEntity<CautionCommissionConfigResponseDTO> create(
            @Valid @RequestBody CautionCommissionConfigRequestDTO dto) {
        log.info("POST /cautions/commission-config — banqueId={}", dto.banqueId());
        return new ResponseEntity<>(service.create(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier une configuration de commission caution")
    @PutMapping("/{id}")
    public ResponseEntity<CautionCommissionConfigResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CautionCommissionConfigRequestDTO dto) {
        log.info("PUT /cautions/commission-config/{}", id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @Operation(summary = "Supprimer une configuration de commission caution")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /cautions/commission-config/{}", id);
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
