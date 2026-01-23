package staport.finance.gBanque.moyensDePaiements.virements.virementErrone.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.entities.VirementErrone;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.mappers.VirementErroneMapper;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.repositories.VirementErroneRepository;
import staport.finance.gBanque.moyensDePaiements.virements.virementErrone.specs.VirementErroneSpec;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;

@Service
public class VirementErroneService {

    private static final Logger log = LoggerFactory.getLogger(VirementErroneService.class);
    private final VirementErroneRepository virementErroneRepository;
    private final BanqueRepository banqueRepository;
    private final VirementErroneMapper virementErroneMapper;

    public VirementErroneService(VirementErroneRepository virementErroneRepository,
                                 BanqueRepository banqueRepository,
                                 VirementErroneMapper virementErroneMapper) {
        this.virementErroneRepository = virementErroneRepository;
        this.banqueRepository = banqueRepository;
        this.virementErroneMapper = virementErroneMapper;
        log.info("Initialisation du service VirementErrone");
    }

    @Transactional
    public VirementErroneResponseDto createVirementErrone(VirementErroneRequestDto requestDto) {
        log.info("Création d'un virement erroné - Banque ID: {}, Libellé: '{}', Montant: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());
        VirementErrone virementErrone = virementErroneMapper.toEntity(requestDto, banque);
        VirementErrone saved = virementErroneRepository.save(virementErrone);

        log.info("Virement erroné créé avec succès - ID: {}, Statut: {}", saved.getId(), saved.getStatus());
        return virementErroneMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public VirementErroneResponseDto getById(Long id) {
        log.info("Récupération du virement erroné avec l'ID: {}", id);

        VirementErrone virementErrone = virementErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement erroné non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Virement erroné non trouvé avec l'ID: " + id);
                });

        log.debug("Virement erroné trouvé - Libellé: '{}', Montant: {}",
                virementErrone.getLibelle(), virementErrone.getMontant());
        return virementErroneMapper.toResponseDto(virementErrone);
    }

    @Transactional(readOnly = true)
    public Page<VirementErroneResponseDto> searchVirementErrone(VirementErroneSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de virements erronés - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<VirementErroneResponseDto> result = virementErroneRepository.findAll(
                        VirementErroneSpec.buildSpecification(searchDto), pageable)
                .map(virementErroneMapper::toResponseDto);

        log.info("Recherche terminée - {} virement(s) erroné(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Transactional
    public VirementErroneResponseDto updateVirementErrone(Long id, VirementErroneRequestDto requestDto) {
        log.info("Mise à jour du virement erroné ID: {} - Nouveau libellé: '{}'", id, requestDto.getLibelle());

        VirementErrone virementErrone = virementErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement erroné non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Virement erroné non trouvé avec l'ID: " + id);
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        virementErrone.setLibelle(requestDto.getLibelle());
        virementErrone.setMontant(requestDto.getMontant());
        virementErrone.setBanque(banque);
        virementErrone.setDateEmission(requestDto.getDateEmission());

        VirementErrone updated = virementErroneRepository.save(virementErrone);
        log.info("Virement erroné ID: {} mis à jour avec succès", id);
        return virementErroneMapper.toResponseDto(updated);
    }

    @Transactional
    public void deleteVirementErrone(Long id) {
        log.info("Suppression du virement erroné avec l'ID: {}", id);

        if (!virementErroneRepository.existsById(id)) {
            log.error("Virement erroné non trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Virement erroné non trouvé avec l'ID: " + id);
        }

        virementErroneRepository.deleteById(id);
        log.info("Virement erroné ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public EncoursVirementErroneDto getOutstandingVirementErrone(VirementErroneSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des virements erronés en cours - Critères: {}", searchDto);

        Specification<VirementErrone> spec = Specification.where(VirementErroneSpec.outstandingVirements())
                .and(VirementErroneSpec.hasBanque(searchDto.getBanqueId()))
                .and(VirementErroneSpec.hasLibelle(searchDto.getLibelle()))
                .and(VirementErroneSpec.hasMontant(searchDto.getMontant()));

        Page<VirementErrone> page = virementErroneRepository.findAll(spec, pageable);
        BigDecimal total = virementErroneRepository.findAll(spec).stream()
                .map(VirementErrone::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre de virements erronés: {}", total, page.getTotalElements());

        EncoursVirementErroneDto result = new EncoursVirementErroneDto();
        result.setPage(page.map(virementErroneMapper::toResponseDto));
        result.setTotalDesEncours(total);
        return result;
    }

    @Transactional
    public VirementErroneResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut du virement erroné ID: {}", id);

        VirementErrone virementErrone = virementErroneRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement erroné non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Virement erroné non trouvé avec l'ID: " + id);
                });

        Status oldStatus = virementErrone.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;

        virementErrone.setStatus(newStatus);
        VirementErrone updated = virementErroneRepository.save(virementErrone);

        log.info("Statut du virement erroné ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return virementErroneMapper.toResponseDto(updated);
    }
}