package staport.finance.gBanque.leasing.contratLeasing.service.imp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingServiceImpl.class);
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
    @Transactional
    public ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) throws ValidationException {
        log.info("Début de la création d'un contrat de leasing dans la couche service");
        log.debug("Type de contrat : {}, Numéro : {}, Montant : {}", requestDTO.getTypeContrat(), requestDTO.getNumeroContrat(), requestDTO.getMontantAchatTTC());

        // 1. Perform  type-specific validations
        log.debug("Validation des règles métier pour le type de contrat : {}", requestDTO.getTypeContrat());
        validateTypeSpecificContratLeasingRequest(requestDTO);

        // 2. Retrieve related entities (Banque and OrganismeLeasing)
        log.debug("Récupération de la banque ID : {}", requestDTO.getBanqueDePrelevementId());
        Banque banque = banqueRepository.findById(requestDTO.getBanqueDePrelevementId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID : {}", requestDTO.getBanqueDePrelevementId());
                    return new ResourceNotFoundException("Banque with ID " + requestDTO.getBanqueDePrelevementId() + " not found.");
                });

        log.debug("Récupération de l'organisme de leasing ID : {}", requestDTO.getOrganismeLeasingId());
        OrganismeLeasing organismeLeasing = organismeLeasingRepository.findById(requestDTO.getOrganismeLeasingId())
                .orElseThrow(() -> {
                    log.error("Organisme de leasing non trouvé avec l'ID : {}", requestDTO.getOrganismeLeasingId());
                    return new ResourceNotFoundException("OrganismeLeasing with ID " + requestDTO.getOrganismeLeasingId() + " not found.");
                });

        // 3. Map the DTO to the ContratLeasing entity
        log.debug("Conversion du DTO vers l'entité ContratLeasing");
        ContratLeasing contratLeasing = contratLeasingMapper.toEntity(requestDTO, banque, organismeLeasing);

        // 4. Calculate and set the Residual Value
        log.debug("Calcul de la valeur résiduelle pour le montant : {}", requestDTO.getMontantAchatTTC());
        setValeurResiduelleBasedOnType(contratLeasing, requestDTO.getMontantAchatTTC());

        // 5. Save the new contract to the database
        log.debug("Sauvegarde du contrat de leasing en base de données");
        ContratLeasing savedContrat = contratLeasingRepository.save(contratLeasing);

        log.info("Contrat de leasing sauvegardé avec succès avec l'ID : {}", savedContrat.getId());

        // 6. Générer les échéances pour le contrat nouvellement créé
        log.debug("Génération des échéances pour le contrat ID : {}", savedContrat.getId());
        echeanceLeasingService.generateEcheances(savedContrat);

        log.info("Création du contrat de leasing terminée avec succès pour l'ID : {}", savedContrat.getId());

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
    @Transactional(readOnly = true)
    public ContratLeasingResponseDTO getContratLeasingById(Long id) throws ResourceNotFoundException {
        log.info("Récupération du contrat de leasing avec l'ID : {} dans la couche service", id);

        ContratLeasingResponseDTO dto = contratLeasingRepository.findById(id)
                .map(contratLeasingMapper::toDto)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", id);
                    return new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
                });

        log.debug("Contrat trouvé : {}", dto.getNumeroContrat());
        return dto;
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
    @Transactional
    public ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) throws ResourceNotFoundException, ValidationException {
        log.info("Début de la mise à jour du contrat de leasing avec l'ID : {} dans la couche service", id);
        log.debug("Nouvelles données : Type={}, Numéro={}", requestDTO.getTypeContrat(), requestDTO.getNumeroContrat());

        // 1. Retrieve the existing contract entity
        ContratLeasing existingContrat = contratLeasingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", id);
                    return new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
                });

        log.debug("Contrat existant trouvé : {}", existingContrat.getNumeroContrat());

        // 2. Perform common business validations and type-specific validations
        log.debug("Validation des règles métier pour le type : {}", requestDTO.getTypeContrat());
        validateTypeSpecificContratLeasingRequest(requestDTO);

        // 3. Re-fetch related entities
        log.debug("Récupération de la banque ID : {}", requestDTO.getBanqueDePrelevementId());
        Banque banque = banqueRepository.findById(requestDTO.getBanqueDePrelevementId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID : {}", requestDTO.getBanqueDePrelevementId());
                    return new ResourceNotFoundException("Banque with ID " + requestDTO.getBanqueDePrelevementId() + " not found.");
                });

        log.debug("Récupération de l'organisme de leasing ID : {}", requestDTO.getOrganismeLeasingId());
        OrganismeLeasing organismeLeasing = organismeLeasingRepository.findById(requestDTO.getOrganismeLeasingId())
                .orElseThrow(() -> {
                    log.error("Organisme de leasing non trouvé avec l'ID : {}", requestDTO.getOrganismeLeasingId());
                    return new ResourceNotFoundException("OrganismeLeasing with ID " + requestDTO.getOrganismeLeasingId() + " not found.");
                });

        // 4. Update the existing entity using the mapper
        log.debug("Mise à jour de l'entité existante avec les nouvelles données");
        contratLeasingMapper.updateEntityFromDto(requestDTO, existingContrat, banque, organismeLeasing);

        // 5. Calculate and set the Residual Value
        log.debug("Recalcul de la valeur résiduelle");
        setValeurResiduelleBasedOnType(existingContrat, existingContrat.getMontantAchatTTC());

        // 6. Save the updated contract
        log.debug("Sauvegarde du contrat mis à jour");
        ContratLeasing updatedContrat = contratLeasingRepository.save(existingContrat);

        log.debug("Régénération des échéances pour le contrat ID : {}", updatedContrat.getId());
        this.echeanceLeasingService.regenerateEcheances(updatedContrat.getId());

        log.info("Mise à jour du contrat de leasing terminée avec succès pour l'ID : {}", id);

        // 7. Map the updated entity to a response DTO and return it
        return contratLeasingMapper.toDto(updatedContrat);
    }
    /**
     * Deletes a leasing contract by its ID.
     *
     * @param id The ID of the contract to delete.
     * @throws ResourceNotFoundException If no contract exists with the given ID.
     */
    /**
     * Supprime un contrat de leasing ainsi que toutes ses données associées (échéances et commissions).
     * La suppression est bloquée si le contrat contient des échéances déjà payées.
     *
     * @param id L'ID du contrat à supprimer
     * @throws ResourceNotFoundException Si le contrat n'existe pas
     * @throws ValidationException Si le contrat contient des échéances payées
     */
    @Override
    @Transactional
    public void deleteContratLeasing(Long id) throws ResourceNotFoundException, ValidationException {
        log.info("Début de la suppression du contrat de leasing avec l'ID : {} dans la couche service", id);

        // 1. Récupérer le contrat
        ContratLeasing contrat = contratLeasingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", id);
                    return new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
                });

        log.debug("Contrat trouvé : {}", contrat.getNumeroContrat());

        // 2. Vérifier s'il y a des échéances payées (protection métier)
        boolean hasPaidEcheances = echeanceLeasingService.hasPaidEcheances(id);
        if (hasPaidEcheances) {
            log.error("Tentative de suppression d'un contrat avec échéances payées : ID {}", id);
            throw new ValidationException(
                    "Impossible de supprimer le contrat avec l'ID " + id +
                            " car il contient des échéances déjà payées. " +
                            "Veuillez le marquer comme inactif au lieu de le supprimer."
            );
        }

        // 3. Supprimer les échéances et commissions
        log.debug("Suppression des échéances et commissions pour le contrat ID : {}", id);
        echeanceLeasingService.deleteEcheancesAndCommissionByContrat(contrat);

        // 4. Supprimer le contrat
        log.debug("Suppression du contrat ID : {}", id);
        contratLeasingRepository.deleteById(id);

        log.info("Contrat de leasing et toutes les données associées supprimés avec succès pour l'ID : {}", id);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        log.info("Récupération des contrats inactifs dans la couche service");
        log.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Specification<ContratLeasing> statusSpec = ContratLeasingSpecification.getInactiveContrats();
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.bySearchCriteria(searchDTO);

        Page<ContratLeasingResponseDTO> result = contratLeasingRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(contratLeasingMapper::toDto);

        log.info("Nombre de contrats inactifs trouvés : {} sur {} au total", result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        log.info("Récupération des contrats en attente de main levée dans la couche service");
        log.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        Specification<ContratLeasing> statusSpec = ContratLeasingSpecification.getPendingLienReleaseContrats();
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.bySearchCriteria(searchDTO);

        Page<ContratLeasingResponseDTO> result = contratLeasingRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(contratLeasingMapper::toDto);

        log.info("Nombre de contrats en attente de main levée trouvés : {} sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        log.info("Récupération des contrats actifs dans la couche service");
        log.debug("Critères de recherche : {}, Page : {}, Taille : {}", searchDTO, pageable.getPageNumber(), pageable.getPageSize());

        // Crée la spécification de statut (e.g., actif)
        Specification<ContratLeasing> statusSpec = ContratLeasingSpecification.getActiveContrats();

        // Crée la spécification de recherche dynamique (basée sur le DTO)
        Specification<ContratLeasing> searchSpec = ContratLeasingSpecification.bySearchCriteria(searchDTO);

        // Combine les deux spécifications avec un "AND"
        Page<ContratLeasingResponseDTO> result = contratLeasingRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(contratLeasingMapper::toDto);

        log.info("Nombre de contrats actifs trouvés : {} sur {} au total", result.getNumberOfElements(), result.getTotalElements());

        return result;
    }

    @Override
    @Transactional
    public ContratLeasingResponseDTO markAsInactive(Long id) throws ResourceNotFoundException, ValidationException {
        log.info("Début du marquage du contrat ID : {} comme inactif dans la couche service", id);

        // 1. Récupérer le contrat existant
        ContratLeasing contrat = contratLeasingRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", id);
                    return new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
                });

        log.debug("Contrat trouvé : {}, Statut actuel : completed={}", contrat.getNumeroContrat(), contrat.isCompleted());

        // 2. Vérifier si le contrat est déjà terminé
        if (contrat.isCompleted()) {
            log.error("Le contrat ID : {} est déjà marqué comme inactif", id);
            throw new ValidationException("Le contrat avec l'ID " + id + " est déjà inactif.");
        }

        // 3. Vérifier s'il reste des échéances avec le statut PREVU
        boolean hasPendingInstallments = contrat.getEcheances().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceLeasing.PREVU);

        if (hasPendingInstallments) {
            log.error("Le contrat ID : {} contient encore des échéances avec le statut PREVU", id);
            throw new ValidationException("Le contrat ne peut être marqué comme inactif que si toutes les échéances ont été payées.");
        }

        // 4. Marquer le contrat comme terminé
        log.debug("Marquage du contrat ID : {} comme terminé", id);
        contrat.setCompleted(true);
        ContratLeasing updatedContrat = contratLeasingRepository.save(contrat);

        log.info("Contrat ID : {} marqué comme inactif avec succès", id);

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
        log.debug("Validation spécifique au type de contrat : {}", requestDTO.getTypeContrat());

        // Rule: The contract type must be specified.
        if (requestDTO.getTypeContrat() == null) {
            log.error("Type de contrat manquant dans la requête");
            throw new ValidationException("Le type de contrat est obligatoire.");
        }

        // Delegates to the appropriate validation method based on the contract type.
        switch (requestDTO.getTypeContrat()) {
            case LINEAIRE:
                log.debug("Validation des règles pour un contrat LINEAIRE");
                validateLineaireContrat(requestDTO);
                break;
            case PROGRESSIF:
                log.debug("Validation des règles pour un contrat PROGRESSIF");
                validateProgressifContrat(requestDTO);
                break;
            case DEGRESSIF:
                log.debug("Validation des règles pour un contrat DEGRESSIF");
                validateDegressifContrat(requestDTO);
                break;
            default:
                log.error("Type de contrat inconnu : {}", requestDTO.getTypeContrat());
                throw new ValidationException("Type de contrat inconnu: " + requestDTO.getTypeContrat());
        }

        log.debug("Validation du type de contrat réussie");
    }
    /**
     * Validates rules specific to **LINEAIRE** contracts.
     * Key characteristics: exactly one phase, now no financial coherence check based on payments.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If business rules specific to LINEAIRE contracts are violated.
     */
    private void validateLineaireContrat(ContratLeasingRequestDTO requestDTO) {
        log.debug("Validation des règles spécifiques au contrat LINEAIRE");

        // Rule 1: LINEAIRE contracts must have **exactly one single phase**.
        if (requestDTO.getPhases() == null || requestDTO.getPhases().size() != 1) {
            log.error("Contrat LINEAIRE avec un nombre de phases incorrect : {}",
                    requestDTO.getPhases() != null ? requestDTO.getPhases().size() : 0);
            throw new ValidationException("Un contrat de type LINEAIRE doit avoir exactement une seule phase.");
        }

        // Retrieve the single phase for validation.
        ContratLeasingPhaseRequestDTO singlePhase = requestDTO.getPhases().get(0);

        // Rule 2: The single phase must have `numeroPhase` 1.
        if (singlePhase.getNumeroPhase() == null || singlePhase.getNumeroPhase() != 1) {
            log.error("Phase unique du contrat LINEAIRE avec un numéro incorrect : {}", singlePhase.getNumeroPhase());
            throw new ValidationException("Pour un contrat LINEAIRE, la phase unique doit avoir le numéro 1.");
        }

        // Rule 3: The duration of the single phase must match the total contract period.
        if (!singlePhase.getDureeMois().equals(requestDTO.getPeriodeEnMois())) {
            log.error("Durée de phase ({}) ne correspond pas à la période du contrat ({})",
                    singlePhase.getDureeMois(), requestDTO.getPeriodeEnMois());
            throw new ValidationException(String.format(
                    "Pour un contrat LINEAIRE, la durée de la phase unique (%d mois) doit correspondre à la période totale du contrat (%d mois).",
                    singlePhase.getDureeMois(), requestDTO.getPeriodeEnMois()
            ));
        }

        log.debug("Validation du contrat LINEAIRE réussie");
    }
    /**
     * Validates rules specific to **PROGRESSIF** contracts.
     * Key characteristics: multiple phases, now no total cost matching check, increasing/constant monthly payments.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If business rules specific to PROGRESSIF contracts are violated.
     */
    private void validateProgressifContrat(ContratLeasingRequestDTO requestDTO) {
        log.debug("Validation des règles spécifiques au contrat PROGRESSIF");

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
                    log.error("Mensualité de la phase {} ({}) inférieure à la phase {} ({})",
                            currentPhase.getNumeroPhase(), currentPhase.getMensualiteTTC(),
                            previousPhase.getNumeroPhase(), previousPhase.getMensualiteTTC());
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

        log.debug("Validation du contrat PROGRESSIF réussie");
    }
    /**
     * Validates rules specific to **DEGRESSIF** contracts.
     * Key characteristics: multiple phases, now no total cost matching check, decreasing/constant monthly payments.
     *
     * @param requestDTO The DTO containing the contract data.
     * @throws ValidationException If business rules specific to DEGRESSIF contracts are violated.
     */
    private void validateDegressifContrat(ContratLeasingRequestDTO requestDTO) {
        log.debug("Validation des règles spécifiques au contrat DEGRESSIF");

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
                    log.error("Mensualité de la phase {} ({}) supérieure à la phase {} ({})",
                            currentPhase.getNumeroPhase(), currentPhase.getMensualiteTTC(),
                            previousPhase.getNumeroPhase(), previousPhase.getMensualiteTTC());
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

        log.debug("Validation du contrat DEGRESSIF réussie");
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
        log.debug("Validation des règles communes pour les contrats à phases");

        // Rule 1: Phased contracts must define at least one phase.
        if (requestDTO.getPhases() == null || requestDTO.getPhases().isEmpty()) {
            log.error("Aucune phase définie pour le contrat de type {}", requestDTO.getTypeContrat());
            throw new ValidationException("Un contrat de type " + requestDTO.getTypeContrat().name() + " doit définir des phases.");
        }

        log.debug("Nombre de phases à valider : {}", requestDTO.getPhases().size());

        AtomicInteger totalPhaseDuration = new AtomicInteger(0);
        Set<Integer> phaseNumbers = new HashSet<>(); // Used to track unique phase numbers

        List<ContratLeasingPhaseRequestDTO> phases = requestDTO.getPhases();

        // Iterate over each phase to perform individual and cumulative validations.
        for (ContratLeasingPhaseRequestDTO phase : phases) {

            // Rule 2: Phase numbers must be unique within a contract.
            if (!phaseNumbers.add(phase.getNumeroPhase())) {
                log.error("Numéro de phase dupliqué : {}", phase.getNumeroPhase());
                throw new ValidationException("Les numéros de phase doivent être uniques par contrat.");
            }

            // Accumulate total duration of all phases.
            totalPhaseDuration.addAndGet(phase.getDureeMois());

        }

        log.debug("Durée totale des phases : {} mois, Période du contrat : {} mois",
                totalPhaseDuration.get(), requestDTO.getPeriodeEnMois());

        // Rule 3: The sum of all phase durations must match the total contract period.
        if (!Integer.valueOf(totalPhaseDuration.get()).equals(requestDTO.getPeriodeEnMois())) {
            log.error("Somme des durées des phases ({}) ne correspond pas à la période du contrat ({})",
                    totalPhaseDuration.get(), requestDTO.getPeriodeEnMois());
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
                log.error("Numéros de phase non séquentiels détectés");
                throw new ValidationException("Les numéros de phase doivent être séquentiels et commencer par 1 (e.g., 1, 2, 3...).");
            }
        }

        log.debug("Validation des phases communes réussie");
    }
    /**
     * Calculates and sets the residual value on the ContratLeasing entity.
     * As per new requirements, for ALL contract types, it's 1% of the MontantAchatTTC.
     *
     * @param contratLeasing The ContratLeasing entity to update.
     * @param montantAchatTTC The total purchase amount, used for calculation.
     */
    private void setValeurResiduelleBasedOnType(ContratLeasing contratLeasing, BigDecimal montantAchatTTC) {
        log.debug("Calcul de la valeur résiduelle pour le montant TTC : {}", montantAchatTTC);

        BigDecimal valeurResiduelle = montantAchatTTC.multiply(leasingProperties.getValeurResiduelle()).setScale(2, RoundingMode.HALF_UP);
        contratLeasing.setValeurResiduelle(valeurResiduelle);

        log.debug("Valeur résiduelle calculée : {}", valeurResiduelle);
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
        log.info("Début de l'attachement/remplacement du fichier PDF pour le contrat ID : {}", contratId);
        log.debug("Nom du fichier : {}, Taille : {} octets", fileName, pdfContent != null ? pdfContent.length : 0);

        // 1. Vérifier que le contrat existe
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", contratId);
                    return new ResourceNotFoundException("ContratLeasing with ID " + contratId + " not found.");
                });

        // 2. Valider que le fichier est un PDF
        if (pdfContent == null || pdfContent.length == 0) {
            log.error("Tentative d'upload d'un fichier PDF vide pour le contrat ID : {}", contratId);
            throw new ValidationException("Le contenu du fichier PDF est vide.");
        }

        if (fileName == null || !fileName.toLowerCase().endsWith(".pdf")) {
            log.error("Extension de fichier invalide : {}", fileName);
            throw new ValidationException("Seuls les fichiers PDF sont acceptés.");
        }

        // 4. Attacher ou remplacer le fichier PDF
        log.debug("Attachement du fichier PDF au contrat ID : {}", contratId);
        contrat.setFichierPdfContenu(pdfContent);
        contrat.setNomFichierPdf(fileName);

        // 5. Sauvegarder
        contratLeasingRepository.save(contrat);

        log.info("Fichier PDF attaché avec succès au contrat ID : {}", contratId);
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
        log.info("Récupération du fichier PDF pour le contrat ID : {}", contratId);

        // 1. Récupérer le contrat
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", contratId);
                    return new ResourceNotFoundException("ContratLeasing with ID " + contratId + " not found.");
                });

        // 2. Vérifier qu'un PDF est attaché
        if (!contrat.hasFichierPdf()) {
            log.error("Aucun fichier PDF attaché au contrat ID : {}", contratId);
            throw new ResourceNotFoundException("Aucun fichier PDF n'est attaché au contrat avec l'ID " + contratId + ".");
        }

        log.debug("Fichier PDF trouvé : {}, Taille : {} octets",
                contrat.getNomFichierPdf(), contrat.getTailleFichierPdf());

        // 3. Créer et retourner le DTO
        PdfFileDTO pdfFileDTO = new PdfFileDTO();
        pdfFileDTO.setContenu(contrat.getFichierPdfContenu());
        pdfFileDTO.setNomFichier(contrat.getNomFichierPdf());
        pdfFileDTO.setTailleFichier(contrat.getTailleFichierPdf());
        pdfFileDTO.setDateUpload(contrat.getDateUploadPdf());

        log.info("Fichier PDF récupéré avec succès pour le contrat ID : {}", contratId);
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
        log.info("Début de la suppression du fichier PDF pour le contrat ID : {}", contratId);

        // 1. Récupérer le contrat
        ContratLeasing contrat = contratLeasingRepository.findById(contratId)
                .orElseThrow(() -> {
                    log.error("Contrat de leasing non trouvé avec l'ID : {}", contratId);
                    return new ResourceNotFoundException("ContratLeasing with ID " + contratId + " not found.");
                });

        // 2. Supprimer le PDF
        log.debug("Suppression du fichier PDF du contrat ID : {}", contratId);
        contrat.removeFichierPdf();

        // 3. Sauvegarder
        contratLeasingRepository.save(contrat);

        log.info("Fichier PDF supprimé avec succès pour le contrat ID : {}", contratId);
    }

}