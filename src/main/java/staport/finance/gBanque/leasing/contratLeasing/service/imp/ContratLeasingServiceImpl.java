package staport.finance.gBanque.leasing.contratLeasing.service.imp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.leasing.contratLeasing.config.LeasingProperties;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingPhaseRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingRequestDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingResponseDTO;
import staport.finance.gBanque.leasing.contratLeasing.dto.ContratLeasingSearchDTO;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.contratLeasing.mapper.ContratLeasingMapper;
import staport.finance.gBanque.leasing.contratLeasing.repositories.ContratLeasingRepository;
import staport.finance.gBanque.leasing.contratLeasing.service.ContratLeasingService;
import staport.finance.gBanque.leasing.contratLeasing.specification.ContratLeasingSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.StatutEcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces.IEcheanceLeasingService;
import staport.finance.gBanque.leasing.contratCadre.entities.ContratCadre;
import staport.finance.gBanque.leasing.contratCadre.services.IContratCadreService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ContratLeasingServiceImpl implements ContratLeasingService {

    private static final Logger log = LoggerFactory.getLogger(ContratLeasingServiceImpl.class);

    private final ContratLeasingRepository contratLeasingRepository;
    private final ContratLeasingMapper contratLeasingMapper;
    private final IBanqueService banqueService;
    private final IContratCadreService contratCadreService;
    private final IEcheanceLeasingService echeanceLeasingService;
    private final LeasingProperties leasingProperties;

    public ContratLeasingServiceImpl(IEcheanceLeasingService echeanceLeasingService,
                                     ContratLeasingRepository contratLeasingRepository,
                                     ContratLeasingMapper contratLeasingMapper,
                                     IBanqueService banqueService,
                                     IContratCadreService contratCadreService,
                                     LeasingProperties leasingProperties) {
        this.contratLeasingRepository = contratLeasingRepository;
        this.contratLeasingMapper = contratLeasingMapper;
        this.banqueService = banqueService;
        this.contratCadreService = contratCadreService;
        this.echeanceLeasingService = echeanceLeasingService;
        this.leasingProperties = leasingProperties;
    }

    // ── Méthode interne centralisée ──────────────────────────────────────────

    private ContratLeasing findContratOrThrow(Long id) {
        return contratLeasingRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("ContratLeasing non trouvé — ID : {}", id);
                    return new ResourceNotFoundException("ContratLeasing with ID " + id + " not found.");
                });
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = ContratLeasing.class)
    public ContratLeasingResponseDTO createContratLeasing(ContratLeasingRequestDTO requestDTO) {
        log.info("Création d'un contrat de leasing — type : {}, numéro : {}",
                requestDTO.typeContrat(), requestDTO.numeroContrat());

        validateTypeSpecificContratLeasingRequest(requestDTO);

        Banque banque = banqueService.findOrThrow(requestDTO.banqueDePrelevementId());
        ContratCadre contratCadre = contratCadreService.resolveActif(requestDTO.organismeLeasingId());

        ContratLeasing contratLeasing = contratLeasingMapper.toEntity(requestDTO, banque);
        contratLeasing.setContratCadre(contratCadre);
        setValeurResiduelleBasedOnType(contratLeasing, requestDTO.montantAchatTTC());

        ContratLeasing savedContrat = contratLeasingRepository.save(contratLeasing);
        log.info("Contrat de leasing créé — ID : {}", savedContrat.getId());

        echeanceLeasingService.generateEcheances(savedContrat);

        return contratLeasingMapper.toDto(savedContrat);
    }

    @Override
    @Transactional(readOnly = true)
    public ContratLeasingResponseDTO getContratLeasingById(Long id) {
        ContratLeasing contrat = findContratOrThrow(id);
        log.debug("Contrat récupéré — numéro : {}", contrat.getNumeroContrat());
        return contratLeasingMapper.toDto(contrat);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratLeasing.class)
    public ContratLeasingResponseDTO updateContratLeasing(Long id, ContratLeasingRequestDTO requestDTO) {
        log.info("Mise à jour du contrat de leasing — ID : {}", id);

        ContratLeasing existingContrat = findContratOrThrow(id);
        validateTypeSpecificContratLeasingRequest(requestDTO);

        Banque banque = banqueService.findOrThrow(requestDTO.banqueDePrelevementId());
        ContratCadre contratCadre = contratCadreService.resolveActif(requestDTO.organismeLeasingId());

        contratLeasingMapper.updateEntityFromDto(requestDTO, existingContrat, banque);
        existingContrat.setContratCadre(contratCadre);
        setValeurResiduelleBasedOnType(existingContrat, existingContrat.getMontantAchatTTC());

        ContratLeasing updatedContrat = contratLeasingRepository.save(existingContrat);
        echeanceLeasingService.regenerateEcheances(updatedContrat.getId());

        log.info("Contrat de leasing mis à jour — ID : {}", id);
        return contratLeasingMapper.toDto(updatedContrat);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = ContratLeasing.class)
    public void deleteContratLeasing(Long id) {
        log.info("Suppression du contrat de leasing — ID : {}", id);

        ContratLeasing contrat = findContratOrThrow(id);

        boolean hasPaidEcheances = echeanceLeasingService.hasPaidEcheances(id);
        if (hasPaidEcheances) {
            log.warn("Suppression bloquée — contrat ID : {} contient des échéances payées", id);
            throw new ValidationException(
                    "Impossible de supprimer le contrat avec l'ID " + id +
                            " car il contient des échéances déjà payées. " +
                            "Veuillez le marquer comme inactif au lieu de le supprimer."
            );
        }

        echeanceLeasingService.deleteEcheancesAndCommissionByContrat(contrat);
        contratLeasingRepository.deleteById(id);

        log.info("Contrat de leasing supprimé — ID : {}", id);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = ContratLeasing.class)
    public ContratLeasingResponseDTO markAsInactive(Long id) {
        log.info("Clôture du contrat de leasing — ID : {}", id);

        ContratLeasing contrat = findContratOrThrow(id);

        if (contrat.isCompleted()) {
            log.warn("Contrat ID : {} est déjà inactif", id);
            throw new ValidationException("Le contrat avec l'ID " + id + " est déjà inactif.");
        }

        boolean hasPendingInstallments = contrat.getEcheances().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceLeasing.PREVU);

        if (hasPendingInstallments) {
            log.warn("Clôture bloquée — contrat ID : {} contient encore des échéances PREVU", id);
            throw new ValidationException("Le contrat ne peut être marqué comme inactif que si toutes les échéances ont été payées.");
        }

        contrat.setCompleted(true);
        ContratLeasing updatedContrat = contratLeasingRepository.save(contrat);

        log.info("Contrat ID : {} marqué comme inactif", id);
        return contratLeasingMapper.toDto(updatedContrat);
    }

    // ── Listes ───────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getInactiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getInactiveContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return contratLeasingRepository.findAll(spec, effectivePageable(pageable)).map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getPendingLienReleaseContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getPendingLienReleaseContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return contratLeasingRepository.findAll(spec, effectivePageable(pageable)).map(contratLeasingMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ContratLeasingResponseDTO> getActiveContrats(ContratLeasingSearchDTO searchDTO, Pageable pageable) {
        Specification<ContratLeasing> spec = Specification
                .where(ContratLeasingSpecification.getActiveContrats())
                .and(ContratLeasingSpecification.searchByCriteria(searchDTO));
        return contratLeasingRepository.findAll(spec, effectivePageable(pageable)).map(contratLeasingMapper::toDto);
    }

    // ── Validation métier ────────────────────────────────────────────────────

    private void validateTypeSpecificContratLeasingRequest(ContratLeasingRequestDTO requestDTO) {
        if (requestDTO.typeContrat() == null) {
            log.warn("Type de contrat manquant dans la requête");
            throw new ValidationException("Le type de contrat est obligatoire.");
        }
        switch (requestDTO.typeContrat()) {
            case LINEAIRE -> validateLineaireContrat(requestDTO);
            case PROGRESSIF -> validateProgressifContrat(requestDTO);
            case DEGRESSIF -> validateDegressifContrat(requestDTO);
            default -> {
                log.warn("Type de contrat inconnu : {}", requestDTO.typeContrat());
                throw new ValidationException("Type de contrat inconnu: " + requestDTO.typeContrat());
            }
        }
    }

    private void validateLineaireContrat(ContratLeasingRequestDTO requestDTO) {
        if (requestDTO.phases() == null || requestDTO.phases().size() != 1) {
            throw new ValidationException("Un contrat de type LINEAIRE doit avoir exactement une seule phase.");
        }
        ContratLeasingPhaseRequestDTO singlePhase = requestDTO.phases().get(0);
        if (singlePhase.numeroPhase() == null || singlePhase.numeroPhase() != 1) {
            throw new ValidationException("Pour un contrat LINEAIRE, la phase unique doit avoir le numéro 1.");
        }
        if (!singlePhase.dureeMois().equals(requestDTO.periodeEnMois())) {
            throw new ValidationException(String.format(
                    "Pour un contrat LINEAIRE, la durée de la phase unique (%d mois) doit correspondre à la période totale du contrat (%d mois).",
                    singlePhase.dureeMois(), requestDTO.periodeEnMois()
            ));
        }
    }

    private void validateProgressifContrat(ContratLeasingRequestDTO requestDTO) {
        validatePhasedContrat(requestDTO);
        List<ContratLeasingPhaseRequestDTO> sortedPhases = requestDTO.phases().stream()
                .sorted(Comparator.comparingInt(ContratLeasingPhaseRequestDTO::numeroPhase))
                .toList();
        for (int i = 1; i < sortedPhases.size(); i++) {
            ContratLeasingPhaseRequestDTO current = sortedPhases.get(i);
            ContratLeasingPhaseRequestDTO previous = sortedPhases.get(i - 1);
            if (current.mensualiteTTC().compareTo(previous.mensualiteTTC()) < 0) {
                throw new ValidationException(String.format(
                        "Pour un contrat PROGRESSIF, la mensualité de la phase %d (%.2f) doit être supérieure ou égale à celle de la phase %d (%.2f).",
                        current.numeroPhase(), current.mensualiteTTC(),
                        previous.numeroPhase(), previous.mensualiteTTC()
                ));
            }
        }
    }

    private void validateDegressifContrat(ContratLeasingRequestDTO requestDTO) {
        validatePhasedContrat(requestDTO);
        List<ContratLeasingPhaseRequestDTO> sortedPhases = requestDTO.phases().stream()
                .sorted(Comparator.comparingInt(ContratLeasingPhaseRequestDTO::numeroPhase))
                .toList();
        for (int i = 1; i < sortedPhases.size(); i++) {
            ContratLeasingPhaseRequestDTO current = sortedPhases.get(i);
            ContratLeasingPhaseRequestDTO previous = sortedPhases.get(i - 1);
            if (current.mensualiteTTC().compareTo(previous.mensualiteTTC()) > 0) {
                throw new ValidationException(String.format(
                        "Pour un contrat DEGRESSIF, la mensualité de la phase %d (%.2f) doit être inférieure ou égale à celle de la phase %d (%.2f).",
                        current.numeroPhase(), current.mensualiteTTC(),
                        previous.numeroPhase(), previous.mensualiteTTC()
                ));
            }
        }
    }

    private void validatePhasedContrat(ContratLeasingRequestDTO requestDTO) {
        if (requestDTO.phases() == null || requestDTO.phases().isEmpty()) {
            throw new ValidationException("Un contrat de type " + requestDTO.typeContrat().name() + " doit définir des phases.");
        }

        AtomicInteger totalPhaseDuration = new AtomicInteger(0);
        Set<Integer> phaseNumbers = new HashSet<>();

        for (ContratLeasingPhaseRequestDTO phase : requestDTO.phases()) {
            if (!phaseNumbers.add(phase.numeroPhase())) {
                throw new ValidationException("Les numéros de phase doivent être uniques par contrat.");
            }
            totalPhaseDuration.addAndGet(phase.dureeMois());
        }

        if (!Integer.valueOf(totalPhaseDuration.get()).equals(requestDTO.periodeEnMois())) {
            throw new ValidationException(String.format(
                    "La somme des durées des phases (%d mois) ne correspond pas à la période totale du contrat (%d mois).",
                    totalPhaseDuration.get(), requestDTO.periodeEnMois()
            ));
        }

        List<Integer> sortedPhaseNumbers = requestDTO.phases().stream()
                .map(ContratLeasingPhaseRequestDTO::numeroPhase)
                .sorted()
                .toList();
        for (int i = 0; i < sortedPhaseNumbers.size(); i++) {
            if (sortedPhaseNumbers.get(i) != (i + 1)) {
                throw new ValidationException("Les numéros de phase doivent être séquentiels et commencer par 1 (e.g., 1, 2, 3...).");
            }
        }
    }

    // ── Calcul ───────────────────────────────────────────────────────────────

    private void setValeurResiduelleBasedOnType(ContratLeasing contratLeasing, BigDecimal montantAchatTTC) {
        BigDecimal valeurResiduelle = montantAchatTTC
                .multiply(leasingProperties.getValeurResiduelle())
                .setScale(2, RoundingMode.HALF_UP);
        contratLeasing.setValeurResiduelle(valeurResiduelle);
    }

    // ── Utilitaire ───────────────────────────────────────────────────────────

    private Pageable effectivePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }
}
