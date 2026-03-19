package staport.finance.gBanque.commision.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.commision.dtos.*;
import staport.finance.gBanque.commision.entities.Commission;
import staport.finance.gBanque.commision.mapper.CommissionMapper;
import staport.finance.gBanque.commision.repository.CommisionRepository;
import staport.finance.gBanque.commision.specs.CommissionSpecifications;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;


@Service
public class CommissionService {

    private static final Logger log = LogManager.getLogger(CommissionService.class);
    private final CommisionRepository commissionRepository;
    private final BanqueRepository banqueRepository;
    private final CommissionMapper commissionMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public CommissionService(CommisionRepository commissionRepository, BanqueRepository banqueRepository, CommissionMapper commissionMapper) {
        this.commissionRepository = commissionRepository;
        this.banqueRepository = banqueRepository;
        this.commissionMapper = commissionMapper;
    }

    // --- Create ---
    @Transactional
    public CommissionResponseDto createCommission(CommissionRequestDto requestDto) {
        log.info("Creating commission for banqueId={} libelle={}", requestDto.getBanqueId(), requestDto.getLibelle());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        Commission commission = commissionMapper.toEntity(requestDto, banque);
        commissionRepository.save(commission);

        log.info("Commission created with id={}", commission.getId());
        return commissionMapper.toResponseDto(commission);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public CommissionResponseDto getById(Long id) {
        log.info("Fetching commission with id={}", id);

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        return commissionMapper.toResponseDto(commission);
    }

    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<CommissionResponseDto> searchCommissions(CommissionSearchDto searchDto, Pageable pageable) {
        log.info("Searching commissions with criteria: {}", searchDto);
        return commissionRepository.findAll(
                        CommissionSpecifications.buildSpecification(searchDto),
                        pageable
                )
                .map(commissionMapper::toResponseDto);
    }

    // --- Update ---
    @Transactional
    public CommissionResponseDto updateCommission(Long id, CommissionRequestDto requestDto) {
        log.info("Updating commission id={} with libelle={}", id, requestDto.getLibelle());

        Commission commission = commissionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Commission not found with id={}", id);
                    return new IllegalArgumentException("Commission not found");
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        commission.setLibelle(requestDto.getLibelle());
        commission.setMontant(requestDto.getMontant());
        commission.setBanque(banque);
        commission.setOperation(requestDto.getOperation());
        commission.setDateEmission(requestDto.getDateEmission());

        commissionRepository.save(commission);
        log.info("Commission updated successfully id={}", commission.getId());
        return commissionMapper.toResponseDto(commission);
    }

    // --- Delete ---
    @Transactional
    public void deleteCommission(Long id) {
        log.info("Deleting commission with id={}", id);
        if (!commissionRepository.existsById(id)) {
            log.error("Commission not found with id={}", id);
            throw new IllegalArgumentException("Commission not found");
        }
        commissionRepository.deleteById(id);
        log.info("Commission deleted successfully id={}", id);
    }

    /**
     * Retrieves a paginated list of outstanding commissions and calculates their total amount.
     * Outstanding commissions are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursCommissionDto containing the page of commissions and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursCommissionDto getOutstandingCommissions(CommissionSearchDto searchDto, Pageable pageable) {
        // Build the specification to filter the records
        Specification<Commission> spec = Specification.where(CommissionSpecifications.outstandingUntil(endOfMonth))
                .and(CommissionSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(CommissionSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(CommissionSpecifications.hasOperation(searchDto.getOperation()))
                .and(CommissionSpecifications.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of commissions
        Page<Commission> commissionPage = commissionRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = commissionRepository.findAll(spec).stream()
                .map(Commission::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<CommissionResponseDto> pageDto = commissionPage.map(commissionMapper::toResponseDto);

        // Create and return the final DTO
        EncoursCommissionDto result = new EncoursCommissionDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


    /**
     * Supprime toutes les commissions liées à un contrat de leasing.
     * Utilise le numéro de contrat pour identifier les commissions via leur libellé.
     *
     * @param numeroContrat Le numéro du contrat de leasing
     */
    @Transactional
    public void deleteCommissionsByContratLeasing(String numeroContrat) {
        log.info("Début de la suppression des commissions pour le contrat N° : {}", numeroContrat);

        if (numeroContrat == null || numeroContrat.trim().isEmpty()) {
            log.warn("Tentative de suppression de commissions avec un numéro de contrat vide");
            return;
        }

        // Pattern de recherche : toutes les commissions dont le libellé contient le numéro du contrat
        String libellePattern = "Commission Prelevement Leasing contrat N°" + numeroContrat + "%";

        log.debug("Pattern de recherche : {}", libellePattern);

        // Rechercher toutes les commissions correspondantes (sans filtre de date)
        List<Commission> commissions = commissionRepository.findByLibelleLike(libellePattern);

        if (commissions.isEmpty()) {
            log.info("Aucune commission trouvée pour le contrat N° : {}", numeroContrat);
            return;
        }

        int nombreCommissions = commissions.size();
        log.debug("Nombre de commissions trouvées : {}", nombreCommissions);

        // Supprimer toutes les commissions
        commissionRepository.deleteAll(commissions);

        log.info("Suppression terminée : {} commission(s) supprimée(s) pour le contrat N° : {}",
                nombreCommissions, numeroContrat);

    }

    @Transactional
    public void toggleCommisionStatus(Long commissionId) {

        Commission commission = commissionRepository.findById(commissionId)
                .orElseThrow(() -> {
                    log.error("Commission avec l'id {} introuvable", commissionId);
                    return new ResourceNotFoundException(
                            "Commission avec l'id " + commissionId + " introuvable"
                    );
                });

        log.info("Commission avec l'id {} trouvée. Statut actuel : {}", commissionId, commission.getStatus());
        commission.setStatus(commission.getStatus() == Status.PAYE ? Status.PREVU : Status.PAYE);

        log.info("Statut de la commission avec l'id {} mis à jour. Nouveau statut : {}", commissionId, commission.getStatus());

        commissionRepository.save(commission);
    }

}
