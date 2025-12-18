package staport.finance.gBanque.leasing.contratLeasing.controller;

import jakarta.validation.Valid;
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
        ContratLeasingResponseDTO createdContrat = contratLeasingService.createContratLeasing(requestDTO);
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
        ContratLeasingResponseDTO contrat = contratLeasingService.getContratLeasingById(id);
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
        ContratLeasingResponseDTO updatedContrat = contratLeasingService.updateContratLeasing(id, requestDTO);
        return ResponseEntity.ok(updatedContrat); // Return 200 OK
    }

    @PutMapping("/{id}/markAsInactive")
    public ResponseEntity<ContratLeasingResponseDTO> markAsInactive(@PathVariable Long id) {
        ContratLeasingResponseDTO updatedContrat = contratLeasingService.markAsInactive(id);
        return ResponseEntity.ok(updatedContrat); // Return 200 OK
    }

    @GetMapping("/actifs")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> getAllContratsLeasing(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.getActiveContrats(searchDTO, pageable);
        return ResponseEntity.ok(resultPage);
    }

    @GetMapping("/en-attente-main-levee")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> searchPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.getPendingLienReleaseContrats(searchDTO, pageable);
        return ResponseEntity.ok(resultPage);
    }

    @GetMapping("/inactifs")
    public ResponseEntity<Page<ContratLeasingResponseDTO>> searchInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.getInactiveContrats(searchDTO, pageable);
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
        contratLeasingService.deleteContratLeasing(id);
        return ResponseEntity.noContent().build(); // Return 204 No Content
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
    public ResponseEntity<TableauAmortissementDTO> getContratLeasingAmortizationTable(
            @PathVariable Long contratId,
            EcheanceLeasingSearchDTO searchDTO, // Spring will auto-bind query parameters here
            Pageable pageable) { // Spring will auto-bind page, size, sort query parameters here
        // Call the service method, passing all the necessary parameters
        TableauAmortissementDTO tableau = echeanceLeasingService.getTableauAmortissementForContrat(contratId, searchDTO, pageable);
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
    public ResponseEntity<String> uploadPdfToContrat(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {

        // Validation du fichier
        if (file.isEmpty()) {
            throw new ValidationException("Le fichier est vide.");
        }

        // Appel au service pour attacher le PDF
        contratLeasingService.attachOrReplacePdfToContrat(
                id,
                file.getBytes(),
                file.getOriginalFilename()
        );

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

        // Récupérer le PDF depuis le service
        PdfFileDTO pdfFile = contratLeasingService.getPdfFromContrat(id);

        // Configurer les headers HTTP pour le téléchargement
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(pdfFile.getNomFichier())
                        .build()
        );
        headers.setContentLength(pdfFile.getTailleFichier());

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

        contratLeasingService.removePdfFromContrat(id);

        return ResponseEntity.noContent().build(); // Return 204 No Content
    }



}