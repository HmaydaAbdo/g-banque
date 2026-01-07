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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;


@Service
public class CommissionService {

    private static final Logger log = LogManager.getLogger(CommissionService.class);
    private final CommisionRepository commissionRepository;
    private final BanqueRepository banqueRepository;
    private final CommissionMapper commissionMapper;

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
        Specification<Commission> spec = Specification.where(CommissionSpecifications.outstandingCommissions())
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

    @Transactional
    public void deleteCommissionsByLibellePattern(String libellePattern, OffsetDateTime dateEmission) {
        List<Commission> commissions = commissionRepository.findByLibelleLikeAndDateEmission(
                libellePattern, dateEmission);

        if (!commissions.isEmpty()) {
            commissionRepository.deleteAll(commissions);
        }
    }
}
