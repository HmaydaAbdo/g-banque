package staport.finance.gBanque.leasing.contratLeasing.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException; // These are handled by global handler
import staport.finance.gBanque.exception.customExceptions.ValidationException;     // These are handled by global handler
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.PdfFileDTO;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.TableauAmortissementDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.services.EcheanceLeasingService;

import java.io.IOException;


/**
 * REST Controller for managing ContratLeasing operations.
 * Handles incoming HTTP requests and delegates to ContratLeasingService.
 * Base path for this controller will be /gBanque/api/contratsLeasing.
 * Exceptions (like ValidationException, ResourceNotFoundException) are expected to be
 * handled by a global @ControllerAdvice for consistent error responses.
 */
@RestController
@RequestMapping("/contratsLeasing") // Defines the base URL path for all endpoints in this controller
public class ContratLeasingController {


    private static final Logger logger = LoggerFactory.getLogger(ContratLeasingController.class);
    private final ContratLeasingService contratLeasingService;
    private final EcheanceLeasingService echeanceLeasingService;

    /**
     * Constructor for ContratLeasingController, injecting the ContratLeasingService.
     * Spring automatically provides the instance of ContratLeasingService.
     *
     * @param contratLeasingService The service layer for leasing contracts.
     */
    public ContratLeasingController(ContratLeasingService contratLeasingService, EcheanceLeasingService echeanceLeasingService) {
        this.contratLeasingService = contratLeasingService;
        this.echeanceLeasingService = echeanceLeasingService;
    }

    /**
     * Endpoint to create a new leasing contract.
     * Maps to POST /gBanque/api/contratsLeasing
     *
     * @param requestDTO The DTO containing the data for the new contract.
     * @Valid annotation triggers validation rules defined in ContratLeasingRequestDTO
     * before the request even reaches the method body.
     * @return ResponseEntity with the created ContratLeasingResponseDTO and HTTP Status 201 (Created).
     * @throws ValidationException (Handled by global exception handler)
     * @throws ResourceNotFoundException (Handled by global exception handler)
     */
    @PostMapping
    public ResponseEntity<ContratLeasingResponseDTO> createContratLeasing(@Valid @RequestBody ContratLeasingRequestDTO requestDTO) {
        logger.info("Début de la création d'un nouveau contrat de leasing");
        logger.debug("Données reçues pour la création : {}", requestDTO);
        ContratLeasingResponseDTO createdContrat = contratLeasingService.createContratLeasing(requestDTO);
        logger.info("Contrat de leasing créé avec succès avec l'ID : {}", createdContrat.getId());
        return new ResponseEntity<>(createdContrat, HttpStatus.CREATED); // Return 201 Created
    }

