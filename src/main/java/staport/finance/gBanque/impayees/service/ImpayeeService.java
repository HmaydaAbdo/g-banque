package staport.finance.gBanque.impayees.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.impayees.dtos.*;
import staport.finance.gBanque.impayees.entities.Impayee;
import staport.finance.gBanque.impayees.mapper.ImpayeeMapper;
import staport.finance.gBanque.impayees.repository.ImpayeeRepository;
import staport.finance.gBanque.impayees.specs.ImpayeeSpecifications;
import staport.finance.gBanque.prefinancement.enums.Status;
import java.math.BigDecimal;

@Service
public class ImpayeeService {

    private static final Logger log = LogManager.getLogger(ImpayeeService.class);
    private final ImpayeeRepository impayeeRepository;
    private final BanqueRepository banqueRepository;
    private final ImpayeeMapper impayeeMapper;

    public ImpayeeService(ImpayeeRepository impayeeRepository, BanqueRepository banqueRepository, ImpayeeMapper impayeeMapper) {
        this.impayeeRepository = impayeeRepository;
        this.banqueRepository = banqueRepository;
        this.impayeeMapper = impayeeMapper;
    }

    @Transactional
    public ImpayeeResponseDto createImpayee(ImpayeeRequestDto requestDto) {
        log.info("Début de création d'une impayée avec les données: {}", requestDto);

        Banque banqueEncaissement = banqueRepository.findById(requestDto.getBanqueEncaissementId())
                .orElseThrow(() -> {
                    log.error("Banque d'encaissement introuvable avec l'id={}", requestDto.getBanqueEncaissementId());
                    return new IllegalArgumentException("Banque d'encaissement introuvable");
                });

        Impayee impayee = impayeeMapper.toEntity(requestDto, banqueEncaissement);
        impayeeRepository.save(impayee);

        log.info("Impayée créée avec succès: id={}", impayee.getId());
        return impayeeMapper.toResponseDto(impayee);
    }

    @Transactional(readOnly = true)
    public ImpayeeResponseDto getById(Long id) {
        log.info("Récupération de l'impayée id={}", id);
        Impayee impayee = impayeeRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Impayée introuvable"));
        return impayeeMapper.toResponseDto(impayee);
    }

    @Transactional(readOnly = true)
    public Page<ImpayeeResponseDto> searchImpayees(ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'impayées - Critères: {}", searchDto);
        Page<ImpayeeResponseDto> result = impayeeRepository.findAll(ImpayeeSpecifications.buildSpecification(searchDto), pageable)
                .map(impayeeMapper::toResponseDto);
        log.info("Recherche terminée: {} éléments trouvés", result.getTotalElements());
        return result;
    }

    @Transactional
    public ImpayeeResponseDto updateImpayee(Long id, ImpayeeRequestDto requestDto) {
        log.info("Mise à jour de l'impayée id={}", id);

        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Impayée introuvable"));

        Banque banqueEncaissement = banqueRepository.findById(requestDto.getBanqueEncaissementId())
                .orElseThrow(() -> new IllegalArgumentException("Banque d'encaissement introuvable"));

        impayee.setLibelle(requestDto.getLibelle());
        impayee.setMontant(requestDto.getMontant());
        impayee.setBanqueEncaissement(banqueEncaissement);
        impayee.setMotif(requestDto.getMotif());
        impayee.setDateEmission(requestDto.getDateEmission());
        impayee.setObservation(requestDto.getObservation());

        impayeeRepository.save(impayee);
        log.info("Impayée mise à jour avec succès: id={}", id);
        return impayeeMapper.toResponseDto(impayee);
    }

    @Transactional
    public void deleteImpayee(Long id) {
        log.info("Suppression de l'impayée id={}", id);

        Impayee impayee=impayeeRepository.findById(id).orElseThrow(()->{
            log.error("Impayer avec l'id {} est introuvable ",id);
            return new RuntimeException("Impayée avec id " +id +" est introuvable ");
        });

        if(impayee.getStatus()!=Status.PREVU){
            log.error("l'impayée avec l'id {} est déja réglé ",id);
            throw new RuntimeException("l'Impayer avec l'id "+id+" est déja réglé");
        }else{
            impayeeRepository.deleteById(id);
        }
        log.info("Impayée supprimée avec succès: id={}", id);
    }

    @Transactional
    public ImpayeeResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'impayée ID: {}", id);

        Impayee impayee = impayeeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impayée introuvable avec l'id={}", id);
                    return new IllegalArgumentException("Impayée introuvable");
                });

        Status oldStatus = impayee.getStatus();
        Status newStatus = (oldStatus == Status.PAYE) ? Status.PREVU : Status.PAYE;

        impayee.setStatus(newStatus);
        Impayee updated = impayeeRepository.save(impayee);

        log.info("Statut basculé pour ID {}: {} -> {}", id, oldStatus, newStatus);
        return impayeeMapper.toResponseDto(updated);
    }

    @Transactional(readOnly = true)
    public EncoursImpayeeDto getOutstandingImpayees(ImpayeeSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des encours impayées - Critères: {}", searchDto);

        Specification<Impayee> spec = Specification.where(ImpayeeSpecifications.outstandingImpayees())
                .and(ImpayeeSpecifications.hasBanqueEncaissement(searchDto.getBanqueEncaissementId()))
                .and(ImpayeeSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(ImpayeeSpecifications.hasMotif(searchDto.getMotif()))
                .and(ImpayeeSpecifications.hasMontant(searchDto.getMontant()));

        Page<Impayee> impayeePage = impayeeRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = impayeeRepository.findAll(spec).stream()
                .map(Impayee::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<ImpayeeResponseDto> pageDto = impayeePage.map(impayeeMapper::toResponseDto);

        EncoursImpayeeDto result = new EncoursImpayeeDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours récupérés: {} éléments, Total: {}", pageDto.getTotalElements(), totalDesEncours);
        return result;
    }
}