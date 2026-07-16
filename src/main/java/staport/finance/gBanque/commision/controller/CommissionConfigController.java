package staport.finance.gBanque.commision.controller;

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
import staport.finance.gBanque.commision.dtos.CommissionConfigRequestDTO;
import staport.finance.gBanque.commision.dtos.CommissionConfigResponseDTO;
import staport.finance.gBanque.commision.dtos.CommissionConfigSearchCriteria;
import staport.finance.gBanque.commision.service.ICommissionConfigService;

/**
 * Endpoints d'administration des règles de commission.
 * Accès réservé à {@code SYSTEM_ADMIN}.
 */
@Tag(name = "Configuration Commissions", description = "Administration des règles de résolution des montants de commission")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/commission-config")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class CommissionConfigController {

    private static final Logger log = LoggerFactory.getLogger(CommissionConfigController.class);

    private final ICommissionConfigService commissionConfigService;

    public CommissionConfigController(ICommissionConfigService commissionConfigService) {
        this.commissionConfigService = commissionConfigService;
    }

    @Operation(summary = "Lister les règles de commission (paginé, triable, filtrable)")
    @GetMapping
    public ResponseEntity<Page<CommissionConfigResponseDTO>> findAll(
            @ModelAttribute CommissionConfigSearchCriteria criteria,
            @PageableDefault(size = 20, sort = "banque.nom") Pageable pageable) {
        log.info("GET /commission-config — page={}, size={}, sort={}, criteria={}",
                pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort(), criteria);
        return ResponseEntity.ok(commissionConfigService.findAll(criteria, pageable));
    }

    @Operation(summary = "Créer une règle de commission")
    @PostMapping
    public ResponseEntity<CommissionConfigResponseDTO> create(
            @Valid @RequestBody CommissionConfigRequestDTO dto) {
        log.info("POST /commission-config — banqueId={}, type={}", dto.banqueId(), dto.type());
        return new ResponseEntity<>(commissionConfigService.create(dto), HttpStatus.CREATED);
    }

    @Operation(summary = "Modifier une règle de commission")
    @PutMapping("/{id}")
    public ResponseEntity<CommissionConfigResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CommissionConfigRequestDTO dto) {
        log.info("PUT /commission-config/{}", id);
        return ResponseEntity.ok(commissionConfigService.update(id, dto));
    }

    @Operation(summary = "Supprimer une règle de commission")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("DELETE /commission-config/{}", id);
        commissionConfigService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
