package staport.finance.gBanque.LigneFonctionnelle.config.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigRequestDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigResponseDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.services.interfaces.ILigneFonctionnelleConfigService;

/**
 * Administration de la configuration LigneFonctionnelle (seuils d'alerte/avertissement
 * d'expiration, utilisés par le batch général et le batch spécifique).
 * Accès réservé à {@code SYSTEM_ADMIN}, à l'image de {@code /cmt-config} et {@code /leasing-config}.
 */
@Tag(name = "Configuration LigneFonctionnelle", description = "Paramètres administrables du module LigneFonctionnelle")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lignes-fonctionnelles-config")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class LigneFonctionnelleConfigController {

    private final ILigneFonctionnelleConfigService service;

    public LigneFonctionnelleConfigController(ILigneFonctionnelleConfigService service) {
        this.service = service;
    }

    @Operation(summary = "Récupérer la configuration LigneFonctionnelle")
    @GetMapping
    public ResponseEntity<LigneFonctionnelleConfigResponseDTO> getConfig() {
        return ResponseEntity.ok(service.getConfig());
    }

    @Operation(summary = "Modifier la configuration LigneFonctionnelle")
    @ApiResponse(responseCode = "400", description = "Données invalides")
    @PutMapping
    public ResponseEntity<LigneFonctionnelleConfigResponseDTO> update(@Valid @RequestBody LigneFonctionnelleConfigRequestDTO dto) {
        return ResponseEntity.ok(service.update(dto));
    }
}
