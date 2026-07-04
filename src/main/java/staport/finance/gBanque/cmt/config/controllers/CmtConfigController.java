package staport.finance.gBanque.cmt.config.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigRequestDTO;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigResponseDTO;
import staport.finance.gBanque.cmt.config.services.interfaces.ICmtConfigService;

/**
 * Administration de la configuration CMT (seuils d'alerte des échéances à venir).
 * Accès réservé à {@code SYSTEM_ADMIN}, à l'image de {@code /leasing-config} et {@code /commission-config}.
 */
@Tag(name = "Configuration CMT", description = "Paramètres administrables du module CMT")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/cmt-config")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class CmtConfigController {

    private final ICmtConfigService service;

    public CmtConfigController(ICmtConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Récupérer la configuration CMT")
    @GetMapping
    public ResponseEntity<CmtConfigResponseDTO> getConfig() {
        return ResponseEntity.ok(service.getConfig());
    }

    @Operation(summary = "Modifier la configuration CMT")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @PutMapping
    public ResponseEntity<CmtConfigResponseDTO> update(@Valid @RequestBody CmtConfigRequestDTO dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
