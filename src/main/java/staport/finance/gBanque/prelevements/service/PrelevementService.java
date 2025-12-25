package staport.finance.gBanque.prelevements.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.prelevements.dtos.EncoursPrelevement;
import staport.finance.gBanque.prelevements.dtos.PrelevementRequestDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementResponseDto;
import staport.finance.gBanque.prelevements.dtos.PrelevementSearchDto;
import staport.finance.gBanque.prelevements.entitie.Prelevement;
import staport.finance.gBanque.prelevements.mapper.PrelevementMapper;
import staport.finance.gBanque.prelevements.repository.PrelevementRepository;
import staport.finance.gBanque.prelevements.spec.PrelevementSpec;
import java.math.BigDecimal;

@Service
public class PrelevementService {

    private static final Logger log = LogManager.getLogger(PrelevementService.class);
    private final PrelevementRepository prelevementRepository;
    private final BanqueRepository banqueRepository;
    private final PrelevementMapper prelevementMapper;


    public PrelevementService(PrelevementRepository prelevementRepository, BanqueRepository banqueRepository, PrelevementMapper prelevementMapper) {
        this.prelevementRepository = prelevementRepository;
        this.banqueRepository = banqueRepository;
        this.prelevementMapper = prelevementMapper;
    }

    // --- Create ---
    @Transactional
    public PrelevementResponseDto createPrelevement(PrelevementRequestDto requestDto) {
        log.info("Creating prelevement for banqueId={}", requestDto.getBanqueId());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found with id: " + requestDto.getBanqueId() );
                });

        Prelevement prelevement = prelevementMapper.toEntity(requestDto, banque);
        prelevementRepository.save(prelevement);

        log.info(" prelevement created with id={}", prelevement.getId());
        return prelevementMapper.toResponseDto(prelevement);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public PrelevementResponseDto getById(Long id) {
        log.info("Fetching prelevement with id={}", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("prelevement not found with id={}", id);
                    return new IllegalArgumentException("prelevement not found with id: " + id);
                });

        return prelevementMapper.toResponseDto(prelevement);
    }


    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<PrelevementResponseDto> searchPrelevements(PrelevementSearchDto searchDto, Pageable pageable) {
        log.info("Searching prelevements with criteria: {}", searchDto);
        return prelevementRepository.findAll(
                        PrelevementSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(prelevementMapper::toResponseDto);
    }

    // --- Update ---
    @Transactional
    public PrelevementResponseDto updatePrelevement(Long id, PrelevementRequestDto requestDto) {
        log.info("Updating prelevement id={} ", id);

        Prelevement prelevement = prelevementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("prelevement not found with id={}", id);
                    return new IllegalArgumentException("prelevement not found with id: " + id);
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found with id: " + requestDto.getBanqueId());
                });

        prelevement.setLibelle(requestDto.getLibelle());
        prelevement.setMontant(requestDto.getMontant());
        prelevement.setBanque(banque);
        prelevement.setDateEmission(requestDto.getDateEmission());

        prelevementRepository.save(prelevement);
        log.info("prelevements updated successfully id={}", prelevement.getId());
        return prelevementMapper.toResponseDto(prelevement);
    }

    @Transactional
    public void deletePrelevement(Long id) {
        log.info("Deleting Prelevement with id={}", id);
        if (!prelevementRepository.existsById(id)) {
            log.error("prelevement not found with id={}", id);
            throw new IllegalArgumentException("prelevement not found with id: " + id );
        }
        prelevementRepository.deleteById(id);
        log.info("prelevement deleted successfully id={}", id);
    }


    @Transactional(readOnly = true)
    public EncoursPrelevement getOutstandingPrelevements(PrelevementSearchDto searchDto, Pageable pageable) {

        // Build the specification to filter the records
        Specification<Prelevement> spec = Specification.where(PrelevementSpec.outstandingPrelevement())
                .and(PrelevementSpec.hasBanque(searchDto.getBanqueId()))
                .and(PrelevementSpec.hasLibelle(searchDto.getLibelle()))
                .and(PrelevementSpec.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of prelevements
        Page<Prelevement> prelevementPage = prelevementRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = prelevementRepository.findAll(spec).stream()
                .map(Prelevement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<PrelevementResponseDto> pageDto = prelevementPage.map(prelevementMapper::toResponseDto);

        // Create and return the final DTO
        EncoursPrelevement result = new EncoursPrelevement();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


}

