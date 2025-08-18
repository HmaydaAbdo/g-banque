
package staport.finance.gBanque.leasing.contratLeasing.service.imp;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.dto.*;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.mapper.ContratLeasingMapper;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.contratLeasing.specification.ContratLeasingSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.services.EcheanceLeasingService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Implementation of the ContratLeasingService interface.
 * Manages the business logic for leasing contract management.
 */
@Service
public class ContratLeasingServiceImpl implements ContratLeasingService {

    private final ContratLeasingRepository contratLeasingRepository;
    private final ContratLeasingMapper contratLeasingMapper;
    private final BanqueRepository banqueRepository;
    private final OrganismeLeasingRepository organismeLeasingRepository;
    private final  EcheanceLeasingService echeanceLeasingService;
    // Defines the residual value percentage as a constant
    private static final BigDecimal RESIDUAL_VALUE_PERCENTAGE = new BigDecimal("0.01"); // 1%

    /**
     * Constructor for ContratLeasingServiceImpl, injecting necessary dependencies.
     *
     * @param contratLeasingRepository The repository for ContratLeasing entities.
     * @param contratLeasingMapper The mapper for converting between DTOs and entities.
     * @param banqueRepository The repository for Banque entities.
     * @param organismeLeasingRepository The repository for OrganismeLeasing entities.
     */
    public ContratLeasingServiceImpl(EcheanceLeasingService echeanceLeasingService, ContratLeasingRepository contratLeasingRepository,
                                     ContratLeasingMapper contratLeasingMapper,
                                     BanqueRepository banqueRepository,
                                     OrganismeLeasingRepository organismeLeasingRepository) {
        this.contratLeasingRepository = contratLeasingRepository;
        this.contratLeasingMapper = contratLeasingMapper;
        this.banqueRepository = banqueRepository;
        this.organismeLeasingRepository = organismeLeasingRepository;
        this.echeanceLeasingService = echeanceLeasingService;
    }

