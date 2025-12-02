package staport.finance.gBanque.lcn.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cheques.dtos.ChequeRequestDto;
import staport.finance.gBanque.cheques.dtos.ChequeResponseDto;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.mappers.ChequeMapper;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.lcn.dtos.*;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.lcn.mapper.LcnMapper;
import staport.finance.gBanque.lcn.repositories.LcnRepository;
import staport.finance.gBanque.lcn.specs.LcnSpecification;
import staport.finance.gBanque.virements.enums.Type;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class LcnService {

    private static final Logger log = LoggerFactory.getLogger(LcnService.class);
    private final LcnRepository lcnRepository;
    private final BanqueRepository banqueRepository;

    public LcnService(LcnRepository lcnRepository, BanqueRepository banqueRepository) {
        this.lcnRepository = lcnRepository;
        this.banqueRepository = banqueRepository;
    }

    @Transactional
    public LcnResponseDTO createLcn(LcnRequestDto requestDto) {
        log.info("Creating new encaissement lcn: {}", requestDto);
        Banque banque = banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(() -> new ResourceNotFoundException("Bank with id " + requestDto.getBanqueId() + " not found."));
        Lcn lcn = LcnMapper.toEntity(requestDto,banque);
        log.debug("Saving lcn entity: {}", lcn);
        lcn = lcnRepository.save(lcn);
        log.info("Successfully created lcn with id: {}", lcn.getId());
        return LcnMapper.toDto(lcn);
    }

    @Transactional
    public LcnResponseDTO editEncaissementLcn(Long id, LcnRequestDto dto) {

        log.info("Editing encaissement lcn id={} with data={}", id, dto);
        Lcn existingLcn = lcnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lcn with id " + id + " not found."));

        if(existingLcn.getType()!=Type.ENCAISSEMENT){
            log.warn("Attempt to modify decaissement lcn with  id={}", id);
            throw new IllegalArgumentException("Modification interdite: le lcn with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!existingLcn.isActive()) {
            log.warn("Attempt to modify inactive lcn id={}", id);
            throw new IllegalArgumentException("Modification interdite: le lcn with id " + id + " est déja réglé.");
        }

        if(dto.getDateEmission()!=null){
            existingLcn.setDateEmission(dto.getDateEmission());
        }

        if(dto.getNumero()!=null){
            existingLcn.setNumero(dto.getNumero());
        }

        if(dto.getDateEcheance()!=null){
            existingLcn.setDateEcheance(dto.getDateEcheance());
        }
        if (dto.getBanqueId() != null) {
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found."));
            existingLcn.setBanque(banque);
        }

        if(dto.getMontant()!=null){
            existingLcn.setMontant(dto.getMontant());
        }

        if(dto.getEmetteur()!=null){
            existingLcn.setEmetteur(dto.getEmetteur());
        }

        existingLcn=lcnRepository.save(existingLcn);
        return LcnMapper.toDto(existingLcn);

    }

    @Transactional(readOnly = true)
    public LcnResponseDTO getLcnById(Long id) {
        log.info("Fetching lcn with id={}", id);
        Lcn lcn = lcnRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("lcn with id " + id + " not found."));
        return LcnMapper.toDto(lcn);
    }

    @Transactional
    public void deleteLcn(Long id) {
        log.info("Deleting lcn with id={}", id);

        Lcn lcn=lcnRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("lcn with id " + id + " not found."));

        if (lcn.getType() != Type.ENCAISSEMENT) {
            log.warn("Attempt to delete non-encaissement lcn id={}", id);
            throw new IllegalArgumentException("Suppression interdite: le lcn with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!lcn.isActive()) {
            log.warn("Attempt to delete inactive lcn id={}", id);
            throw new IllegalArgumentException("Suppression interdite: le lcn with id " + id + " est déja réglé.");
        }
        lcnRepository.deleteById(id);
        log.info("Successfully deleted lcn id={}", id);

    }

    public Page<LcnResponseDTO> getDecaissements(LcnSearchDTO searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()))
                .and(LcnSpecification.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }

    public Page<LcnResponseDTO> getEncaissements(LcnSearchDTO searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Lcn> spec = Specification.where(LcnSpecification.hasType(Type.ENCAISSEMENT))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasBanqueNom(searchDto.getNomBanque()))
                .and(LcnSpecification.isActive(searchDto.getActive()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));
        // Execute the search with the combined specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);

        // Map the result Page<Lcn> to Page<LcnResponseDTO>
        return lcnPage.map(LcnMapper::toDto);
    }

    @Transactional
    public LcnResponseDTO toggleLcnStatus(Long  id) {
        Lcn lcn = lcnRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Lcn with id " + id + " not found."));

        // Toggle the 'active' status
        lcn.setActive(!lcn.isActive());

        // Save the updated lcn
        lcn = lcnRepository.save(lcn);

        // Map and return the updated DTO
        return LcnMapper.toDto(lcn);
    }


    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursDecaissementsForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active LCN for the specified bank in the current month
        Specification<Lcn> spec = LcnSpecification.encoursDecaissement(banqueId)
                .and(LcnSpecification.hasNumero(searchDto.getNumero()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(LcnMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursLcnResponse(totalDesEncours,result);
    }

    @Transactional(readOnly = true)
    public EncoursLcnResponse getEncoursEncaissementsForLcn(EncoursLcnSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active LCN for the specified bank in the current month
        Specification<Lcn> spec = LcnSpecification.encoursEncaissement(banqueId)
                .and(LcnSpecification.hasEmetteur(searchDto.getEmetteur()))
                .and(LcnSpecification.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(LcnSpecification.hasMontant(searchDto.getMontant()))
                .and(LcnSpecification.hasDateEcheance(searchDto.getDateEcheance()))
                .and(LcnSpecification.hasDateEmission(searchDto.getDateEmission()))
                .and(LcnSpecification.hasNumero(searchDto.getNumero()));

        // Execute the search with the specification and pagination
        Page<Lcn> lcnPage = lcnRepository.findAll(spec, pageable);
        BigDecimal totalDesEncours = lcnRepository.findAll(spec).stream().map(Lcn::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        Page<LcnResponseDTO> result = lcnPage.map(LcnMapper::toDto);
        // Map the result to DTOs and return
        return new EncoursLcnResponse(totalDesEncours,result);
    }
}