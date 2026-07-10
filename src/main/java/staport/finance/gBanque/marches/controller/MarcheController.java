package staport.finance.gBanque.marches.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import staport.finance.gBanque.marches.dtos.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.MarcheResponseDTO;
import staport.finance.gBanque.marches.dtos.MarcheSearchDTO;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.marches.service.MarcheService;

/**
 * Contrôleur du module Marches.
 *
 * <p>Autorisations (cf. {@code docs/rbac/marches.md}) :</p>
 * <ul>
 *   <li>Lecture — tout utilisateur authentifié.</li>
 *   <li>Création / mise à jour / archivage / désarchivage — Finance ou SYSTEM_ADMIN.</li>
 *   <li>Suppression définitive — SYSTEM_ADMIN uniquement (et seulement si le marché n'a
 *       aucune donnée liée, cf. {@code MarcheService.deleteMarche}).</li>
 * </ul>
 */
@Controller
@RequestMapping("/marches")
public class MarcheController {

    private static final Logger log = LoggerFactory.getLogger(MarcheController.class);

    private static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";
    private static final String SYSTEM_ADMIN = "hasAuthority('SYSTEM_ADMIN')";
    private static final String AUTHENTICATED = "isAuthenticated()";

    private final MarcheService marcheService;

    public MarcheController(MarcheService marcheService) {
        this.marcheService = marcheService;
    }

    /**
     * Endpoint to create a new Marche.
     * HTTP Method: POST
     * URI: /api/marches
     *
     * @param requestDTO The DTO containing the Marche data.
     * @return A ResponseEntity with the created Marche and HTTP status 201.
     */
    @PostMapping
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> createMarche(@Valid @RequestBody MarcheRequestDTO requestDTO) {
        log.info("Début de la création d'un nouveau marché avec ls données suivants : {}", requestDTO);
        MarcheResponseDTO responseDTO = marcheService.createMarche(requestDTO);
        log.info("Marché créé avec succès - ID: {}, Numéro: {}", responseDTO.getId(), responseDTO.getNumero());
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    /**
     * Endpoint to get a single Marche by its ID.
     * HTTP Method: GET
     * URI: /api/marches/{id}
     *
     * @param id The ID of the Marche.
     * @return A ResponseEntity with the Marche response DTO and HTTP status 200.
     */
    @GetMapping("/{id}")
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<MarcheResponseDTO> getMarcheById(@PathVariable Long id) {
        log.info("Récupération du marché par ID: {}", id);
        MarcheResponseDTO responseDTO = marcheService.getMarcheById(id);
        log.info("Marché récupéré avec succès - ID: {}, Numéro: {}", responseDTO.getId(), responseDTO.getNumero());
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Endpoint to get all Marches with search, pagination, and sorting.
     * HTTP Method: GET
     * URI: /api/marches
     *
     * @param searchDTO The DTO for search criteria (e.g., ?nom=test&numero=123).
     * @param pageable  The pagination and sorting information (e.g., ?page=0&size=10&sort=nom,asc).
     * @return A paginated list of Marche response DTOs.
     */
    @GetMapping
    @PreAuthorize(AUTHENTICATED)
    public ResponseEntity<Page<MarcheResponseDTO>> getAllMarches(
            MarcheSearchDTO searchDTO,
            Pageable pageable) {
        log.info("Recherche de marchés - Critères: {}, Page: {}, Taille: {}",
                searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Page<MarcheResponseDTO> marches = marcheService.findAllMarches(searchDTO, pageable);

        log.info("Marchés récupérés avec succès - Nombre total: {}, Éléments de la page: {}",
                marches.getTotalElements(), marches.getNumberOfElements());
        return ResponseEntity.ok(marches);
    }

    /**
     * Endpoint to update an existing Marche.
     * HTTP Method: PUT
     * URI: /api/marches/{id}
     *
     * @param id The ID of the Marche to update.
     * @param requestDTO The DTO with the updated data.
     * @return A ResponseEntity with the updated Marche and HTTP status 200.
     */
    @PutMapping("/{id}")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> updateMarche(@PathVariable Long id,
                                                          @Valid @RequestBody MarcheRequestDTO requestDTO) {
        log.info("Début de la mise à jour du marché - ID: {}, Nouveau données: {}",
                id, requestDTO);
        MarcheResponseDTO responseDTO = marcheService.updateMarche(id, requestDTO);
        log.info("Marché mis à jour avec succès - ID: {}, Numéro: {}",
                responseDTO.getId(), responseDTO.getNumero());
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Endpoint to delete a Marche by its ID.
     * HTTP Method: DELETE
     * URI: /api/marches/{id}
     * Réservé SYSTEM_ADMIN. Refusé si le marché a des données liées (cf. {@code MarcheService}).
     *
     * @param id The ID of the Marche to delete.
     * @return A ResponseEntity with a status of 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize(SYSTEM_ADMIN)
    public ResponseEntity<Void> deleteMarche(@PathVariable Long id) {
        log.info("Début de la suppression du marché - ID: {}", id);
        marcheService.deleteMarche(id);
        log.info("Marché supprimé avec succès - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint to archive a Marche by its ID.
     * HTTP Method: PATCH
     * URI: /api/marches/{id}/archiver
     * Le marché reste consultable mais sort définitivement des calculs futurs (agios, encours).
     *
     * @param id The ID of the Marche to archive.
     * @return A ResponseEntity with the archived Marche and HTTP status 200.
     */
    @PatchMapping("/{id}/archiver")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> archiverMarche(@PathVariable Long id) {
        log.info("Début de l'archivage du marché - ID: {}", id);
        MarcheResponseDTO responseDTO = marcheService.archiverMarche(id);
        log.info("Marché archivé avec succès - ID: {}", id);
        return ResponseEntity.ok(responseDTO);
    }

    /**
     * Endpoint to reactivate an archived Marche by its ID.
     * HTTP Method: PATCH
     * URI: /api/marches/{id}/desarchiver
     *
     * @param id The ID of the Marche to reactivate.
     * @return A ResponseEntity with the reactivated Marche and HTTP status 200.
     */
    @PatchMapping("/{id}/desarchiver")
    @PreAuthorize(FINANCE_OR_ADMIN)
    public ResponseEntity<MarcheResponseDTO> desarchiverMarche(@PathVariable Long id) {
        log.info("Début du désarchivage du marché - ID: {}", id);
        MarcheResponseDTO responseDTO = marcheService.desarchiverMarche(id);
        log.info("Marché désarchivé avec succès - ID: {}", id);
        return ResponseEntity.ok(responseDTO);
    }
}