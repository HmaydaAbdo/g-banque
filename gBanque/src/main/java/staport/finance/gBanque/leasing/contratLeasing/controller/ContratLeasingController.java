package staport.finance.gBanque.leasing.contratLeasing.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException; // These are handled by global handler
import staport.finance.gBanque.exception.customExceptions.ValidationException;     // These are handled by global handler
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.EcheanceLeasingSearchDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.TableauAmortissementDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.services.EcheanceLeasingService;


/**
 * REST Controller for managing ContratLeasing operations.
 * Handles incoming HTTP requests and delegates to ContratLeasingService.
 * Base path for this controller will be /gBanque/api/contratsLeasing.
 *
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
     * Retrieves a paginated and sorted list of leasing contracts, with optional search criteria.
     *
     * @param searchDTO An object containing the search filters (e.g., numeroContrat, typeContrat, montantAchatTTC, etc.).
     * Spring automatically binds request parameters to this DTO.
     * Example: /api/contrats-leasing?numeroContrat=CL-001&typeContrat=LINEAIRE
     * @param pageable  Pagination and sorting information provided by Spring Data.
     * Parameters:
     * - page: (0-indexed) The page number to retrieve. Default is 0.
     * - size: The number of items per page. Default is 20.
     * - sort: Comma-separated list of properties to sort by, optionally followed by (ASC|DESC).
     * Example: /api/contrats-leasing?page=0&size=10&sort=numeroContrat,desc&sort=montantAchatTTC
     * @return A {@link Page} of {@link ContratLeasingResponseDTO} objects, wrapped in a {@link ResponseEntity}.
     */
    @GetMapping // Use a single GET endpoint for listing, searching, paginating, and sorting actif contracts
    public ResponseEntity<Page<ContratLeasingResponseDTO>> getAllContratsLeasing(
            ContratLeasingSearchDTO searchDTO, // DTO for search criteria, automatically mapped from request params
            Pageable pageable) { // Pagination and sorting parameters
        Page<ContratLeasingResponseDTO> resultPage = contratLeasingService.searchOnActifOnly(searchDTO, pageable);
        return ResponseEntity.ok(resultPage); // Return 200 OK with the paginated results
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


}