    /**
     * Endpoint to retrieve a leasing contract by its ID.
     * Maps to GET /gBanque/api/contratsLeasing/{id}
     *
     * @param id The ID of the contract to retrieve.
     * @return ResponseEntity with the found ContratLeasingResponseDTO and HTTP Status 200 (OK).
     * @throws ResourceNotFoundException (Handled by global exception handler)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContratLeasingResponseDTO> getContratLeasingById(@PathVariable Long id) {
        logger.info("Récupération du contrat de leasing avec l'ID : {}", id);
        ContratLeasingResponseDTO contrat = contratLeasingService.getContratLeasingById(id);
        logger.debug("Contrat de leasing trouvé : {}", contrat.getNumeroContrat());
        return ResponseEntity.ok(contrat); // Return 200 OK
    }

    /**
     * Endpoint to update an existing leasing contract by its ID.
     * Maps to PUT /gBanque/api/contratsLeasing/{id}
     *
     * @param id The ID of the contract to update.
     * @param requestDTO The DTO containing the updated data.
     * @return ResponseEntity with the updated ContratLeasingResponseDTO and HTTP Status 200 (OK).
     * @throws ResourceNotFoundException (Handled by global exception handler)
     * @throws ValidationException (Handled by global exception handler)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContratLeasingResponseDTO> updateContratLeasing(@PathVariable Long id, @Valid @RequestBody ContratLeasingRequestDTO requestDTO) {
        logger.info("Début de la mise à jour du contrat de leasing avec l'ID : {}", id);
        logger.debug("Nouvelles données reçues : {}", requestDTO);
        ContratLeasingResponseDTO updatedContrat = contratLeasingService.updateContratLeasing(id, requestDTO);
        logger.info("Contrat de leasing mis à jour avec succès avec l'ID : {}", id);
        return ResponseEntity.ok(updatedContrat); // Return 200 OK
    }

    @PutMapping("/{id}/markAsInactive")
    public ResponseEntity<ContratLeasingResponseDTO> markAsInactive(@PathVariable Long id) {
        logger.info("Début du marquage du contrat ID : {} comme inactif", id);
        ContratLeasingResponseDTO updatedContrat = contratLeasingService.markAsInactive(id);
        logger.info("Contrat ID : {} marqué comme inactif avec succès", id);
        return ResponseEntity.ok(updatedContrat); // Return 200 OK
    }

    @GetMapping("/actifs")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> getAllContratsLeasing(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        logger.info("Récupération des contrats de leasing actifs");
        logger.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.getActiveContrats(searchDTO, pageable);
        logger.info("Nombre de contrats actifs trouvés : {} sur {} au total", resultPage.getNumberOfElements(), resultPage.getTotalElements());
        return ResponseEntity.ok(resultPage);
    }

    @GetMapping("/en-attente-main-levee")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> searchPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        logger.info("Récupération des contrats en attente de main levée");
        logger.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.getPendingLienReleaseContrats(searchDTO, pageable);
        logger.info("Nombre de contrats en attente de main levée trouvés : {} sur {} au total", resultPage.getNumberOfElements(), resultPage.getTotalElements());
        return ResponseEntity.ok(resultPage);
    }

    @GetMapping("/inactifs")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> searchInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        logger.info("Récupération des contrats de leasing inactifs");
        logger.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());
        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.getInactiveContrats(searchDTO, pageable);
        logger.info("Nombre de contrats inactifs trouvés : {} sur {} au total", resultPage.getNumberOfElements(), resultPage.getTotalElements());
        return ResponseEntity.ok(resultPage);
    }


        /**
         * Endpoint to delete a leasing contract by its ID.
         * Maps to DELETE /gBanque/api/contratsLeasing/{id}
         *
         * @param id The ID of the contract to delete.
         * @return ResponseEntity with no content and HTTP Status 204 (No Content) on successful deletion.
         * @throws ResourceNotFoundException (Handled by global exception handler)
         */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContratLeasing(@PathVariable Long id) {
        logger.info("Début de la suppression du contrat de leasing avec l'ID : {}", id);

        contratLeasingService.deleteContratLeasing(id);

        logger.info("Contrat de leasing supprimé avec succès avec l'ID : {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the amortization table for a specific leasing contract,
     * with pagination, sorting, and optional search criteria
     * Maps to GET /gBanque/api/contratsLeasing/{contratId}/tableauAmortissement
     *
     * @param contratId The ID of the leasing contract.
     * @param searchDTO An object containing search filters for installments (e.g., numeroEcheance, month, year, statutPaiement).
     * Spring automatically binds request parameters to this DTO.
     * Example: /api/contratsLeasing/1/tableauAmortissement?numeroEcheance=3&month=10&year=2024&statutPaiement=PREVU
     * @param pageable  Pagination and sorting information provided by Spring Data.
     * Parameters: page, size, sort.
     * Example: /api/contratsLeasing/1/tableauAmortissement?page=0&size=10&sort=dateEcheance,asc
     * @return ResponseEntity with the TableauAmortissementDTO and HTTP Status 200 (OK).
     * @throws ResourceNotFoundException (Handled by global exception handler) if the contract is not found.
     */
    @GetMapping("/{contratId}/tableauAmortissement")
    public ResponseEntity<TableauAmortissementDTO> getContratLeasingAmortizationTable(@PathVariable Long contratId, EcheanceLeasingSearchDTO searchDTO, Pageable pageable) {
        logger.info("Récupération du tableau d'amortissement pour le contrat ID : {}", contratId);
        logger.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());
        TableauAmortissementDTO tableau = echeanceLeasingService.getTableauAmortissementForContrat(contratId, searchDTO, pageable);
        logger.info("Tableau d'amortissement récupéré avec succès pour le contrat ID : {}", contratId);
        return ResponseEntity.ok(tableau);
    }

    /**
     * Endpoint pour uploader ou remplacer le fichier PDF d'un contrat.
     * Maps to POST /contratsLeasing/{id}/pdf
     *
     * @param id L'ID du contrat
     * @param file Le fichier PDF à uploader
     * @return ResponseEntity avec un message de succès et HTTP Status 200 (OK)
     * @throws ResourceNotFoundException Si le contrat n'existe pas
     * @throws ValidationException Si le fichier n'est pas valide
     */
    @PostMapping("/{id}/pdf")
    public ResponseEntity<String> uploadPdfToContrat(@PathVariable Long id, @RequestParam("file") MultipartFile file) throws IOException {
        logger.info("Début de l'upload du fichier PDF pour le contrat ID : {}", id);
        logger.debug("Nom du fichier : {}, Taille : {} octets", file.getOriginalFilename(), file.getSize());

        if (file.isEmpty()) {
            logger.error("Tentative d'upload d'un fichier vide pour le contrat ID : {}", id);
            throw new ValidationException("Le fichier est vide.");
        }

        contratLeasingService.attachOrReplacePdfToContrat(
                id,
                file.getBytes(),
                file.getOriginalFilename()
        );

        logger.info("Fichier PDF uploadé avec succès pour le contrat ID : {}", id);
        return ResponseEntity.ok("Fichier PDF uploadé avec succès.");
    }

    /**
     * Endpoint pour télécharger le fichier PDF d'un contrat.
     * Maps to GET /contratsLeasing/{id}/pdf
     *
     * @param id L'ID du contrat
     * @return ResponseEntity avec le contenu du PDF et les headers appropriés
     * @throws ResourceNotFoundException Si le contrat ou le PDF n'existe pas
     */
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdfFromContrat(@PathVariable Long id) {
        logger.info("Début du téléchargement du fichier PDF pour le contrat ID : {}", id);

        PdfFileDTO pdfFile = contratLeasingService.getPdfFromContrat(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(pdfFile.getNomFichier())
                        .build()
        );
        headers.setContentLength(pdfFile.getTailleFichier());

        logger.info("Fichier PDF téléchargé avec succès pour le contrat ID : {} - Nom : {}, Taille : {} octets",
                id, pdfFile.getNomFichier(), pdfFile.getTailleFichier());
        return new ResponseEntity<>(pdfFile.getContenu(), headers, HttpStatus.OK);
    }
    /**
     * Endpoint pour supprimer le fichier PDF d'un contrat.
     * Maps to DELETE /contratsLeasing/{id}/pdf
     *
     * @param id L'ID du contrat
     * @return ResponseEntity avec un message de succès et HTTP Status 204 (No Content)
     * @throws ResourceNotFoundException Si le contrat n'existe pas
     */
    @DeleteMapping("/{id}/pdf")
    public ResponseEntity<Void> deletePdfFromContrat(@PathVariable Long id) {
        logger.info("Début de la suppression du fichier PDF pour le contrat ID : {}", id);

        contratLeasingService.removePdfFromContrat(id);

        logger.info("Fichier PDF supprimé avec succès pour le contrat ID : {}", id);
        return ResponseEntity.noContent().build();
    }



}