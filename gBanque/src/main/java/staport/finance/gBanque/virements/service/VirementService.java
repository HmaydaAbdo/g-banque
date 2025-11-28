package staport.finance.gBanque.virements.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.virements.dtos.*;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.enums.Type;
import staport.finance.gBanque.virements.mapper.VirementMapper;
import staport.finance.gBanque.virements.repositories.VirementRepository;
import staport.finance.gBanque.virements.specs.VirementSpec;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class VirementService {

    private static final Logger log = LoggerFactory.getLogger(VirementService.class);
    private final VirementRepository virementRepository;
    private final BanqueRepository banqueRepository;
    private final VirementMapper virementMapper;


    public VirementService(VirementRepository virementRepository, BanqueRepository banqueRepository, VirementMapper virementMapper) {
        this.virementRepository = virementRepository;
        this.banqueRepository = banqueRepository;
        this.virementMapper = virementMapper;
    }

    @Transactional
    public VirementResponseDto createEncaissementVirement(VirementRequestDto dto) {
        log.info("Creating new encaissement virement: {}", dto);

        Virement virement = this.virementMapper.toEntity(dto);
        virement.setActive(true);

        log.debug("Saving virement entity: {}", virement);
        virement = virementRepository.save(virement);

        log.info("Successfully created virement with id: {}", virement.getId());
        return this.virementMapper.toDto(virement);
    }

    @Transactional
    public VirementResponseDto editEncaissementVirement(Long id, VirementRequestDto dto) {
        log.info("Editing encaissement virement id={} with data={}", id, dto);

        Virement existingVirement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement with id {} not found", id);
                    return new NoSuchElementException("Virement with id " + id + " not found.");
                });

        if (existingVirement.getType() != Type.ENCAISSEMENT) {
            log.warn("Attempt to modify non-encaissement virement id={}", id);
            throw new IllegalArgumentException("Modification interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!existingVirement.isActive()) {
            log.warn("Attempt to modify inactive virement id={}", id);
            throw new IllegalArgumentException("Modification interdite: le virement with id " + id + " est déja réglé.");
        }

        if (dto.getMontant() != null) {
            log.debug("Updating montant for virement id={}", id);
            existingVirement.setMontant(dto.getMontant());
        }
        if (dto.getDateReception() != null) {
            log.debug("Updating dateReception for virement id={}", id);
            existingVirement.setDateEmission(dto.getDateReception());
        }
        if (dto.getEmetteur() != null) {
            log.debug("Updating emetteur for virement id={}", id);
            existingVirement.setEmetteur(dto.getEmetteur());
        }
        if (dto.getBanqueBeneficiaireId() != null) {
            log.debug("Updating banqueBeneficiaire for virement id={}", id);
            Banque banque = banqueRepository.findById(dto.getBanqueBeneficiaireId())
                    .orElseThrow(() -> {
                        log.error("Banque with id {} not found", dto.getBanqueBeneficiaireId());
                        return new ResourceNotFoundException("Banque with id: " + dto.getBanqueBeneficiaireId() + " not found");
                    });
            existingVirement.setBanqueBeneficiare(banque);
        }

        existingVirement = virementRepository.save(existingVirement);
        log.info("Successfully updated virement id={}", id);
        return this.virementMapper.toDto(existingVirement);
    }

    @Transactional
    public VirementResponseDto getVirementById(Long id) {
        log.info("Fetching virement with id={}", id);

        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement with id {} not found", id);
                    return new ResourceNotFoundException("Virement with id " + id + " not found.");
                });

        log.debug("Found virement: {}", virement);
        return this.virementMapper.toDto(virement);
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getEncaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.info("Searching encaissement virements with filters={} pageable={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.ENCAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        log.info("Found {} encaissement virements", virementPage.getTotalElements());
        return virementPage.map(this.virementMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getDecaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.info("Searching decaissement virements with filters={} pageable={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.DECAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        log.info("Found {} decaissement virements", virementPage.getTotalElements());
        return virementPage.map(this.virementMapper::toDto);
    }

    @Transactional
    public VirementResponseDto toggleVirementStatus(Long id) {
        log.info("Toggling status for virement id={}", id);

        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement with id {} not found", id);
                    return new NoSuchElementException("Virement with id " + id + " not found.");
                });

        virement.setActive(!virement.isActive());
        log.debug("New status for virement id={} isActive={}", id, virement.isActive());

        virement = virementRepository.save(virement);

        log.info("Status toggled successfully for virement id={}", id);
        return this.virementMapper.toDto(virement);
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursDecaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Fetching encours decaissement for banqueId={} with filters={}", banqueId, searchDto);

        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId)
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total encours decaissement for banqueId={} is {}", banqueId, totalDesEncours);

        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this.virementMapper::toDto));
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursEncaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Fetching encours encaissement for banqueId={} with filters={}", banqueId, searchDto);

        Banque banqueBeneficiaire = banqueRepository.findById(banqueId)
                .orElseThrow(() -> {
                    log.error("Banque with id {} not found", banqueId);
                    return new NoSuchElementException("Bank with id " + banqueId + " not found.");
                });

        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()))
                .and(VirementSpec.hasEmetteur(searchDto.getEmetteur()));

        Page<Virement> virementPage = virementRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Total encours encaissement for banqueId={} is {}", banqueId, totalDesEncours);

        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this.virementMapper::toDto));
    }

    @Transactional
    public void deleteEncaissementVirement(Long id) {
        log.info("Deleting encaissement virement id={}", id);

        Virement virement = virementRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Virement with id {} not found", id);
                    return new NoSuchElementException("Virement with id " + id + " not found.");
                });

        if (virement.getType() != Type.ENCAISSEMENT) {
            log.warn("Attempt to delete non-encaissement virement id={}", id);
            throw new IllegalArgumentException("Suppression interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!virement.isActive()) {
            log.warn("Attempt to delete inactive virement id={}", id);
            throw new IllegalArgumentException("Suppression interdite: le virement with id " + id + " est déja réglé.");
        }

        virementRepository.delete(virement);
        log.info("Successfully deleted virement id={}", id);
    }
}
