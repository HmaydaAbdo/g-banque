package staport.finance.gBanque.cheques.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cheques.dtos.*;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.mappers.ChequeMapper;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.cheques.specs.ChequeSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.virements.enums.Type;
import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Service
public class ChequeService {

    private static final Logger log = LoggerFactory.getLogger(ChequeService.class);
    private final ChequeRepository chequeRepository;
    private final BanqueRepository banqueRepository;

    public ChequeService(ChequeRepository chequeRepository, BanqueRepository banqueRepository) {
        this.chequeRepository = chequeRepository;
        this.banqueRepository = banqueRepository;
    }


    @Transactional
    public ChequeResponseDto createEncaissementCheque(ChequeRequestDto dto) {
        log.info("Creating new encaissement cheque: {}", dto);

        Banque banque = banqueRepository.findById(dto.getBanqueId()).orElseThrow(() -> new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found."));
        Cheque cheque = ChequeMapper.toEntity(dto,banque);

        log.debug("Saving cheque entity: {}", cheque);
        cheque = chequeRepository.save(cheque);

        log.info("Successfully created cheque with id: {}", cheque.getId());
        return ChequeMapper.toDto(cheque);
    }

    @Transactional
    public ChequeResponseDto editEncaissementCheque(Long id, ChequeRequestDto dto) {

        log.info("Editing encaissement cheque id={} with data={}", id, dto);
        Cheque existingCheque = chequeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cheque with id " + id + " not found."));

        if(existingCheque.getType()!=Type.ENCAISSEMENT){
            log.warn("Attempt to modify decaissement cheque with  id={}", id);
            throw new IllegalArgumentException("Modification interdite: le cheque with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!existingCheque.isActive()) {
            log.warn("Attempt to modify inactive cheque id={}", id);
            throw new IllegalArgumentException("Modification interdite: le cheque with id " + id + " est déja réglé.");
        }

        if (dto.getNumero() != null) {
            existingCheque.setNumero(dto.getNumero());
        }

        if(dto.getDateEmission()!=null){
            existingCheque.setDateEmission(dto.getDateEmission());
        }

        if(dto.getDateEcheance()!=null){
            existingCheque.setDateEcheance(dto.getDateEcheance());
        }
         if (dto.getBanqueId() != null) {
            Banque banque = banqueRepository.findById(dto.getBanqueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bank with id " + dto.getBanqueId() + " not found."));
            existingCheque.setBanque(banque);
         }

         if(dto.getMontant()!=null){
             existingCheque.setMontant(dto.getMontant());
         }

         if(dto.getEmetteur()!=null){
             existingCheque.setEmetteur(dto.getEmetteur());
         }

       existingCheque=chequeRepository.save(existingCheque);
       return ChequeMapper.toDto(existingCheque);

    }

    @Transactional(readOnly = true)
    public ChequeResponseDto getChequeById(Long id) {
        log.info("Fetching cheque with id={}", id);
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cheque with id " + id + " not found."));
        return ChequeMapper.toDto(cheque);
    }

    @Transactional
    public void deleteCheque(Long id) {
        log.info("Deleting cheque with id={}", id);

        Cheque cheque=chequeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cheque with id " + id + " not found."));

        if (cheque.getType() != Type.ENCAISSEMENT) {
            log.warn("Attempt to delete non-encaissement cheque id={}", id);
            throw new IllegalArgumentException("Suppression interdite: le cheque with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (!cheque.isActive()) {
            log.warn("Attempt to delete inactive cheque id={}", id);
            throw new IllegalArgumentException("Suppression interdite: le cheque with id " + id + " est déja réglé.");
        }
        chequeRepository.deleteById(id);
        log.info("Successfully deleted cheque id={}", id);

    }

    public Page<ChequeResponseDto> getAllDecaissement(ChequeSearchDto searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Cheque> spec = Specification.where(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.isActive(searchDto.getActive()))
                .and(ChequeSpec.hasType(Type.DECAISSEMENT));

        // Execute the search with the combined specification and pagination
        Page<Cheque> chequePage = chequeRepository.findAll(spec, pageable);

        // Map the result Page<Cheque> to Page<ChequeResponseDto>
        return chequePage.map(ChequeMapper::toDto);
    }

    public Page<ChequeResponseDto> getAllEncaissement(ChequeSearchDto searchDto, Pageable pageable) {
        // Combine specifications based on non-null search criteria from the DTO
        Specification<Cheque> spec = Specification.where((ChequeSpec.hasType(Type.ENCAISSEMENT))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.isActive(searchDto.getActive()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur())))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()));

        // Execute the search with the combined specification and pagination
        Page<Cheque> chequePage = chequeRepository.findAll(spec, pageable);

        // Map the result Page<Cheque> to Page<ChequeResponseDto>
        return chequePage.map(ChequeMapper::toDto);
    }

    @Transactional
    public ChequeResponseDto toggleChequeStatus(Long id) {
        Cheque cheque = chequeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Cheque with id " + id + " not found."));

        // Toggle the 'active' status
        cheque.setActive(!cheque.isActive());

        // Save the updated cheque
        cheque = chequeRepository.save(cheque);

        // Map and return the updated DTO
        return ChequeMapper.toDto(cheque);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursDecaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active cheques for the specified bank in the current month
        Specification<Cheque> spec = ChequeSpec.encoursDecaissements(banqueId)
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()));

        // Execute the search with the specification and pagination
        Page<Cheque> page = chequeRepository.findAll(spec, pageable);
        Page<ChequeResponseDto> chequePage = page.map(ChequeMapper::toDto);
        BigDecimal total=chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Map the result to DTOs and return
        return new EncoursChequeResponseDto(total,chequePage);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursEncaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        // Build the specification for active cheques for the specified bank in the current month
        Specification<Cheque> spec = ChequeSpec.encoursEncaissements(banqueId)
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()));

        // Execute the search with the specification and pagination
        Page<Cheque> page = chequeRepository.findAll(spec, pageable);
        Page<ChequeResponseDto> chequePage = page.map(ChequeMapper::toDto);
        BigDecimal total=chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        // Map the result to DTOs and return
        return new EncoursChequeResponseDto(total,chequePage);
    }
}