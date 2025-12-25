package staport.finance.gBanque.marches.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Controller
@RequestMapping("/marches")
public class MarcheController {

    private static final Logger log = LoggerFactory.getLogger(MarcheController.class);
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
     *
     * @param id The ID of the Marche to delete.
     * @return A ResponseEntity with a status of 204 (No Content).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMarche(@PathVariable Long id) {
        log.info("Début de la suppression du marché - ID: {}", id);
        marcheService.deleteMarche(id);
        log.info("Marché supprimé avec succès - ID: {}", id);
        return ResponseEntity.noContent().build();
    }
}