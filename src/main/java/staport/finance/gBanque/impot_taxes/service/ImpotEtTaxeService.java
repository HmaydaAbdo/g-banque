package staport.finance.gBanque.impot_taxes.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.impot_taxes.dtos.*;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.mapper.ImpotEtTaxeMapper;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.impot_taxes.specs.ImpotEtTaxeSpecifications;

import java.math.BigDecimal;

@Service
public class ImpotEtTaxeService {

    private static final Logger log = LogManager.getLogger(ImpotEtTaxeService.class);
    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final BanqueRepository banqueRepository;
    private final ImpotEtTaxeMapper impotEtTaxeMapper;

    public ImpotEtTaxeService(ImpotEtTaxeRepository impotEtTaxeRepository,
                              BanqueRepository banqueRepository,
                              ImpotEtTaxeMapper impotEtTaxeMapper) {
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.banqueRepository = banqueRepository;
        this.impotEtTaxeMapper = impotEtTaxeMapper;
    }

    // --- Create ---
    @Transactional
    public ImpotEtTaxeResponseDto createImpotEtTaxe(ImpotEtTaxeRequestDto requestDto) {
        log.info("Creating impot et taxe avec les données suivantes {} ", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        ImpotEtTaxe impotEtTaxe = impotEtTaxeMapper.toEntity(requestDto, banque);
        impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Impot et taxe created with id={}", impotEtTaxe.getId());
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public ImpotEtTaxeResponseDto getById(Long id) {
        log.info("Fetching impot et taxe with id={}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impot et taxe not found with id={}", id);
                    return new IllegalArgumentException("Impot et taxe not found");
                });

        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<ImpotEtTaxeResponseDto> searchImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Searching impots et taxes with criteria: {}", searchDto);
        return impotEtTaxeRepository.findAll(ImpotEtTaxeSpecifications.buildSpecification(searchDto), pageable)
                .map(impotEtTaxeMapper::toResponseDto);
    }

    // --- Update ---
    @Transactional
    public ImpotEtTaxeResponseDto updateImpotEtTaxe(Long id, ImpotEtTaxeRequestDto requestDto) {
        log.info("Updating impot et taxe avec ces nouvelles données {}",requestDto);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impot et taxe not found with id={}", id);
                    return new IllegalArgumentException("Impot et taxe not found");
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        impotEtTaxe.setLibelle(requestDto.getLibelle());
        impotEtTaxe.setMontant(requestDto.getMontant());
        impotEtTaxe.setBanque(banque);
        impotEtTaxe.setMotif(requestDto.getMotif());
        impotEtTaxe.setDateEmission(requestDto.getDateEmission());
        impotEtTaxe.setObservation(requestDto.getObservation());

        impotEtTaxeRepository.save(impotEtTaxe);
        log.info("Impot et taxe updated successfully id={}", impotEtTaxe.getId());
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    // --- Delete ---
    @Transactional
    public void deleteImpotEtTaxe(Long id) {
        log.info("Deleting impot et taxe with id={}", id);
        if (!impotEtTaxeRepository.existsById(id)) {
            log.error("Impot et taxe not found with id={}", id);
            throw new IllegalArgumentException("Impot et taxe not found");
        }
        impotEtTaxeRepository.deleteById(id);
        log.info("Impot et taxe deleted successfully id={}", id);
    }

    /**
     * Retrieves a paginated list of outstanding impots et taxes and calculates their total amount.
     * Outstanding impots et taxes are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursImpotEtTaxeDto containing the page of impots et taxes and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursImpotEtTaxeDto getOutstandingImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        // Build the specification to filter the records
        Specification<ImpotEtTaxe> spec = Specification.where(ImpotEtTaxeSpecifications.outstandingImpotEtTaxes())
                .and(ImpotEtTaxeSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(ImpotEtTaxeSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(ImpotEtTaxeSpecifications.hasMotif(searchDto.getMotif()))
                .and(ImpotEtTaxeSpecifications.hasMontant(searchDto.getMontant()));

        // Fetch the paginated list of impots et taxes
        Page<ImpotEtTaxe> impotEtTaxePage = impotEtTaxeRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = impotEtTaxeRepository.findAll(spec).stream()
                .map(ImpotEtTaxe::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<ImpotEtTaxeResponseDto> pageDto = impotEtTaxePage.map(impotEtTaxeMapper::toResponseDto);

        // Create and return the final DTO
        EncoursImpotEtTaxeDto result = new EncoursImpotEtTaxeDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }


}