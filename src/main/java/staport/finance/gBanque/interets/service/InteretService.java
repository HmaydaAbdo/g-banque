package staport.finance.gBanque.interets.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.interets.dtos.EncoursInteretDto;
import staport.finance.gBanque.interets.dtos.InteretRequestDto;
import staport.finance.gBanque.interets.dtos.InteretResponseDto;
import staport.finance.gBanque.interets.dtos.InteretSearchDto;
import staport.finance.gBanque.interets.entities.Interet;
import staport.finance.gBanque.interets.mapper.InteretMapper;
import staport.finance.gBanque.interets.repository.InteretRepository;
import staport.finance.gBanque.interets.specs.InteretSpec;

import java.math.BigDecimal;

@Service
public class InteretService {


    private static final Logger log = LogManager.getLogger(InteretService.class);
    private final InteretRepository interetRepository;
    private final BanqueRepository banqueRepository;
    private final InteretMapper interetMapper;

    public InteretService(InteretRepository interetRepository, BanqueRepository banqueRepository, InteretMapper interetMapper) {
        this.interetRepository = interetRepository;
        this.banqueRepository = banqueRepository;
        this.interetMapper = interetMapper;
    }


    // --- Create ---
    @Transactional
    public InteretResponseDto createInterest(InteretRequestDto requestDto) {

        log.info("Creating Interest for banqueId={} libelle={}", requestDto.getBanqueId(), requestDto.getLibelle());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        Interet interet = interetMapper.toEntity(requestDto, banque);

        interetRepository.save(interet);

        log.info("Interest created with id={}", interet.getId());

        return interetMapper.toResponseDto(interet);
    }


    // --- Get by ID ---
    @Transactional(readOnly = true)
    public InteretResponseDto getById(Long id) {

        log.info("Fetching interest Bancaire with id={}", id);

        Interet interet = interetRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Interest not found with id={}", id);
                    return new IllegalArgumentException("Interest not found with id "+id);
                });

        return interetMapper.toResponseDto(interet);
    }

    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<InteretResponseDto> searchInterest(InteretSearchDto searchDto, Pageable pageable) {

        log.info("Searching interest with criteria: {}", searchDto);

        return interetRepository.findAll(
                InteretSpec.buildSpecification(searchDto),
                pageable
        ).map(interetMapper::toResponseDto);
    }

    // --- Update ---
    @Transactional
    public InteretResponseDto updateInterest(Long id, InteretRequestDto requestDto) {

        log.info("Updating interest id={} ", id);

        Interet interet = interetRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("interest not found with id={}", id);
                    return new IllegalArgumentException("Interest not found");
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        interet.setLibelle(requestDto.getLibelle());
        interet.setMontant(requestDto.getMontant());
        interet.setBanque(banque);
        interet.setDateEmission(requestDto.getDateEmission());

        interetRepository.save(interet);
        log.info("Interest updated successfully id={}", interet.getId());
        return interetMapper.toResponseDto(interet);
    }

    // --- Delete ---
    @Transactional
    public void deleteInterest(Long id) {
        log.info("Deleting  with id={}", id);

        if (!interetRepository.existsById(id)) {
            log.error("interest not found with id={}", id);
            throw new IllegalArgumentException("Interest not found with id " + id);
        }

        interetRepository.deleteById(id);
        log.info("Interest deleted successfully id={}", id);
    }

    /**
     * Retrieves a paginated list of outstanding interest and calculates their total amount.
     * Outstanding interest are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursInteretDto containing the page of interest and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursInteretDto getOutstandingInterest(InteretSearchDto searchDto, Pageable pageable) {

        // Build the specification to filter the records
        Specification<Interet> spec = Specification.where(InteretSpec.outstandingInterests())
                .and(InteretSpec.hasBanque(searchDto.getBanqueId()))
                .and(InteretSpec.hasLibelle(searchDto.getLibelle()))
                .and(InteretSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of interest
        Page<Interet> interetPage = interetRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = interetRepository.findAll(spec).stream()
                .map(Interet::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<InteretResponseDto> pageDto = interetPage.map(interetMapper::toResponseDto);

        // Create and return the final DTO
        EncoursInteretDto result = new EncoursInteretDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

}
