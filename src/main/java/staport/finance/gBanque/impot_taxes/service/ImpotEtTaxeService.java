package staport.finance.gBanque.impot_taxes.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.impot_taxes.dtos.*;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;
import staport.finance.gBanque.impot_taxes.mapper.ImpotEtTaxeMapper;
import staport.finance.gBanque.impot_taxes.repositories.ImpotEtTaxeRepository;
import staport.finance.gBanque.impot_taxes.specs.ImpotEtTaxeSpecifications;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class ImpotEtTaxeService {

    private static final Logger log = LogManager.getLogger(ImpotEtTaxeService.class);
    private final ImpotEtTaxeRepository impotEtTaxeRepository;
    private final BanqueRepository banqueRepository;
    private final ImpotEtTaxeMapper impotEtTaxeMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public ImpotEtTaxeService(ImpotEtTaxeRepository impotEtTaxeRepository,
                              BanqueRepository banqueRepository,
                              ImpotEtTaxeMapper impotEtTaxeMapper) {
        this.impotEtTaxeRepository = impotEtTaxeRepository;
        this.banqueRepository = banqueRepository;
        this.impotEtTaxeMapper = impotEtTaxeMapper;
    }

    @Transactional
    public ImpotEtTaxeResponseDto createImpotEtTaxe(ImpotEtTaxeRequestDto requestDto) {
        log.info("Début de création d'un impôt/taxe avec les données: {}", requestDto);

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable avec l'id={}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque introuvable");
                });

        ImpotEtTaxe impotEtTaxe = impotEtTaxeMapper.toEntity(requestDto, banque);
        impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Impôt/taxe créé avec succès: id={}", impotEtTaxe.getId());
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    @Transactional(readOnly = true)
    public ImpotEtTaxeResponseDto getById(Long id) {
        log.info("Récupération de l'impôt/taxe id={}", id);
        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Impôt/taxe introuvable"));
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    @Transactional(readOnly = true)
    public Page<ImpotEtTaxeResponseDto> searchImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Recherche d'impôts/taxes - Critères: {}", searchDto);
        Page<ImpotEtTaxeResponseDto> result = impotEtTaxeRepository.findAll(ImpotEtTaxeSpecifications.buildSpecification(searchDto), pageable)
                .map(impotEtTaxeMapper::toResponseDto);
        log.info("Recherche terminée: {} éléments trouvés", result.getTotalElements());
        return result;
    }

    @Transactional
    public ImpotEtTaxeResponseDto updateImpotEtTaxe(Long id, ImpotEtTaxeRequestDto requestDto) {
        log.info("Mise à jour de l'impôt/taxe id={}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Impôt/taxe introuvable"));

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> new IllegalArgumentException("Banque introuvable"));

        impotEtTaxe.setLibelle(requestDto.getLibelle());
        impotEtTaxe.setMontant(requestDto.getMontant());
        impotEtTaxe.setBanque(banque);
        impotEtTaxe.setMotif(requestDto.getMotif());
        impotEtTaxe.setDateEmission(requestDto.getDateEmission());
        impotEtTaxe.setObservation(requestDto.getObservation());

        impotEtTaxeRepository.save(impotEtTaxe);
        log.info("Impôt/taxe mis à jour avec succès: id={}", id);
        return impotEtTaxeMapper.toResponseDto(impotEtTaxe);
    }

    @Transactional
    public void deleteImpotEtTaxe(Long id) {
        log.info("Suppression de l'impôt/taxe id={}", id);
        if (!impotEtTaxeRepository.existsById(id)) {
            throw new IllegalArgumentException("Impôt/taxe introuvable");
        }
        impotEtTaxeRepository.deleteById(id);
        log.info("Impôt/taxe supprimé avec succès: id={}", id);
    }

    @Transactional
    public ImpotEtTaxeResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut de l'impôt/taxe ID: {}", id);

        ImpotEtTaxe impotEtTaxe = impotEtTaxeRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Impôt/taxe introuvable avec l'id={}", id);
                    return new IllegalArgumentException("Impôt/taxe introuvable");
                });

        Status oldStatus = impotEtTaxe.getStatus();
        Status newStatus = (oldStatus == Status.PAYE) ? Status.PREVU : Status.PAYE;

        impotEtTaxe.setStatus(newStatus);
        ImpotEtTaxe updated = impotEtTaxeRepository.save(impotEtTaxe);

        log.info("Statut basculé pour ID {}: {} -> {}", id, oldStatus, newStatus);
        return impotEtTaxeMapper.toResponseDto(updated);
    }

    @Transactional(readOnly = true)
    public EncoursImpotEtTaxeDto getOutstandingImpotEtTaxes(ImpotEtTaxeSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des encours impôts/taxes - Critères: {}", searchDto);

        Specification<ImpotEtTaxe> spec = Specification.where(ImpotEtTaxeSpecifications.outstandingUntil(endOfMonth))
                .and(ImpotEtTaxeSpecifications.hasBanque(searchDto.getBanqueId()))
                .and(ImpotEtTaxeSpecifications.hasLibelle(searchDto.getLibelle()))
                .and(ImpotEtTaxeSpecifications.hasMotif(searchDto.getMotif()))
                .and(ImpotEtTaxeSpecifications.hasMontant(searchDto.getMontant()));

        Page<ImpotEtTaxe> impotEtTaxePage = impotEtTaxeRepository.findAll(spec, pageable);

        BigDecimal totalDesEncours = impotEtTaxeRepository.findAll(spec).stream()
                .map(ImpotEtTaxe::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Page<ImpotEtTaxeResponseDto> pageDto = impotEtTaxePage.map(impotEtTaxeMapper::toResponseDto);

        EncoursImpotEtTaxeDto result = new EncoursImpotEtTaxeDto();
        result.setPage(pageDto);
        result.setTotalDesEncours(totalDesEncours);

        log.info("Encours récupérés: {} éléments, Total: {}", pageDto.getTotalElements(), totalDesEncours);
        return result;
    }
}