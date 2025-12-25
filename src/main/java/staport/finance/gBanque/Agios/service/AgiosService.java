package staport.finance.gBanque.Agios.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Agios.dtos.AgiosRequestDto;
import staport.finance.gBanque.Agios.dtos.AgiosResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosSearchDto;
import staport.finance.gBanque.Agios.dtos.EncoursAgiosDto;
import staport.finance.gBanque.Agios.entities.Agios;
import staport.finance.gBanque.Agios.mapper.AgiosMapper;
import staport.finance.gBanque.Agios.repository.AgiosRepository;
import staport.finance.gBanque.Agios.spec.AgiosSpec;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import java.math.BigDecimal;

@Service
public class AgiosService {

    private static final Logger log = LogManager.getLogger(AgiosService.class);
    private final AgiosRepository agiosRepository;
    private final BanqueRepository banqueRepository;
    private final AgiosMapper agiosMapper;

    public AgiosService(AgiosRepository agiosRepository, BanqueRepository banqueRepository, AgiosMapper agiosMapper) {
        this.agiosRepository = agiosRepository;
        this.banqueRepository = banqueRepository;
        this.agiosMapper = agiosMapper;
    }


    // --- Create ---
    @Transactional
    public AgiosResponseDto createAgios(AgiosRequestDto requestDto) {
        log.info("Creating agios for banqueId={}", requestDto.getBanqueId());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found with id: " + requestDto.getBanqueId() );
                });

        Agios agios = agiosMapper.toEntity(requestDto, banque);
        agiosRepository.save(agios);

        log.info("Agios created with id={}", agios.getId());
        return agiosMapper.toResponseDto(agios);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public AgiosResponseDto getById(Long id) {
        log.info("Fetching agios with id={}", id);

        Agios agios = agiosRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("agios not found with id={}", id);
                    return new IllegalArgumentException("agios not found with id: " + id);
                });

        return agiosMapper.toResponseDto(agios);
    }

    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<AgiosResponseDto> searchAgios(AgiosSearchDto searchDto, Pageable pageable) {
        log.info("Searching agios with criteria: {}", searchDto);
        return agiosRepository.findAll(
                        AgiosSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(agiosMapper::toResponseDto);
    }

    // --- Update ---
    @Transactional
    public AgiosResponseDto updateAgios(Long id, AgiosRequestDto requestDto) {
        log.info("Updating agios id={} ", id);

        Agios agios = agiosRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("agios not found with id={}", id);
                    return new IllegalArgumentException("agios not found with id: " + id);
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found with id: " + requestDto.getBanqueId());
                });

        agios.setLibelle(requestDto.getLibelle());
        agios.setMontant(requestDto.getMontant());
        agios.setBanque(banque);
        agios.setDateEmission(requestDto.getDateEmission());

        agiosRepository.save(agios);
        log.info("Agios updated successfully id={}", agios.getId());
        return agiosMapper.toResponseDto(agios);
    }

    // --- Delete ---
    @Transactional
    public void deleteAgios(Long id) {
        log.info("Deleting Agios with id={}", id);
        if (!agiosRepository.existsById(id)) {
            log.error("Agios not found with id={}", id);
            throw new IllegalArgumentException("Agios not found with id: " + id );
        }
        agiosRepository.deleteById(id);
        log.info("Agios deleted successfully id={}", id);
    }

    /**
     * Retrieves a paginated list of outstanding agios and calculates their total amount.
     * Outstanding agios are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursAgiosDto containing the page of agios and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursAgiosDto getOutstandingAgios(AgiosSearchDto searchDto, Pageable pageable) {
        // Build the specification to filter the records
        Specification<Agios> spec = Specification.where(AgiosSpec.outstandingAgios())
                .and(AgiosSpec.hasBanque(searchDto.getBanqueId()))
                .and(AgiosSpec.hasLibelle(searchDto.getLibelle()))
                .and(AgiosSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of agios
        Page<Agios> agiosPage = agiosRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = agiosRepository.findAll(spec).stream()
                .map(Agios::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<AgiosResponseDto> pageDto = agiosPage.map(agiosMapper::toResponseDto);

        // Create and return the final DTO
        EncoursAgiosDto result = new EncoursAgiosDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

}