    /**
     * Creates a new leasing contract after validating its business rules.
     *
     * @param requestDTO The DTO containing the new contract's data.
     * @return The response DTO of the created contract.
     * @throws ValidationException If business validations fail.
     * @throws ResourceNotFoundException If the associated Banque or OrganismeLeasing are not found.
     */
    @Override
    @Transactional // Ensures the entire operation is atomic (all or nothing)
    public ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) throws ValidationException {

        // 1. Perform common business validations and type-specific validations
        validateCommonContratLeasingRequest(requestDTO, null); // Pass null for ID as it's a creation
        validateTypeSpecificContratLeasingRequest(requestDTO);

        // 2. Retrieve related entities (Banque and OrganismeLeasing)
        // If not found, a ResourceNotFoundException is thrown.
        Banque banque = banqueRepository.findById(requestDTO.getBanqueDePrelevementId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque with ID " + requestDTO.getBanqueDePrelevementId() + " not found."));

        OrganismeLeasing organismeLeasing = organismeLeasingRepository.findById(requestDTO.getOrganismeLeasingId())
                .orElseThrow(() -> new ResourceNotFoundException("OrganismeLeasing with ID " + requestDTO.getOrganismeLeasingId() + " not found."));

        // 3. Map the DTO to the ContratLeasing entity
        ContratLeasing contratLeasing = contratLeasingMapper.toEntity(requestDTO, banque, organismeLeasing);

        // 4. Calculate and set the Residual Value based on the new rule (1% of MontantAchatTTC for all types)
        // This logic is now uniform for all contract types.
        setValeurResiduelleBasedOnType(contratLeasing, requestDTO.getMontantAchatTTC());

        // 5. Establish the bidirectional relationship for phases.
        // This is crucial for JPA to correctly persist the association from the phase side.
        if (contratLeasing.getPhases() != null) {
            contratLeasing.getPhases().forEach(phase -> phase.setContratLeasing(contratLeasing));
        }

        // 6. Save the new contract to the database
        ContratLeasing savedContrat = contratLeasingRepository.save(contratLeasing);

        // 7. Générer les échéances pour le contrat nouvellement créé

        echeanceLeasingService.generateEcheances(savedContrat);


        // 7. Map the saved entity to a response DTO and return it
        return contratLeasingMapper.toDto(savedContrat);
    }

    /**
     * Retrieves a leasing contract by its ID.
     *
     * @param id The ID of the contract.
     * @return The response DTO of the found contract.
     * @throws ResourceNotFoundException If no contract exists with the given ID.
     */
    @Override
    @Transactional(readOnly = true) // Read-only transaction for performance
    public ContratLeasingResponseDTO getContratLeasingById(Long id) throws ResourceNotFoundException {
        // Finds the contract by ID and maps it to a DTO, or throws an exception if not found.
        return contratLeasingRepository.findById(id)
                .map(contratLeasingMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("ContratLeasing with ID " + id + " not found."));
    }

    /**
     * Updates an existing leasing contract by its ID after validating the changes.
     *
     * @param id The ID of the contract to update.
     * @param requestDTO The DTO containing the updated contract data.
     * @return The response DTO of the updated contract.
     * @throws ResourceNotFoundException If the contract or associated entities are not found.
     * @throws ValidationException If business validations fail.
     */
    @Override
    @Transactional // Ensures the update operation is atomic
    public ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) throws ResourceNotFoundException, ValidationException {
        // 1. Retrieve the existing contract entity
        ContratLeasing existingContrat = contratLeasingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContratLeasing with ID " + id + " not found."));

        // 2. Perform common business validations and type-specific validations on the updated data
        validateCommonContratLeasingRequest(requestDTO, id); // Pass the ID for uniqueness checks during update
        validateTypeSpecificContratLeasingRequest(requestDTO);

        // 3. Re-fetch related entities (Banque and OrganismeLeasing), in case their IDs changed in the DTO
        Banque banque = banqueRepository.findById(requestDTO.getBanqueDePrelevementId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque with ID " + requestDTO.getBanqueDePrelevementId() + " not found."));

        OrganismeLeasing organismeLeasing = organismeLeasingRepository.findById(requestDTO.getOrganismeLeasingId())
                .orElseThrow(() -> new ResourceNotFoundException("OrganismeLeasing with ID " + requestDTO.getOrganismeLeasingId() + " not found."));

        // 4. Update the existing entity using the mapper.
        // The mapper handles property copying and phase collection management ("clear and re-add" strategy).
        contratLeasingMapper.updateEntityFromDto(requestDTO, existingContrat, banque, organismeLeasing);

        // 5. Calculate and set the Residual Value based on the new rule (1% of MontantAchatTTC for all types)
        // This is applied after mapping, using the potentially updated MontantAchatTTC.
        setValeurResiduelleBasedOnType(existingContrat, existingContrat.getMontantAchatTTC()); // Use entity's montantAchatTTC as it's now updated

        // 6. Re-establish the bidirectional relationship for phases after the update.
        // This is important if phases were added, removed, or modified.
        if (existingContrat.getPhases() != null) {
            existingContrat.getPhases().forEach(phase -> phase.setContratLeasing(existingContrat));
        }

        // 7. Save the updated contract to the database
        ContratLeasing updatedContrat = contratLeasingRepository.save(existingContrat);

        this.echeanceLeasingService.regenerateEcheances(updatedContrat.getId());
        // 8. Map the updated entity to a response DTO and return it
        return contratLeasingMapper.toDto(updatedContrat);
    }

    /**
     * Deletes a leasing contract by its ID.
     *
     * @param id The ID of the contract to delete.
     * @throws ResourceNotFoundException If no contract exists with the given ID.
     */
    @Override
    @Transactional // Ensures the delete operation is atomic
    public void deleteContratLeasing(Long id) throws ResourceNotFoundException {
        // Checks if the contract exists before attempting to delete it
        if (!contratLeasingRepository.existsById(id)) {
            throw new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
        }
        contratLeasingRepository.deleteById(id);
    }

    /**
     * Searches for leasing contracts based on criteria and returns a paginated result.
     *
     * @param searchDTO The DTO containing the search filters.
     * @param pageable The pagination and sorting information.
     * @return A page of contract response DTOs.
     */
    @Override
    @Transactional(readOnly = true) // Read-only transaction for performance
    public Page<ContratLeasingResponseDTO> searchContratsLeasing(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        // Uses a Spring Data JPA Specification to build a dynamic query based on search criteria
        return contratLeasingRepository.findAll(ContratLeasingSpecification.bySearchCriteria(searchDTO), pageable)
                .map(contratLeasingMapper::toDto); // Maps each entity on the page to a DTO
    }

    // ⭐ Nouvelle méthode pour rechercher uniquement les contrats actifs
    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> searchOnActifOnly(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = ContratLeasingSpecification.bySearchCriteria(searchDTO);
        spec = spec.and((root, query, builder) -> builder.isFalse(root.get("completed")));
        return contratLeasingRepository.findAll(spec, pageable)
                .map(contratLeasingMapper::toDto);
    }

    // ⭐ Écouteur d'événement pour marquer le contrat comme terminé
    @EventListener
    @Transactional
    public void handleLeasingCompletedEvent(LeasingCompletedEvent event) {
        Long contratId = event.getContratId();
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> new ResourceNotFoundException("ContratLeasing introuvable avec l'ID : " + contratId));

        if (!contrat.isCompleted()) {
            contrat.setCompleted(true);
            contratLeasingRepository.save(contrat);
        }
    }

    /**
     * Performs **common business validations** applicable to all contract types.
     * This method ensures that fundamental data integrity rules are respected regardless of the contract's nature.
     *
     * @param requestDTO The DTO containing the contract data.
     * @param existingId The ID of the contract being updated. Null if it's a creation operation.
     * @throws ValidationException If a common business rule is violated.
     */
    private void validateCommonContratLeasingRequest(ContratLeasingRequestDTO requestDTO, Long existingId) {
        // Rule 1: Ensure `numeroContrat` is unique.
        Optional<ContratLeasing> existingByNumero = contratLeasingRepository.findByNumeroContrat(requestDTO.getNumeroContrat());

        if (existingByNumero.isPresent()) {
            if (existingId == null || !existingByNumero.get().getId().equals(existingId)) {
                throw new ValidationException("Le numéro de contrat '" + requestDTO.getNumeroContrat() + "' existe déjà. Il doit être unique.");
            }
        }

        // Rule 2: Period in months must be positive.
        if (requestDTO.getPeriodeEnMois() == null || requestDTO.getPeriodeEnMois() <= 0) {
            throw new ValidationException("La période en mois doit être un entier positif.");
        }

        // Rule 3: Purchase amount (MontantAchatTTC) must be positive.
        if (requestDTO.getMontantAchatTTC() == null || requestDTO.getMontantAchatTTC().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Le montant d'achat TTC est obligatoire et doit être positif.");
        }

        // MODIFIED: Rule 4: Residual value (ValeurResiduelle) must be null or positive.
        // It should not be negative if provided, as it will be calculated regardless.
        if (requestDTO.getValeurResiduelle() != null && requestDTO.getValeurResiduelle().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("La valeur résiduelle ne peut pas être un montant négatif.");
        }
    }

    /**
     * **Delegates** to type-specific contract validation functions.
     * This method ensures that validations are applied according to the specific rules
     * of LINEAIRE, PROGRESSIF, or DEGRESSIF contracts.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If the contract type is missing or unknown, or if type-specific rules are violated.
     */
    private void validateTypeSpecificContratLeasingRequest(ContratLeasingRequestDTO requestDTO) {
        // Rule: The contract type must be specified.
        if (requestDTO.getTypeContrat() == null) {
            throw new ValidationException("Le type de contrat est obligatoire.");
        }

        // Delegates to the appropriate validation method based on the contract type.
        switch (requestDTO.getTypeContrat()) {
            case LINEAIRE:
                validateLineaireContrat(requestDTO);
                break;
            case PROGRESSIF:
                validateProgressifContrat(requestDTO);
                break;
            case DEGRESSIF:
                validateDegressifContrat(requestDTO);
                break;
            default:
                // Fallback for any unknown or unhandled contract type (should not happen with an Enum)
                throw new ValidationException("Type de contrat inconnu: " + requestDTO.getTypeContrat());
        }
    }

    /**
     * Validates rules specific to **LINEAIRE** contracts.
     * Key characteristics: exactly one phase, now no financial coherence check based on payments.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If business rules specific to LINEAIRE contracts are violated.
     */
    private void validateLineaireContrat(ContratLeasingRequestDTO requestDTO) {
        // Rule 1: LINEAIRE contracts must have **exactly one single phase**.
        if (requestDTO.getPhases() == null || requestDTO.getPhases().size() != 1) {
            throw new ValidationException("Un contrat de type LINEAIRE doit avoir exactement une seule phase.");
        }

        // Retrieve the single phase for validation.
        ContratLeasingPhaseRequestDTO singlePhase = requestDTO.getPhases().get(0);

        // Rule 2: The single phase must have `numeroPhase` 1.
        if (singlePhase.getNumeroPhase() == null || singlePhase.getNumeroPhase() != 1) {
            throw new ValidationException("Pour un contrat LINEAIRE, la phase unique doit avoir le numéro 1.");
        }

        // Rule 3: The duration of the single phase must be positive.
        if (singlePhase.getDureeMois() == null || singlePhase.getDureeMois() <= 0) {
            throw new ValidationException("La durée de la phase du contrat LINEAIRE doit être un nombre de mois positif.");
        }

        // Rule 4: The monthly payment (Mensualite TTC) of the single phase must be positive.
        if (singlePhase.getMensualiteTTC() == null || singlePhase.getMensualiteTTC().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Le montant de la mensualité TTC de la phase du contrat LINEAIRE doit être un montant positif.");
        }

        // Rule 5: The duration of the single phase must match the total contract period.
        if (!singlePhase.getDureeMois().equals(requestDTO.getPeriodeEnMois())) {
            throw new ValidationException(String.format(
                    "Pour un contrat LINEAIRE, la durée de la phase unique (%d mois) doit correspondre à la période totale du contrat (%d mois).",
                    singlePhase.getDureeMois(), requestDTO.getPeriodeEnMois()
            ));
        }


    }

    /**
     * Validates rules specific to **PROGRESSIF** contracts.
     * Key characteristics: multiple phases, now no total cost matching check, increasing/constant monthly payments.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If business rules specific to PROGRESSIF contracts are violated.
     */
    private void validateProgressifContrat(ContratLeasingRequestDTO requestDTO) {
        // First, apply validations common to all phased contracts.
        validatePhasedContrat(requestDTO);

        // Rule: For PROGRESSIF contracts, monthly payments (mensualités) must be increasing or constant from one phase to the next.
        // Sort phases by their number to ensure chronological order for comparison.
        List<ContratLeasingPhaseRequestDTO> sortedPhases = requestDTO.getPhases().stream()
                .sorted(Comparator.comparingInt(ContratLeasingPhaseRequestDTO::getNumeroPhase))
                .toList();

        for (int i = 0; i < sortedPhases.size(); i++) {
            if (i > 0) { // Start comparison from the second phase
                ContratLeasingPhaseRequestDTO currentPhase = sortedPhases.get(i);
                ContratLeasingPhaseRequestDTO previousPhase = sortedPhases.get(i - 1);

                // Check if the current phase's monthly payment is less than the previous phase's.
                // If so, it violates the progressive rule.
                if (currentPhase.getMensualiteTTC().compareTo(previousPhase.getMensualiteTTC()) < 0) {
                    throw new ValidationException(String.format(
                            "Pour un contrat PROGRESSIF, la mensualité de la phase %d (%.2f) doit être supérieure ou égale à celle de la phase %d (%.2f).",
                            currentPhase.getNumeroPhase(),
                            currentPhase.getMensualiteTTC(),
                            previousPhase.getNumeroPhase(),
                            previousPhase.getMensualiteTTC()
                    ));
                }
            }
        }
    }

    /**
     * Validates rules specific to **DEGRESSIF** contracts.
     * Key characteristics: multiple phases, now no total cost matching check, decreasing/constant monthly payments.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If business rules specific to DEGRESSIF contracts are violated.
     */
    private void validateDegressifContrat(ContratLeasingRequestDTO requestDTO) {
        // First, apply validations common to all phased contracts.
        validatePhasedContrat(requestDTO);

        // Rule: For DEGRESSIF contracts, monthly payments (mensualités) must be decreasing or constant from one phase to the next.
        // Sort phases by their number to ensure chronological order for comparison.
        List<ContratLeasingPhaseRequestDTO> sortedPhases = requestDTO.getPhases().stream()
                .sorted(Comparator.comparingInt(ContratLeasingPhaseRequestDTO::getNumeroPhase))
                .toList();

        for (int i = 0; i < sortedPhases.size(); i++) {
            if (i > 0) { // Start comparison from the second phase
                ContratLeasingPhaseRequestDTO currentPhase = sortedPhases.get(i);
                ContratLeasingPhaseRequestDTO previousPhase = sortedPhases.get(i - 1);

                // Check if the current phase's monthly payment is greater than the previous phase's.
                // If so, it violates the degressive rule.
                if (currentPhase.getMensualiteTTC().compareTo(previousPhase.getMensualiteTTC()) > 0) {
                    throw new ValidationException(String.format(
                            "Pour un contrat DEGRESSIF, la mensualité de la phase %d (%.2f) doit être inférieure ou égale à celle de la phase %d (%.2f).",
                            currentPhase.getNumeroPhase(),
                            currentPhase.getMensualiteTTC(),
                            previousPhase.getNumeroPhase(),
                            previousPhase.getMensualiteTTC()
                    ));
                }
            }
        }
    }

    /**
     * Performs **common validations applicable to ALL contracts that use phases**
     * (i.e., PROGRESSIF and DEGRESSIF contracts).
     * This method ensures the integrity and consistency of phase data.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If common phased contract rules are violated.
     */
    private void validatePhasedContrat(ContratLeasingRequestDTO requestDTO) {
        // Rule 1: Phased contracts must define at least one phase.
        if (requestDTO.getPhases() == null || requestDTO.getPhases().isEmpty()) {
            throw new ValidationException("Un contrat de type " + requestDTO.getTypeContrat().name() + " doit définir des phases.");
        }

        AtomicInteger totalPhaseDuration = new AtomicInteger(0);
        Set<Integer> phaseNumbers = new HashSet<>(); // Used to track unique phase numbers

        List<ContratLeasingPhaseRequestDTO> phases = requestDTO.getPhases();

        // Iterate over each phase to perform individual and cumulative validations.
        for (ContratLeasingPhaseRequestDTO phase : phases) {
            // Rule 2: Each phase must have a positive phase number.
            if (phase.getNumeroPhase() == null || phase.getNumeroPhase() <= 0) {
                throw new ValidationException("Le numéro de phase doit être un entier positif.");
            }
            // Rule 3: Phase numbers must be unique within a contract.
            if (!phaseNumbers.add(phase.getNumeroPhase())) {
                throw new ValidationException("Les numéros de phase doivent être uniques par contrat.");
            }
            // Rule 4: The duration of each phase must be positive.
            if (phase.getDureeMois() == null || phase.getDureeMois() <= 0) {
                throw new ValidationException("La durée de la phase doit être un nombre de mois positif.");
            }
            // Rule 5: The monthly payment of each phase must be positive.
            if (phase.getMensualiteTTC() == null || phase.getMensualiteTTC().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Le montant de la mensualité TTC de la phase doit être un montant positif.");
            }

            // Accumulate total duration of all phases.
            totalPhaseDuration.addAndGet(phase.getDureeMois());

        }

        // Rule 6: The sum of all phase durations must match the total contract period.
        if (!Integer.valueOf(totalPhaseDuration.get()).equals(requestDTO.getPeriodeEnMois())) {
            throw new ValidationException(String.format("La somme des durées des phases (%d mois) ne correspond pas à la période totale du contrat (%d mois).", totalPhaseDuration.get(), requestDTO.getPeriodeEnMois()));
        }



        // Rule 7: Phase numbers must be sequential and start from 1 (e.g., 1, 2, 3...).
        // Sort phase numbers to easily check sequential order.
        List<Integer> sortedPhaseNumbers = phases.stream()
                .map(ContratLeasingPhaseRequestDTO::getNumeroPhase)
                .sorted()
                .toList();

        for (int i = 0; i < sortedPhaseNumbers.size(); i++) {
            if (sortedPhaseNumbers.get(i) != (i + 1)) {
                throw new ValidationException("Les numéros de phase doivent être séquentiels et commencer par 1 (e.g., 1, 2, 3...).");
            }
        }
    }

    /**
     * Calculates and sets the residual value on the ContratLeasing entity.
     * As per new requirements, for ALL contract types, it's 1% of the MontantAchatTTC.
     *
     * @param contratLeasing The ContratLeasing entity to update.
     * @param montantAchatTTC The total purchase amount, used for calculation.
     */
    private void setValeurResiduelleBasedOnType(ContratLeasing contratLeasing, BigDecimal montantAchatTTC) {

        BigDecimal valeurResiduelle = montantAchatTTC.multiply(RESIDUAL_VALUE_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        contratLeasing.setValeurResiduelle(valeurResiduelle);
    }
}