package staport.finance.gBanque.miseADisposition.services;

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
import staport.finance.gBanque.miseADisposition.dtos.*;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.miseADisposition.mapper.MiseADispositionMapper;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.virements.enums.Type;
import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class MiseADispositionService {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionService.class);
    private final MiseADispositionRepository miseADispositionRepository;
    private final BanqueRepository banqueRepository;

    public MiseADispositionService(MiseADispositionRepository miseADispositionRepository, BanqueRepository banqueRepository) {
        this.miseADispositionRepository = miseADispositionRepository;
        this.banqueRepository = banqueRepository;
    }

    @Transactional()
    public MiseADispositionResponseDto createEncaissementMiseAdisposition(MiseAdispositionRequestDto dto) {
        log.info("Creating new encaissement mise à disposition: {}", dto);

        Banque banque = banqueRepository.findById(dto.getBanqueId()).orElseThrow(() -> new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found."));
        MiseADisposition miseADisposition = MiseADispositionMapper.toEntity(dto,banque);

        log.debug("Saving Mise à disposition entity: {}", miseADisposition);
        miseADisposition = miseADispositionRepository.save(miseADisposition);

        log.info("Successfully created mise à disposition  with id: {}", miseADisposition.getId());
        return MiseADispositionMapper.toDto(miseADisposition);
    }

    @Transactional
    public MiseADispositionResponseDto editEncaissementMiseAdisposition(Long id, MiseAdispositionRequestDto dto) {

        log.info("Editing encaissement MiseAdispositionRequestDto id={} with data={}", id, dto);
        MiseADisposition existingMiseAdisposition = miseADispositionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("MiseAdisposition with id " + id + " not found."));

        if(existingMiseAdisposition.getType()!=Type.ENCAISSEMENT){
            log.warn("Attempt to modify decaissement MiseADisposition with  id={}", id);
            throw new IllegalArgumentException("Modification interdite: la MiseADisposition with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!existingMiseAdisposition.isActive()) {
            log.warn("Attempt to modify inactive MiseADisposition id={}", id);
            throw new IllegalArgumentException("Modification interdite: la MiseADisposition with id " + id + " est déja réglé.");
        }

        if(dto.getDateEmission()!=null){
            existingMiseAdisposition.setDateEmission(dto.getDateEmission());
        }

        if (dto.getBanqueId() != null) {
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found."));
            existingMiseAdisposition.setBanque(banque);
        }

        if(dto.getMontant()!=null){
            existingMiseAdisposition.setMontant(dto.getMontant());
        }

        if(dto.getEmetteur()!=null){
            existingMiseAdisposition.setEmetteur(dto.getEmetteur());
        }

        existingMiseAdisposition=miseADispositionRepository.save(existingMiseAdisposition);
        return MiseADispositionMapper.toDto(existingMiseAdisposition);

    }

    @Transactional(readOnly = true)
    public MiseADispositionResponseDto getMiseAdispositionById(Long id) {
        log.info("Fetching MiseADisposition with id={}", id);
        MiseADisposition miseADisposition = miseADispositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mise à disposition with id " + id + " not found."));
        return MiseADispositionMapper.toDto(miseADisposition);
    }

    @Transactional
    public void deleteMiseAdispositionById(Long id) {
        log.info("Deleting Mise à  disposition  with id={}", id);

        MiseADisposition miseADisposition=miseADispositionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Mise à disposition  with id " + id + " not found."));

        if (miseADisposition.getType() != Type.ENCAISSEMENT) {
            log.warn("Attempt to delete non-encaissement mise A Disposition id={}", id);
            throw new IllegalArgumentException("Suppression interdite: la Mise à dispo  with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!miseADisposition.isActive()) {
            log.warn("Attempt to delete inactive mise à dispo id={}", id);
            throw new IllegalArgumentException("Suppression interdite: la mise à dispo  with id " + id + " est déja réglé.");
        }
        miseADispositionRepository.deleteById(id);
        log.info("Successfully deleted mise à dispo  id={}", id);

    }


    public Page<MiseADispositionResponseDto> getDecaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }

    public Page<MiseADispositionResponseDto> getEncaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        // Build the combined specification using the static methods from MiseADispositionSpec.
        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT));

        // Execute the search with the combined specification and pagination.
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);

        // Map the resulting Page<MiseADisposition> to Page<MiseADispositionResponseDto>.
        return miseADispositionPage.map(MiseADispositionMapper::toDto);
    }

    @Transactional
    public MiseADispositionResponseDto toggleMiseADispositionStatus(Long id) {
        MiseADisposition miseADisposition = miseADispositionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("MiseADisposition with id " + id + " not found."));

        // Toggle the 'active' status.
        miseADisposition.setActive(!miseADisposition.isActive());

        // Save the updated entity.
        miseADisposition = miseADispositionRepository.save(miseADisposition);

        // Map and return the updated DTO.
        return MiseADispositionMapper.toDto(miseADisposition);
    }


    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursDecaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active mise à disposition for the specified bank in the current month
        Specification<MiseADisposition> spec = MiseADispositionSpec.encoursDecaissementForMiseAdispo(banqueId)
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the specification and pagination
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<MiseADispositionResponseDto> result = miseADispositionPage.map(MiseADispositionMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursMiseAdisposition(totalDesEncours,result);
    }

    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursEncaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active mise à disposition for the specified bank in the current month
        Specification<MiseADisposition> spec = MiseADispositionSpec.encoursEncaissementForMiseAdispo(banqueId)
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT))
                .and(MiseADispositionSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(MiseADispositionSpec.hasDateEmission(searchDto.getDateEmission()));

        // Execute the search with the specification and pagination
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<MiseADispositionResponseDto> result = miseADispositionPage.map(MiseADispositionMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursMiseAdisposition(totalDesEncours,result);
    }
}
