package staport.finance.gBanque.prefinancement.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.entities.LigneFonctionnelleSpecifique;
import staport.finance.gBanque.LigneFonctionnelle.specifiqueLignes.repositories.LigneFonctionnelleSpecifiqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.marches.repositories.MarcheRepository;
import staport.finance.gBanque.prefinancement.dtos.*;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.prefinancement.mapper.PrefinancementMapper;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.prefinancement.spec.PrefinancementSpecification;

import java.math.BigDecimal;
import java.util.Optional;


@Service
public class PrefinancementService {

    private static final Logger log = LoggerFactory.getLogger(PrefinancementService.class);
    private final PrefinancementRepository prefinancementRepository;
    private final MarcheRepository marcheRepository;
    private final PrefinancementMapper prefinancementMapper;
    private final LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository;
    private final NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;

    public PrefinancementService(
            PrefinancementRepository prefinancementRepository,
            MarcheRepository marcheRepository,
            PrefinancementMapper prefinancementMapper,
            LigneFonctionnelleSpecifiqueRepository ligneFonctionnelleSpecifiqueRepository,
            NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository) {
        this.prefinancementRepository = prefinancementRepository;
        this.marcheRepository = marcheRepository;
        this.prefinancementMapper = prefinancementMapper;
        this.ligneFonctionnelleSpecifiqueRepository = ligneFonctionnelleSpecifiqueRepository;
        this.natureLigneFonctionnelleRepository = natureLigneFonctionnelleRepository;
    }

    @Transactional
    public PrefinancementResponseDTO createPrefinancement(PrefinancementRequestDTO requestDTO) throws Exception {
        log.info("Creating prefinancement for MarcheId: {}", requestDTO.getMarcheId());

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marche not found with ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        log.debug("Marche found: {}", marche.getNom());

        NatureLigneFonctionnelle prefiNature = natureLigneFonctionnelleRepository.findByLabel("Préfinancement")
                .orElseThrow(() -> {
                    log.error("Nature de ligne fonctionnelle 'Préfinancement' not found");
                    return new ResourceNotFoundException("Nature de ligne fonctionnelle 'Préfinancement' non trouvée.");
                });

        Optional<LigneFonctionnelleSpecifique> activePrefiLine = ligneFonctionnelleSpecifiqueRepository
                .findByMarcheAndNatureLigneFonctionnelleAndActifTrue(marche, prefiNature);

        if (activePrefiLine.isEmpty()) {
            log.warn("No active 'Préfinancement' line for Marche: {}", marche.getNom());
            throw new Exception("Impossible de créer un préfinancement. Le marché '" + marche.getNom() + "' n'est pas associé à une ligne fonctionnelle 'Préfinancement' active.");
        }

        log.debug("Active 'Préfinancement' line found for Marche: {}", marche.getNom());

        Prefinancement prefinancement = prefinancementMapper.toEntity(requestDTO, marche);
        prefinancement = prefinancementRepository.save(prefinancement);

        log.info("Prefinancement created with ID: {}", prefinancement.getId());
        return prefinancementMapper.toDto(prefinancement);
    }

    @Transactional
    public PrefinancementResponseDTO updatePrefinancement(Long id, PrefinancementRequestDTO requestDTO) {
        log.info("Updating prefinancement with ID: {}", id);

        Prefinancement existingPrefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prefinancement not found with ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        Marche marche = marcheRepository.findById(requestDTO.getMarcheId())
                .orElseThrow(() -> {
                    log.error("Marche not found with ID: {}", requestDTO.getMarcheId());
                    return new ResourceNotFoundException("Marche not found with ID: " + requestDTO.getMarcheId());
                });

        existingPrefinancement.setMarche(marche);
        existingPrefinancement.setDateEmission(requestDTO.getDateEmission());
        existingPrefinancement.setMontant(requestDTO.getMontant());
        existingPrefinancement.setType(requestDTO.getType());
        existingPrefinancement.setObservation("Marche: " + marche.getNumero());

        Prefinancement updatedPrefinancement = prefinancementRepository.save(existingPrefinancement);
        log.info("Prefinancement updated with ID: {}", updatedPrefinancement.getId());

        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    @Transactional
    public void deletePrefinancement(Long id) {
        log.info("Deleting prefinancement with ID: {}", id);

        if (!prefinancementRepository.existsById(id)) {
            log.error("Prefinancement not found with ID: {}", id);
            throw new ResourceNotFoundException("Prefinancement not found with ID: " + id);
        }

        prefinancementRepository.deleteById(id);
        log.info("Prefinancement deleted with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public PrefinancementResponseDTO findById(Long id) {
        log.info("Fetching prefinancement with ID: {}", id);

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prefinancement not found with ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        return prefinancementMapper.toDto(prefinancement);
    }

    @Transactional(readOnly = true)
    public Page<PrefinancementResponseDTO> findAll(PrefinancementSearchDTO searchDTO, Pageable pageable) {
        log.info("Fetching prefinancements with search criteria: {}", searchDTO);

        Specification<Prefinancement> spec = PrefinancementSpecification.bySearchCriteria(searchDTO);
        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable);

        log.info("Found {} prefinancement records", prefinancementPage.getTotalElements());
        return prefinancementPage.map(prefinancementMapper::toDto);
    }

    @Transactional
    public PrefinancementResponseDTO toggleStatus(Long id) {
        log.info("Toggling status for prefinancement ID: {}", id);

        Prefinancement prefinancement = prefinancementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Prefinancement not found with ID: {}", id);
                    return new ResourceNotFoundException("Prefinancement not found with ID: " + id);
                });

        Status existingStatus = prefinancement.getStatus();
        prefinancement.setStatus(existingStatus == Status.PAYE ? Status.PREVU : Status.PAYE);

        Prefinancement updatedPrefinancement = prefinancementRepository.save(prefinancement);
        log.info("Prefinancement status updated to {} for ID: {}", updatedPrefinancement.getStatus(), updatedPrefinancement.getId());

        return prefinancementMapper.toDto(updatedPrefinancement);
    }

    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getRemboursementEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Fetching remboursement encours for banque ID: {}, searchDto: {}", banqueId, searchDto);

        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingRemboursementForCurrentMonth())
                .and(PrefinancementSpecification.byMarcheName(searchDto.getMarcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(PrefinancementSpecification.hasMontant(searchDto.getMontant()));

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = prefinancementRepository.findAll(spec).stream()
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total remboursement encours: {}", totalDesEncours);

        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);
        EncoursPrefinancementDto result = new EncoursPrefinancementDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }

    @Transactional(readOnly = true)
    public EncoursPrefinancementDto getDeblocageEncoursForBanque(EncoursPrefinancementSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Fetching deblocage encours for banque ID: {}, searchDto: {}", banqueId, searchDto);

        Specification<Prefinancement> spec = PrefinancementSpecification.byBanqueId(banqueId)
                .and(PrefinancementSpecification.outstandingDeblocageForCurrentMonth())
                .and(PrefinancementSpecification.byMarcheName(searchDto.getMarcheName()))
                .and(PrefinancementSpecification.byMarcheNumero(searchDto.getMarcheNumero()))
                .and(PrefinancementSpecification.hasMontant(searchDto.getMontant()));

        Page<Prefinancement> prefinancementPage = prefinancementRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = prefinancementRepository.findAll(spec).stream()
                .map(Prefinancement::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total deblocage encours: {}", totalDesEncours);

        Page<PrefinancementResponseDTO> pageDto = prefinancementPage.map(prefinancementMapper::toDto);
        EncoursPrefinancementDto result = new EncoursPrefinancementDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        return result;
    }
}
