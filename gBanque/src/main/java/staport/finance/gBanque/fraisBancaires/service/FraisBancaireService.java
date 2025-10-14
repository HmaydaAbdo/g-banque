package staport.finance.gBanque.fraisBancaires.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResposneDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.mapper.FraisBancaireMapper;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.fraisBancaires.spec.FraisBancaireSpec;
import java.math.BigDecimal;

@Service
public class FraisBancaireService {

    private static final Logger log = LogManager.getLogger(FraisBancaireService.class);
    private final FraisBancaireRepository fraisBancaireRepository;
    private final BanqueRepository banqueRepository;
    private final FraisBancaireMapper fraisBancaireMapper;

    public FraisBancaireService(FraisBancaireRepository fraisBancaireRepository, BanqueRepository banqueRepository, FraisBancaireMapper fraisBancaireMapper) {
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.banqueRepository = banqueRepository;
        this.fraisBancaireMapper = fraisBancaireMapper;
    }

    // --- Create ---
    @Transactional
    public FraisBancaireResposneDto createFraisBancaire(FraisBancaireRequestDto requestDto) {

        log.info("Creating FraisBancaire for banqueId={} libelle={}", requestDto.getBanqueId(), requestDto.getLibelle());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        FraisBancaire fraisBancaire = fraisBancaireMapper.toEntity(requestDto, banque);

        fraisBancaireRepository.save(fraisBancaire);

        log.info("Frais created with id={}", fraisBancaire.getId());

        return fraisBancaireMapper.toResponseDto(fraisBancaire);
    }


    // --- Get by ID ---
    @Transactional(readOnly = true)
    public FraisBancaireResposneDto getById(Long id) {

        log.info("Fetching frais Bancaire with id={}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Frais Bancaire not found with id={}", id);
                    return new IllegalArgumentException("Frais Bancaire not found  not found");
                });

        return fraisBancaireMapper.toResponseDto(fraisBancaire);
    }

    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<FraisBancaireResposneDto> searchFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable) {

        log.info("Searching frais bancaire with criteria: {}", searchDto);

        return fraisBancaireRepository.findAll(
                        FraisBancaireSpec.buildSpecification(searchDto),
                        pageable
                ).map(fraisBancaireMapper::toResponseDto);
    }

    // --- Update ---
    @Transactional
    public FraisBancaireResposneDto updateFraisBancaire(Long id, FraisBancaireRequestDto requestDto) {

        log.info("Updating frais bancaire id={} with libelle={}", id, requestDto.getLibelle());

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Frais Bancaire not found with id={}", id);
                    return new IllegalArgumentException("Frais Bancaire not found");
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque not found with id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque not found");
                });

        fraisBancaire.setLibelle(requestDto.getLibelle());
        fraisBancaire.setMontant(requestDto.getMontant());
        fraisBancaire.setBanque(banque);
        fraisBancaire.setDateEmission(requestDto.getDateEmission());

        fraisBancaireRepository.save(fraisBancaire);
        log.info("Frais Bancaire updated successfully id={}", fraisBancaire.getId());
        return fraisBancaireMapper.toResponseDto(fraisBancaire);
    }

    // --- Delete ---
    @Transactional
    public void deleteFraisBancaire(Long id) {
        log.info("Deleting Frais Bancaire with id={}", id);

        if (!fraisBancaireRepository.existsById(id)) {
            log.error("Frais Bancaire not found with id={}", id);
            throw new IllegalArgumentException("Frais Bancaire not found");
        }

        fraisBancaireRepository.deleteById(id);
        log.info("FraisBancaire deleted successfully id={}", id);
    }

    /**
     * Retrieves a paginated list of outstanding frais bancaires and calculates their total amount.
     * Outstanding frais bancaires are those with a 'PREVU' status and an emission date in the past.
     *
     * @param pageable Pagination and sorting information.
     * @param searchDto Optional search criteria (e.g., for filtering by bank, libelle, etc.).
     * @return An EncoursFraisBancaireDto containing the page of frais bancaires and the total outstanding amount.
     */
    @Transactional(readOnly = true)
    public EncoursFraisBancaireDto getOutstandingFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable) {

        // Build the specification to filter the records
        Specification<FraisBancaire> spec = Specification.where(FraisBancaireSpec.outstandingFraisBancaires())
                .and(FraisBancaireSpec.hasBanque(searchDto.getBanqueId()))
                .and(FraisBancaireSpec.hasLibelle(searchDto.getLibelle()));

        // Fetch the paginated list of frais bancaires
        Page<FraisBancaire> fraisBancairePage = fraisBancaireRepository.findAll(spec, pageable);

        // Fetch the total sum of the montant for the same criteria
        BigDecimal totalDesEncours = fraisBancaireRepository.findAll(spec).stream()
                .map(FraisBancaire::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Map the page of entities to a page of DTOs
        Page<FraisBancaireResposneDto> pageDto = fraisBancairePage.map(fraisBancaireMapper::toResponseDto);

        // Create and return the final DTO
        EncoursFraisBancaireDto result = new EncoursFraisBancaireDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }
}
