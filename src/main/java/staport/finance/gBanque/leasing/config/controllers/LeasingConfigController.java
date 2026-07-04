package staport.finance.gBanque.leasing.config.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigRequestDTO;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigResponseDTO;
import staport.finance.gBanque.leasing.config.services.interfaces.ILeasingConfigService;

/**
 * Administration de la configuration leasing (valeur résiduelle, taux de TVA).
 * Accès réservé à {@code SYSTEM_ADMIN}, à l'image de {@code /commission-config}.
 */
@Tag(name = "Configuration leasing", description = "Paramètres administrables du module leasing")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/leasing-config")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class LeasingConfigController {

    private final ILeasingConfigService service;

    public LeasingConfigController(ILeasingConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Récupérer la configuration leasing")
    @GetMapping
    public ResponseEntity<LeasingConfigResponseDTO> getConfig() {
        return ResponseEntity.ok(service.getConfig());
    }

    @Operation(summary = "Modifier la configuration leasing")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @PutMapping
    public ResponseEntity<LeasingConfigResponseDTO> update(@Valid @RequestBody LeasingConfigRequestDTO dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
