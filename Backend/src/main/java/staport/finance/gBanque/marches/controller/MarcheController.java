package staport.finance.gBanque.marches.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.marches.dtos.request.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.request.MarcheSearchDTO;
import staport.finance.gBanque.marches.dtos.response.MarcheResponseDTO;
import staport.finance.gBanque.marches.exceptions.MarcheExceptionHandler;
import staport.finance.gBanque.marches.service.IMarcheService;

import java.time.LocalDate;

/**
 * Contrôleur REST du module Marches.
 *
 * <p>Injecte {@link IMarcheService} (DIP — jamais l'implémentation concrète). La gestion des
 * exceptions est déléguée à {@link MarcheExceptionHandler}. Aucune logique métier ici.
 *
 * <p>Résout la {@link Banque} via {@link IBanqueService} avant d'appeler le Service — voir la
 * javadoc de {@code MarcheService} pour la raison (évite un cycle de bean Spring).
 *
 * <p>Autorisations (cf. {@code docs/rbac/marches.md}) :</p>
 * <ul>
 *   <li>Lecture — tout utilisateur authentifié.</li>
 *   <li>Création / mise à jour / archivage / désarchivage — Finance ou SYSTEM_ADMIN.</li>
 *   <li>Suppression définitive — SYSTEM_ADMIN uniquement (et seulement si le marché n'a
 *       aucune donnée liée, cf. {@code MarcheService.deleteMarche}).</li>
 * </ul>
 */
@Tag(name = "Marchés", description = "Gestion des marchés (contrats BTP) rattachés à une banque")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/marches")
public class MarcheController {

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
    private static final String AUTHENTICATED = "isAuthenticated()";

    private final IMarcheService marcheService;
    private final IBanqueService banqueService;

    public MarcheController(IMarcheService marcheService, IBanqueService banqueService) {
        this.marcheService = marcheService;
        this.banqueService = banqueService;
    }

    @Operation(summary = "Créer un marché", description = "Réservé Finance / SYSTEM_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Marché créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Banque introuvable"),
            @ApiResponse(responseCode = "409", description = "Numéro de marché déjà existant")
    })
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> createMarche(@Valid @RequestBody MarcheRequestDTO requestDTO) {
        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());
        MarcheResponseDTO responseDTO = marcheService.createMarche(requestDTO, banque);
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    @Operation(summary = "Détail d'un marché par ID")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Marché introuvable")
    })
    @GetMapping("/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<MarcheResponseDTO> getMarcheById(@PathVariable Long id) {
        return ResponseEntity.ok(marcheService.getMarcheById(id));
    }

    @Operation(summary = "Liste paginée des marchés")
    @Parameter(name = "nom",            description = "Filtre LIKE sur le nom")
    @Parameter(name = "numero",         description = "Filtre LIKE sur le numéro")
    @Parameter(name = "maitreDouvrage", description = "Filtre LIKE sur le maître d'ouvrage")
    @Parameter(name = "banqueId",       description = "Filtre exact sur la banque")
    @Parameter(name = "actif",          description = "true = actifs uniquement, false = archivés uniquement, absent = tous")
    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Page<MarcheResponseDTO>> getAllMarches(
            @ModelAttribute MarcheSearchDTO searchDTO,
            Pageable pageable) {
        Page<MarcheResponseDTO> marches = marcheService.findAllMarches(searchDTO, pageable);
        return ResponseEntity.ok(marches);
    }

    @Operation(summary = "Exporter les marchés en PDF",
            description = "Ignore la pagination — exporte tous les résultats filtrés. Mêmes filtres que GET /marches.")
    @GetMapping("/export/pdf")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<byte[]> exportPdf(@ModelAttribute MarcheSearchDTO searchDTO) {
        byte[] pdf = marcheService.exportPdf(searchDTO);
        String filename = "marches_" + LocalDate.now() + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @Operation(summary = "Modifier un marché", description = "Réservé Finance / SYSTEM_ADMIN. La banque n'est modifiée que si banqueId est fourni.")
    @ApiResponses({
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "404", description = "Marché ou banque introuvable"),
            @ApiResponse(responseCode = "409", description = "Numéro de marché déjà existant")
    })
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> updateMarche(@PathVariable Long id,
                                                          @Valid @RequestBody MarcheRequestDTO requestDTO) {
        Banque banque = requestDTO.banqueId() != null ? banqueService.findOrThrow(requestDTO.banqueId()) : null;
        MarcheResponseDTO responseDTO = marcheService.updateMarche(id, requestDTO, banque);
        return ResponseEntity.ok(responseDTO);
    }

    @Operation(summary = "Supprimer définitivement un marché",
            description = "Réservé SYSTEM_ADMIN. Refusé si le marché a des données liées (décomptes, "
                    + "préfinancements, droits constatés, factorings, lignes spécifiques) — utiliser l'archivage.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Marché supprimé"),
            @ApiResponse(responseCode = "404", description = "Marché introuvable"),
            @ApiResponse(responseCode = "422", description = "Marché ayant des données liées")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteMarche(@PathVariable Long id) {
        marcheService.deleteMarche(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Archiver un marché",
            description = "Réservé Finance / SYSTEM_ADMIN. Le marché reste consultable mais sort "
                    + "définitivement des calculs futurs (agios, encours).")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Marché introuvable")
    })
    @PatchMapping("/{id}/archiver")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> archiverMarche(@PathVariable Long id) {
        return ResponseEntity.ok(marcheService.archiverMarche(id));
    }

    @Operation(summary = "Désarchiver un marché", description = "Réservé Finance / SYSTEM_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "404", description = "Marché introuvable")
    })
    @PatchMapping("/{id}/desarchiver")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> desarchiverMarche(@PathVariable Long id) {
        return ResponseEntity.ok(marcheService.desarchiverMarche(id));
    }
}
