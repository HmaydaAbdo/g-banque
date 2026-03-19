package staport.finance.gBanque.fraisBancaires.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.fraisBancaires.dtos.*;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.mapper.FraisBancaireMapper;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.fraisBancaires.spec.FraisBancaireSpec;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Service
public class FraisBancaireService {

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireService.class);
    private final FraisBancaireRepository fraisBancaireRepository;
    private final BanqueRepository banqueRepository;
    private final FraisBancaireMapper fraisBancaireMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public FraisBancaireService(FraisBancaireRepository fraisBancaireRepository,
                                BanqueRepository banqueRepository,
                                FraisBancaireMapper fraisBancaireMapper) {
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.banqueRepository = banqueRepository;
        this.fraisBancaireMapper = fraisBancaireMapper;
        log.info("Initialisation du service FraisBancaire");
    }

    @Transactional
    public FraisBancaireResposneDto createFraisBancaire(FraisBancaireRequestDto requestDto) {
        log.info("Création d'un frais bancaire - Banque ID: {}, Libellé: '{}', Montant: {}",
                requestDto.getBanqueId(), requestDto.getLibelle(), requestDto.getMontant());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        log.debug("Banque trouvée: {} (ID: {})", banque.getNom(), banque.getId());
        FraisBancaire fraisBancaire = fraisBancaireMapper.toEntity(requestDto, banque);
        FraisBancaire saved = fraisBancaireRepository.save(fraisBancaire);

        log.info("Frais bancaire créé avec succès - ID: {}, Statut: {}", saved.getId(), saved.getStatus());
        return fraisBancaireMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public FraisBancaireResposneDto getById(Long id) {
        log.info("Récupération du frais bancaire avec l'ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Frais bancaire non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Frais bancaire non trouvé avec l'ID: " + id);
                });

        log.debug("Frais bancaire trouvé - Libellé: '{}', Montant: {}", fraisBancaire.getLibelle(), fraisBancaire.getMontant());
        return fraisBancaireMapper.toResponseDto(fraisBancaire);
    }

    @Transactional(readOnly = true)
    public Page<FraisBancaireResposneDto> searchFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de frais bancaires - Critères: {}, Page: {}, Taille: {}",
                searchDto, pageable.getPageNumber(), pageable.getPageSize());

        Page<FraisBancaireResposneDto> result = fraisBancaireRepository.findAll(
                        FraisBancaireSpec.buildSpecification(searchDto), pageable)
                .map(fraisBancaireMapper::toResponseDto);

        log.info("Recherche terminée - {} frais bancaire(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Transactional
    public FraisBancaireResposneDto updateFraisBancaire(Long id, FraisBancaireRequestDto requestDto) {
        log.info("Mise à jour du frais bancaire ID: {} - Nouveau libellé: '{}'", id, requestDto.getLibelle());

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Frais bancaire non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Frais bancaire non trouvé avec l'ID: " + id);
                });

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque non trouvée avec l'ID: {}", requestDto.getBanqueId());
                    return new IllegalArgumentException("Banque non trouvée avec l'ID: " + requestDto.getBanqueId());
                });

        fraisBancaire.setLibelle(requestDto.getLibelle());
        fraisBancaire.setMontant(requestDto.getMontant());
        fraisBancaire.setBanque(banque);
        fraisBancaire.setDateEmission(requestDto.getDateEmission());

        FraisBancaire updated = fraisBancaireRepository.save(fraisBancaire);
        log.info("Frais bancaire ID: {} mis à jour avec succès", id);
        return fraisBancaireMapper.toResponseDto(updated);
    }

    @Transactional
    public void deleteFraisBancaire(Long id) {
        log.info("Suppression du frais bancaire avec l'ID: {}", id);

        if (!fraisBancaireRepository.existsById(id)) {
            log.error("Frais bancaire non trouvé avec l'ID: {}", id);
            throw new IllegalArgumentException("Frais bancaire non trouvé avec l'ID: " + id);
        }

        fraisBancaireRepository.deleteById(id);
        log.info("Frais bancaire ID: {} supprimé avec succès", id);
    }

    @Transactional(readOnly = true)
    public EncoursFraisBancaireDto getOutstandingFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable) {
        log.info("Récupération des frais bancaires en cours - Critères: {}", searchDto);

        Specification<FraisBancaire> spec = Specification.where(FraisBancaireSpec.outstandingUntil(endOfMonth))
                .and(FraisBancaireSpec.hasBanque(searchDto.getBanqueId()))
                .and(FraisBancaireSpec.hasLibelle(searchDto.getLibelle()))
                .and(FraisBancaireSpec.hasMontant(searchDto.getMontant()));

        Page<FraisBancaire> page = fraisBancaireRepository.findAll(spec, pageable);
        BigDecimal total = fraisBancaireRepository.findAll(spec).stream()
                .map(FraisBancaire::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre de frais: {}", total, page.getTotalElements());

        EncoursFraisBancaireDto result = new EncoursFraisBancaireDto();
        result.setPage(page.map(fraisBancaireMapper::toResponseDto));
        result.setTotalDesEncours(total);
        return result;
    }

    @Transactional
    public FraisBancaireResposneDto toggleStatus(Long id) {
        log.info("Basculement du statut du frais bancaire ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Frais bancaire non trouvé avec l'ID: {}", id);
                    return new IllegalArgumentException("Frais bancaire non trouvé avec l'ID: " + id);
                });

        Status oldStatus = fraisBancaire.getStatus();
        Status newStatus=oldStatus==Status.PAYE ? Status.PREVU : Status.PAYE;
        fraisBancaire.setStatus(newStatus);
        FraisBancaire updated = fraisBancaireRepository.save(fraisBancaire);

        log.info("Statut du frais bancaire ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return fraisBancaireMapper.toResponseDto(updated);
    }
}
