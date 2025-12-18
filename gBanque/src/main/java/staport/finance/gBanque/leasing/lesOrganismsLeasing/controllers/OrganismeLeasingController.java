package staport.finance.gBanque.leasing.lesOrganismsLeasing.controllers;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingRequestDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingResponseDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.dtos.OrganismeLeasingSearchDTO;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.services.interfaces.OrganismeLeasingService;


@RestController
@RequestMapping("/0rganismesLeasing")
public class OrganismeLeasingController {

    private static final Logger logger = LoggerFactory.getLogger(OrganismeLeasingController.class);
    private final OrganismeLeasingService organismeLeasingService;

    public OrganismeLeasingController(OrganismeLeasingService organismeLeasingService) {
        this.organismeLeasingService = organismeLeasingService;
    }

    @PostMapping
    public ResponseEntity<OrganismeLeasingResponseDTO> createOrganismeLeasing(@Valid @RequestBody OrganismeLeasingRequestDTO requestDTO) {
        logger.info("Début de la création d'un nouvel organisme de leasing");
        logger.debug("Données reçues pour la création : {}", requestDTO);
        OrganismeLeasingResponseDTO createdOrganisme = organismeLeasingService.create(requestDTO);
        logger.info("Organisme de leasing créé avec succès avec l'ID : {}", createdOrganisme.getId());
        return new ResponseEntity<>(createdOrganisme, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganismeLeasingResponseDTO> updateOrganismeLeasing(@PathVariable Long id, @Valid @RequestBody OrganismeLeasingRequestDTO requestDTO) {
        logger.info("Début de la mise à jour de l'organisme de leasing avec l'ID : {}", id);
        logger.debug("Données reçues pour la mise à jour : {}", requestDTO);
        OrganismeLeasingResponseDTO updatedOrganisme = organismeLeasingService.update(id, requestDTO);
        logger.info("Organisme de leasing mis à jour avec succès avec l'ID : {}", id);
        return ResponseEntity.ok(updatedOrganisme);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganismeLeasingResponseDTO> getOrganismeLeasingById(@PathVariable Long id) {
        logger.info("Récupération de l'organisme de leasing avec l'ID : {}", id);
        OrganismeLeasingResponseDTO organisme = organismeLeasingService.getById(id);
        logger.debug("Organisme de leasing trouvé : {}", organisme);
        return ResponseEntity.ok(organisme);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganismeLeasing(@PathVariable Long id) {
        logger.info("Début de la suppression de l'organisme de leasing avec l'ID : {}", id);
        organismeLeasingService.delete(id);
        logger.info("Organisme de leasing supprimé avec succès avec l'ID : {}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<Page<OrganismeLeasingResponseDTO>> searchOrganismeLeasings(OrganismeLeasingSearchDTO criteria, Pageable pageable) {
        logger.info("Début de la recherche d'organismes de leasing");
        logger.debug("Critères de recherche : {}, Pagination : page={}, taille={}", criteria, pageable.getPageNumber(), pageable.getPageSize());
        Page<OrganismeLeasingResponseDTO> organismes = organismeLeasingService.search(criteria, pageable);
        logger.info("Recherche terminée avec succès. Nombre d'éléments trouvés : {} sur {} au total", organismes.getNumberOfElements(), organismes.getTotalElements());
        return ResponseEntity.ok(organismes);
    }
}