package staport.finance.gBanque.interets.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.interets.dtos.*;
import staport.finance.gBanque.interets.entities.Interet;
import staport.finance.gBanque.interets.mapper.InteretMapper;
import staport.finance.gBanque.interets.repository.InteretRepository;
import staport.finance.gBanque.interets.specs.InteretSpec;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Service
public class InteretService {

    private static final Logger log = LoggerFactory.getLogger(InteretService.class);
    private final InteretRepository interetRepository;
    private final BanqueRepository banqueRepository;
    private final InteretMapper interetMapper;

    public InteretService(InteretRepository interetRepository, BanqueRepository banqueRepository, InteretMapper interetMapper) {
        this.interetRepository = interetRepository;
        this.banqueRepository = banqueRepository;
        this.interetMapper = interetMapper;
        log.info("Initialisation du service Interet");
    }

    @Transactional
    public InteretResponseDto createInterest(InteretRequestDto requestDto) {
        log.info("Création d'un intérêt - Banque ID: {}, Libellé: '{}', Montant: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());
        Interet interet = interetMapper.toEntity(requestDto, banque);
        Interet saved = interetRepository.save(interet);

        log.info("Intérêt créé avec succès - ID: {}, Statut: {}", saved.getId(), saved.getStatus());
        return interetMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public InteretResponseDto getById(Long id) {
        log.info("Récupération de l'intérêt avec l'ID: {}", id);

        Interet interet = interetRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Intérêt non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Intérêt non trouvé avec l'ID: " + id);
                });

        log.debug("Intérêt trouvé - Libellé: '{}', Montant: {}", interet.getLibelle(), interet.getMontant());
        return interetMapper.toResponseDto(interet);
    }

    @Transactional(readOnly = true)
    public Page<InteretResponseDto> searchInterest(InteretSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'intérêts - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<InteretResponseDto> result = interetRepository.findAll(
                        InteretSpec.buildSpecification(searchDto), pageable)
                .map(interetMapper::toResponseDto);

        log.info("Recherche terminée - {} intérêt(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Transactional
    public InteretResponseDto updateInterest(Long id, InteretRequestDto requestDto) {
        log.info("Mise à jour de l'intérêt ID: {} - Nouveau libellé: '{}'", id, requestDto.getLibelle());

        Interet interet = interetRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Intérêt non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Intérêt non trouvé avec l'ID: " + id);
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        interet.setLibelle(requestDto.getLibelle());
        interet.setMontant(requestDto.getMontant());
        interet.setBanque(banque);
        interet.setDateEmission(requestDto.getDateEmission());

        Interet updated = interetRepository.save(interet);
        log.info("Intérêt ID: {} mis à jour avec succès", id);
        return interetMapper.toResponseDto(updated);
    }

    @Transactional
    public void deleteInterest(Long id) {
        log.info("Suppression de l'intérêt avec l'ID: {}", id);

        if (!interetRepository.existsById(id)) {
            log.error("Intérêt non trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Intérêt non trouvé avec l'ID: " + id);
        }

        interetRepository.deleteById(id);
        log.info("Intérêt ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public EncoursInteretDto getOutstandingInterest(InteretSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des intérêts en cours - Critères: {}", searchDto);

        Specification<Interet> spec = Specification.where(InteretSpec.outstandingInterests())
                .and(InteretSpec.hasBanque(searchDto.getBanqueId()))
                .and(InteretSpec.hasLibelle(searchDto.getLibelle()))
                .and(InteretSpec.hasMontant(searchDto.getMontant()));

        Page<Interet> page = interetRepository.findAll(spec, pageable);
        BigDecimal total = interetRepository.findAll(spec).stream()
                .map(Interet::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre d'intérêts: {}", total, page.getTotalElements());

        EncoursInteretDto result = new EncoursInteretDto();
        result.setPage(page.map(interetMapper::toResponseDto));
        result.setTotalDesEncours(total);
        return result;
    }

    @Transactional
    public InteretResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'intérêt ID: {}", id);

        Interet interet = interetRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Intérêt non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Intérêt non trouvé avec l'ID: " + id);
                });

        Status oldStatus = interet.getStatus();
        Status newStatus=oldStatus==Status.PAYE ? Status.PREVU : Status.PAYE;


        interet.setStatus(newStatus);
        Interet updated = interetRepository.save(interet);

        log.info("Statut de l'intérêt ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return interetMapper.toResponseDto(updated);
    }
}
