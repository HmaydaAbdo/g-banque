package staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.controller;


import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.dtos.*;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.services.LigneFonctionnelleSpecifiqueService;
import staport.finance.gBanque.droitConstate.dtos.AgregatedSuivieDcLigneResponseDto;
import staport.finance.gBanque.factoring.dtos.AgregatedSuivieFactoringLigneResponseDto;
import staport.finance.gBanque.prefinancement.dtos.AgregatedSuiviePrefiLigneResponseDto;



@RestController
@RequestMapping("/lignes-fonctionnelles-specifiques")
public class LigneFonctionnelleSpecifiqueController {

    private static final Logger logger = LoggerFactory.getLogger(LigneFonctionnelleSpecifiqueController.class);
    private final LigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService;


    public LigneFonctionnelleSpecifiqueController(LigneFonctionnelleSpecifiqueService ligneFonctionnelleSpecifiqueService) {
        this.ligneFonctionnelleSpecifiqueService = ligneFonctionnelleSpecifiqueService;
    }

    @PostMapping
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> createLigneFonctionnelleSpecifique(@Valid @RequestBody LigneFonctionnelleSpecifiqueRequest request) {
        logger.info("Requête de création d'une ligne fonctionnelle spécifique reçue");
        logger.debug("Détails de la requête: {}", request);
        LigneFonctionnelleSpecifiqueResponse createdLigne = ligneFonctionnelleSpecifiqueService.createLigneFonctionnelleSpecifique(request);
        logger.info("Ligne fonctionnelle spécifique créée avec succès - ID: {}", createdLigne.getId());
        return new ResponseEntity<>(createdLigne, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> getLigneFonctionnelleSpecifiqueById(@PathVariable Long id) {
        logger.info("Requête de récupération de la ligne fonctionnelle spécifique - ID: {}", id);
        LigneFonctionnelleSpecifiqueResponse ligne = ligneFonctionnelleSpecifiqueService.getLigneFonctionnelleSpecifiqueById(id);
        logger.debug("Ligne fonctionnelle spécifique récupérée: {}", ligne);
        return ResponseEntity.ok(ligne);
    }

    @GetMapping
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLignesFonctionnellesSpecifiques(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSpecifiqueSearchCriteria searchCriteria) {

        logger.info("Requête de liste des lignes fonctionnelles spécifiques - Page: {}, Size: {}", page, size);
        logger.debug("Critères de recherche: {}", searchCriteria);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleSpecifiqueResponse> lignes = ligneFonctionnelleSpecifiqueService.getLignesFonctionnellesSpecifiques(searchCriteria, pageable);
        logger.info("Récupération réussie - {} lignes trouvées sur {} au total", lignes.getNumberOfElements(), lignes.getTotalElements());

        return ResponseEntity.ok(lignes);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LigneFonctionnelleSpecifiqueResponse> updateLigneFonctionnelleSpecifique(
            @PathVariable Long id,
            @Valid @RequestBody LigneFonctionnelleSpecifiqueRequest request) {
        logger.info("Requête de mise à jour de la ligne fonctionnelle spécifique - ID: {}", id);
        logger.debug("Nouvelles données: {}", request);
        LigneFonctionnelleSpecifiqueResponse updatedLigne = ligneFonctionnelleSpecifiqueService.updateLigneFonctionnelleSpecifique(id, request);
        logger.info("Ligne fonctionnelle spécifique mise à jour avec succès - ID: {}", id);
        return ResponseEntity.ok(updatedLigne);
    }

    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLignesFonctionnellesSpecifiquesByBanqueId(
            @PathVariable Long banqueId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            @ModelAttribute LigneFonctionnelleSpecifiqueSearchCriteria searchCriteria) {

        logger.info("Requête des lignes fonctionnelles spécifiques par banque - BanqueID: {}, Page: {}, Size: {}", banqueId, page, size);
        logger.debug("Critères de recherche: {}", searchCriteria);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleSpecifiqueResponse> lignes = ligneFonctionnelleSpecifiqueService.getLignesFonctionnellesSpecifiquesByBanqueId(banqueId, searchCriteria, pageable);
        logger.info("Récupération réussie pour banque {} - {} lignes trouvées", banqueId, lignes.getTotalElements());

        return ResponseEntity.ok(lignes);
    }

    /**
     * Deactivates a specific  functional line by id
     * PUT /lignes-fonctionnelles-specifique/deactivate/{ligneId}
     * @param ligneId The ID of the functional line.
     * @return HTTP status 204 No Content on success.
     */
    @PutMapping("/deactivate/{ligneId}")
    public ResponseEntity<Void> deactivateLigneFonctionnelle(
            @PathVariable Long ligneId) throws Exception {
        logger.info("Requête de désactivation de la ligne fonctionnelle - ID: {}", ligneId);
        ligneFonctionnelleSpecifiqueService.deactivateLigneFonctionnelleById(ligneId);
        logger.info("Ligne fonctionnelle désactivée avec succès - ID: {}", ligneId);
        return ResponseEntity.noContent().build();
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLigneFonctionnelleSpecifique(@PathVariable Long id) {
        logger.info("Requête de suppression de la ligne fonctionnelle spécifique - ID: {}", id);
        ligneFonctionnelleSpecifiqueService.deleteLigneFonctionnelleSpecifique(id);
        logger.info("Ligne fonctionnelle spécifique supprimée avec succès - ID: {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/marche/{marcheId}")
    public ResponseEntity<Page<LigneFonctionnelleSpecifiqueResponse>> getLinesByMarche(
            @PathVariable Long marcheId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        logger.info("Requête des lignes pour le marché ID: {}",marcheId);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<LigneFonctionnelleSpecifiqueResponse> lines = ligneFonctionnelleSpecifiqueService.getLinesByMarche(
                marcheId,
                pageable
        );

        logger.info("Lignes récupérées pour marché {} - {} résultats sur {} au total",
                marcheId, lines.getNumberOfElements(), lines.getTotalElements());

        return ResponseEntity.ok(lines);
    }


    @GetMapping("/{id}/prefinancements")
    public ResponseEntity<AgregatedSuiviePrefiLigneResponseDto> getPrefinancementsByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        logger.info("Requête de suivi des préfinancements pour la ligne - ID: {}, Page: {}, Size: {}", id, page, size);
        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);
        AgregatedSuiviePrefiLigneResponseDto res = ligneFonctionnelleSpecifiqueService.getSuiviePrefiLigneResponse(id, pageable);
        logger.info("Suivi des préfinancements récupéré avec succès pour la ligne ID: {}", id);

        return ResponseEntity.ok(res);
    }


    @GetMapping("/{id}/dc")
    public ResponseEntity<AgregatedSuivieDcLigneResponseDto> getDroitConstateByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        logger.info("Requête de suivi des droits constatés pour la ligne - ID: {}, Page: {}, Size: {}", id, page, size);


        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        AgregatedSuivieDcLigneResponseDto res = ligneFonctionnelleSpecifiqueService.getSuivieDcLigneResponse(id, pageable);
        logger.info("Suivi des droits constatés récupéré avec succès pour la ligne ID: {}", id);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{id}/factoring")
    public ResponseEntity<AgregatedSuivieFactoringLigneResponseDto> getFactoringByLineId(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        logger.info("Requête de suivi du factoring pour la ligne - ID: {}, Page: {}, Size: {}", id, page, size);

        Sort.Direction direction = Sort.Direction.fromString(sort[1]);
        Sort sortOrder = Sort.by(direction, sort[0]);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        AgregatedSuivieFactoringLigneResponseDto res = ligneFonctionnelleSpecifiqueService.getSuivieFactoringLigneResponse(id, pageable);

        logger.info("Suivi du factoring récupéré avec succès pour la ligne ID: {}", id);
        return ResponseEntity.ok(res);
    }
}