package staport.finance.gBanque.restitution.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.prefinancement.enums.Status;
import staport.finance.gBanque.restitution.dtos.EncoursRestitution;
import staport.finance.gBanque.restitution.dtos.RestitutionRequestDto;
import staport.finance.gBanque.restitution.dtos.RestitutionResponseDto;
import staport.finance.gBanque.restitution.dtos.RestitutionSearchDto;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.restitution.mapper.RestitutionMapper;
import staport.finance.gBanque.restitution.repository.RestitutionRepository;
import staport.finance.gBanque.restitution.specs.RestitutionSpec;

import java.math.BigDecimal;

@Service
public class RestitutionService {

    private static final Logger log = LogManager.getLogger(RestitutionService.class);
    private final RestitutionRepository restitutionRepository;
    private final BanqueRepository banqueRepository;
    private final RestitutionMapper restitutionMapper;

    public RestitutionService(RestitutionRepository restitutionRepository, BanqueRepository banqueRepository, RestitutionMapper restitutionMapper) {
        this.restitutionRepository = restitutionRepository;
        this.banqueRepository = banqueRepository;
        this.restitutionMapper = restitutionMapper;
    }

    // --- Create ---
    @Transactional
    public RestitutionResponseDto createRestitution(RestitutionRequestDto requestDto) {
        log.info("Début de création d'une restitution avec les données suivantes: {}", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable avec l'id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque introuvable avec l'id: " + requestDto.getBanqueId());
                });

        Restitution restitution = restitutionMapper.toEntity(requestDto, banque);
        restitutionRepository.save(restitution);

        log.info("Restitution créée avec succès avec l'id={}", restitution.getId());
        return restitutionMapper.toResponseDto(restitution);
    }

    // --- Get by ID ---
    @Transactional(readOnly = true)
    public RestitutionResponseDto getById(Long id) {
        log.info("Récupération de la restitution avec l'id={}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Restitution introuvable avec l'id={}", id);
                    return new IllegalArgumentException("Restitution introuvable avec l'id: " + id);
                });

        log.info("Restitution récupérée avec succès: id={}", id);
        return restitutionMapper.toResponseDto(restitution);
    }

    // --- Search with pagination ---
    @Transactional(readOnly = true)
    public Page<RestitutionResponseDto> searchRestitutions(RestitutionSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de restitutions avec les critères: {}", searchDto);

        Page<RestitutionResponseDto> result = restitutionRepository.findAll(
                        RestitutionSpec.buildSpecification(searchDto),
                        pageable
                )
                .map(restitutionMapper::toResponseDto);

        log.info("Recherche terminée: {} restitutions trouvées", result.getTotalElements());
        return result;
    }

    // --- Update ---
    @Transactional
    public RestitutionResponseDto updateRestitution(Long id, RestitutionRequestDto requestDto) {
        log.info("Début de mise à jour de la restitution id={} avec les données: {}", id, requestDto);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Restitution introuvable avec l'id={}", id);
                    return new IllegalArgumentException("Restitution introuvable avec l'id: " + id);
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable avec l'id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque introuvable avec l'id: " + requestDto.getBanqueId());
                });

        restitution.setLibelle(requestDto.getLibelle());
        restitution.setObservation(requestDto.getObservation());
        restitution.setMontant(requestDto.getMontant());
        restitution.setBanque(banque);
        restitution.setDateEmission(requestDto.getDateEmission());

        restitutionRepository.save(restitution);
        log.info("Restitution mise à jour avec succès: id={}", restitution.getId());
        return restitutionMapper.toResponseDto(restitution);
    }

    // --- Delete ---
    @Transactional
    public void deleteRestitution(Long id) {
        log.info("Début de suppression de la restitution avec l'id={}", id);

        if (!restitutionRepository.existsById(id)) {
            log.error("Restitution introuvable avec l'id={}", id);
            throw new IllegalArgumentException("Restitution introuvable avec l'id: " + id);
        }

        restitutionRepository.deleteById(id);
        log.info("Restitution supprimée avec succès: id={}", id);
    }

    // --- Get Outstanding Restitutions ---
    @Transactional(readOnly = true)
    public EncoursRestitution getOutstandingRestitutions(RestitutionSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des encours de restitutions avec les critères: {}", searchDto);

        // Construction de la spécification pour filtrer les enregistrements
        Specification<Restitution> spec = Specification.where(RestitutionSpec.outstandingRestitution())
                .and(RestitutionSpec.hasBanque(searchDto.getBanqueId()))
                .and(RestitutionSpec.hasLibelle(searchDto.getLibelle()))
                .and(RestitutionSpec.hasMontant(searchDto.getMontant()))
                .and(RestitutionSpec.hasStatus(searchDto.getStatus()));

        // Récupération de la liste paginée des restitutions
        Page<Restitution> restitutionPage = restitutionRepository.findAll(spec, pageable);

        // Calcul du total des montants pour les mêmes critères
        BigDecimal totalDesEncours = restitutionRepository.findAll(spec).stream()
                .map(Restitution::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Mapping de la page d'entités vers une page de DTOs
        Page<RestitutionResponseDto> pageDto = restitutionPage.map(restitutionMapper::toResponseDto);

        // Création et retour du DTO final
        EncoursRestitution result = new EncoursRestitution();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours de restitutions récupérés: {} restitutions trouvées, total des encours={}",
                pageDto.getTotalElements(), totalDesEncours);

        return result;
    }

    // --- Toggle Status ---
    @Transactional
    public RestitutionResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de la restitution ID: {}", id);

        Restitution restitution = restitutionRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Restitution introuvable avec l'id={}", id);
                    return new IllegalArgumentException("Restitution introuvable avec l'id: " + id);
                });

        Status oldStatus = restitution.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        restitution.setStatus(newStatus);
        Restitution updated = restitutionRepository.save(restitution);

        log.info("Statut de la restitution ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return restitutionMapper.toResponseDto(updated);
    }
}