package staport.finance.gBanque.leasing.contratLeasing.service.imp;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.config.LeasingProperties;
import staport.finance.gBanque.leasing.contratLeasing.dto.*;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.mapper.ContratLeasingMapper;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.contratLeasing.specification.ContratLeasingSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.services.EcheanceLeasingService;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.entities.OrganismeLeasing;
import staport.finance.gBanque.leasing.lesOrganismsLeasing.repositories.OrganismeLeasingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
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
    private final LeasingProperties leasingProperties;

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
                                     OrganismeLeasingRepository organismeLeasingRepository, LeasingProperties leasingProperties) {
        this.contratLeasingRepository = contratLeasingRepository;
        this.contratLeasingMapper = contratLeasingMapper;
        this.banqueRepository = banqueRepository;
        this.organismeLeasingRepository = organismeLeasingRepository;
        this.echeanceLeasingService = echeanceLeasingService;
        this.leasingProperties = leasingProperties;
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

        // 1. Perform  type-specific validations
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

        // 5. Save the new contract to the database
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

        // 6. Save the updated contract to the database
        ContratLeasing updatedContrat = contratLeasingRepository.save(existingContrat);

        this.echeanceLeasingService.regenerateEcheances(updatedContrat.getId());
        // 7. Map the updated entity to a response DTO and return it
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


    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> statusSpec = ContratLeasingSpecification.getInactiveContrats();
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.bySearchCriteria(searchDTO);

        return contratLeasingRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> statusSpec = ContratLeasingSpecification.getPendingLienReleaseContrats();
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.bySearchCriteria(searchDTO);

        return contratLeasingRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        // Crée la spécification de statut (e.g., actif)
        Specification<ContratLeasing> statusSpec = ContratLeasingSpecification.getActiveContrats();

        // Crée la spécification de recherche dynamique (basée sur le DTO)
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.bySearchCriteria(searchDTO);

        // Combine les deux spécifications avec un "AND"
        return contratLeasingRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional
    public ContratLeasingResponseDTO markAsInactive(Long id) throws ResourceNotFoundException, ValidationException {

        // 1. Récupérer le contrat existant
        ContratLeasing contrat = contratLeasingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContratLeasing with ID " + id + " not found."));

        // 2. Vérifier si le contrat est déjà terminé
        if (contrat.isCompleted()) {
            throw new ValidationException("Le contrat avec l'ID " + id + " est déjà inactif.");
        }

        // 3. Vérifier s'il reste des échéances avec le statut PREVU
        boolean hasPendingInstallments = contrat.getEcheances().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceLeasing.PREVU);

        if (hasPendingInstallments) {
            throw new ValidationException("Le contrat ne peut être marqué comme inactif que si toutes les échéances ont été payées.");
        }

        // 4. Marquer le contrat comme terminé
        contrat.setCompleted(true);
        ContratLeasing updatedContrat = contratLeasingRepository.save(contrat);

        // 5. Retourner le DTO mis à jour
        return contratLeasingMapper.toDto(updatedContrat);
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

        // Rule 3: The duration of the single phase must match the total contract period.
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

            // Rule 2: Phase numbers must be unique within a contract.
            if (!phaseNumbers.add(phase.getNumeroPhase())) {
                throw new ValidationException("Les numéros de phase doivent être uniques par contrat.");
            }

            // Accumulate total duration of all phases.
            totalPhaseDuration.addAndGet(phase.getDureeMois());

        }

        // Rule 3: The sum of all phase durations must match the total contract period.
        if (!Integer.valueOf(totalPhaseDuration.get()).equals(requestDTO.getPeriodeEnMois())) {
            throw new ValidationException(String.format("La somme des durées des phases (%d mois) ne correspond pas à la période totale du contrat (%d mois).", totalPhaseDuration.get(), requestDTO.getPeriodeEnMois()));
        }

        // Rule 4: Phase numbers must be sequential and start from 1 (e.g., 1, 2, 3...).
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

        BigDecimal valeurResiduelle = montantAchatTTC.multiply(leasingProperties.getValeurResiduelle()).setScale(2, RoundingMode.HALF_UP);
        contratLeasing.setValeurResiduelle(valeurResiduelle);
    }

    /**
     * Associe ou remplace un fichier PDF à un contrat de leasing.
     *
     * @param contratId L'ID du contrat
     * @param pdfContent Le contenu binaire du fichier PDF
     * @param fileName Le nom du fichier PDF
     * @throws ResourceNotFoundException Si le contrat n'existe pas
     * @throws ValidationException Si le fichier n'est pas un PDF valide
     */
    @Override
    @Transactional
    public void attachOrReplacePdfToContrat(Long contratId, byte[] pdfContent, String fileName)
            throws ResourceNotFoundException, ValidationException {

        // 1. Vérifier que le contrat existe
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ContratLeasing with ID " + contratId + " not found."));

        // 2. Valider que le fichier est un PDF
        if (pdfContent == null || pdfContent.length == 0) {
            throw new ValidationException("Le contenu du fichier PDF est vide.");
        }

        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Seuls les fichiers PDF sont acceptés.");
        }


        // 4. Attacher ou remplacer le fichier PDF
        contrat.setFichierPdfContenu(pdfContent);
        contrat.setNomFichierPdf(fileName);

        // 5. Sauvegarder
        contratLeasingRepository.save(contrat);
    }

    /**
     * Récupère le contenu du fichier PDF d'un contrat de leasing.
     *
     * @param contratId L'ID du contrat
     * @return Un objet PdfFileDTO contenant le contenu et les métadonnées du PDF
     * @throws ResourceNotFoundException Si le contrat n'existe pas ou n'a pas de PDF attaché
     */
    @Override
    @Transactional(readOnly = true)
    public PdfFileDTO getPdfFromContrat(Long contratId) throws ResourceNotFoundException {

        // 1. Récupérer le contrat
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ContratLeasing with ID " + contratId + " not found."));

        // 2. Vérifier qu'un PDF est attaché
        if (!contrat.hasFichierPdf()) {
            throw new ResourceNotFoundException(
                    "Aucun fichier PDF n'est attaché au contrat avec l'ID " + contratId + ".");
        }

        // 3. Créer et retourner le DTO
        PdfFileDTO pdfFileDTO = new PdfFileDTO();
        pdfFileDTO.setContenu(contrat.getFichierPdfContenu());
        pdfFileDTO.setNomFichier(contrat.getNomFichierPdf());
        pdfFileDTO.setTailleFichier(contrat.getTailleFichierPdf());
        pdfFileDTO.setDateUpload(contrat.getDateUploadPdf());

        return pdfFileDTO;
    }

    /**
     * Supprime le fichier PDF d'un contrat de leasing.
     *
     * @param contratId L'ID du contrat
     * @throws ResourceNotFoundException Si le contrat n'existe pas
     */
    @Override
    @Transactional
    public void removePdfFromContrat(Long contratId) throws ResourceNotFoundException {

        // 1. Récupérer le contrat
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ContratLeasing with ID " + contratId + " not found."));

        // 2. Supprimer le PDF
        contrat.removeFichierPdf();

        // 3. Sauvegarder
        contratLeasingRepository.save(contrat);
    }

    /**
     * Vérifie si le contenu binaire est un fichier PDF valide
     * en vérifiant les "magic bytes" (signature du fichier).
     *
     * @param content Le contenu binaire du fichier
     * @return true si c'est un PDF valide, false sinon
     */
    private boolean isPdfFile(byte[] content) {
        // Les fichiers PDF commencent par "%PDF-" (25 50 44 46 2D en hexadécimal)
        if (content == null || content.length < 5) {
            return false;
        }

        return content[0] == 0x25 && // %
                content[1] == 0x50 && // P
                content[2] == 0x44 && // D
                content[3] == 0x46 && // F
                content[4] == 0x2D;   // -
    }
}