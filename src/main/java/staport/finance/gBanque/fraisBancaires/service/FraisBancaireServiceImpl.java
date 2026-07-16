package staport.finance.gBanque.fraisBancaires.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.audit.AuditOperation;
import staport.finance.gBanque.audit.AuditableAction;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireDto;
import staport.finance.gBanque.fraisBancaires.dtos.EncoursFraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireRequestDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireResponseDto;
import staport.finance.gBanque.fraisBancaires.dtos.FraisBancaireSearchDto;
import staport.finance.gBanque.fraisBancaires.entities.FraisBancaire;
import staport.finance.gBanque.fraisBancaires.mapper.FraisBancaireMapper;
import staport.finance.gBanque.fraisBancaires.repositories.FraisBancaireRepository;
import staport.finance.gBanque.fraisBancaires.spec.FraisBancaireSpecification;
import staport.finance.gBanque.prefinancement.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Implémentation du service Frais bancaires.
 *
 * <p>Encapsulation des domaines (architecture.md) : la résolution de la {@link Banque} passe
 * exclusivement par {@link IBanqueService} — jamais par injection directe de
 * {@code BanqueRepository}.
 *
 * <p>Renommé depuis {@code FraisBancaireService} (sweep de conformité) pour respecter la
 * convention {@code IXxxService}/{@code XxxServiceImpl} appliquée à tous les modules déjà
 * sweepés — le contrôleur injectait auparavant la classe concrète directement.
 */
@Service
public class FraisBancaireServiceImpl implements IFraisBancaireService {

    private static final Logger log = LoggerFactory.getLogger(FraisBancaireServiceImpl.class);
    private final FraisBancaireRepository fraisBancaireRepository;
    private final IBanqueService banqueService;
    private final FraisBancaireMapper fraisBancaireMapper;

    public FraisBancaireServiceImpl(FraisBancaireRepository fraisBancaireRepository,
                                     IBanqueService banqueService,
                                     FraisBancaireMapper fraisBancaireMapper) {
        this.fraisBancaireRepository = fraisBancaireRepository;
        this.banqueService = banqueService;
        this.fraisBancaireMapper = fraisBancaireMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsChargeFixeGeneree(Long banqueId, String libelle, LocalDate debut, LocalDate fin) {
        return fraisBancaireRepository.existsByBanqueIdAndLibelleAndDateEmissionBetween(banqueId, libelle, debut, fin);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = FraisBancaire.class)
    public FraisBancaireResponseDto createFraisBancaire(FraisBancaireRequestDto requestDto) {
        log.info("Création d'un frais bancaire - Banque ID: {}, Libellé: '{}'", requestDto.banqueId(), requestDto.libelle());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());
        FraisBancaire fraisBancaire = fraisBancaireMapper.toEntity(requestDto, banque);
        FraisBancaire saved = fraisBancaireRepository.save(fraisBancaire);

        log.info("Frais bancaire créé avec succès - ID: {}", saved.getId());
        return fraisBancaireMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public FraisBancaireResponseDto getById(Long id) {
        log.info("Récupération du frais bancaire avec l'ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        return fraisBancaireMapper.toResponseDto(fraisBancaire);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FraisBancaireResponseDto> searchFraisBancaire(FraisBancaireSearchDto searchDto, Pageable pageable) {
        log.info("Recherche de frais bancaires - Critères: {}", searchDto);

        Page<FraisBancaireResponseDto> result = fraisBancaireRepository.findAll(
                        FraisBancaireSpecification.searchByCriteria(searchDto), pageable)
                .map(fraisBancaireMapper::toResponseDto);

        log.info("Recherche terminée - {} frais bancaire(s) trouvé(s) sur {} au total",
                result.getNumberOfElements(), result.getTotalElements());
        return result;
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = FraisBancaire.class)
    public FraisBancaireResponseDto updateFraisBancaire(Long id, FraisBancaireRequestDto requestDto) {
        log.info("Mise à jour du frais bancaire ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        fraisBancaire.setLibelle(requestDto.libelle());
        fraisBancaire.setObservation(requestDto.observation());
        fraisBancaire.setMontant(requestDto.montant());
        fraisBancaire.setBanque(banque);
        fraisBancaire.setDateEmission(requestDto.dateEmission());

        FraisBancaire updated = fraisBancaireRepository.save(fraisBancaire);
        log.info("Frais bancaire ID: {} mis à jour avec succès", id);
        return fraisBancaireMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = FraisBancaire.class)
    public void deleteFraisBancaire(Long id) {
        log.info("Suppression du frais bancaire avec l'ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        if (fraisBancaire.getStatus() != Status.PREVU) {
            log.warn("Tentative de suppression d'un frais bancaire déjà réglé — id: {}", id);
            throw new ValidationException("Le frais bancaire id: " + id + " est déjà réglé et ne peut plus être supprimé.");
        }

        fraisBancaireRepository.deleteById(id);
        log.info("Frais bancaire ID: {} supprimé avec succès", id);
    }

    @Override
    @Transactional(readOnly = true)
    public EncoursFraisBancaireDto getOutstandingFraisBancaire(EncoursFraisBancaireSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.info("Récupération des frais bancaires en cours - banqueId={}, Critères: {}", banqueId, searchDto);

        LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<FraisBancaire> spec = Specification.where(FraisBancaireSpecification.outstandingUntil(endOfMonth))
                .and(FraisBancaireSpecification.hasBanque(banqueId))
                .and(FraisBancaireSpecification.hasLibelle(searchDto.libelle()))
                .and(FraisBancaireSpecification.hasMontant(searchDto.montant()));

        Page<FraisBancaire> page = fraisBancaireRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        BigDecimal total = fraisBancaireRepository.findAll(spec).stream()
                .map(FraisBancaire::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("Encours calculé: {}, Nombre de frais: {}", total, page.getTotalElements());

        Page<FraisBancaireResponseDto> pageDto = page.map(fraisBancaireMapper::toResponseDto);
        return new EncoursFraisBancaireDto(total, pageDto);
    }

    @Override
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = FraisBancaire.class)
    public FraisBancaireResponseDto toggleStatus(Long id) {
        log.info("Basculement du statut du frais bancaire ID: {}", id);

        FraisBancaire fraisBancaire = fraisBancaireRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Frais bancaire non trouvé avec l'ID: " + id));

        Status oldStatus = fraisBancaire.getStatus();
        Status newStatus = oldStatus == Status.PAYE ? Status.PREVU : Status.PAYE;
        fraisBancaire.setStatus(newStatus);
        FraisBancaire updated = fraisBancaireRepository.save(fraisBancaire);

        log.info("Statut du frais bancaire ID: {} basculé de {} vers {}", id, oldStatus, newStatus);
        return fraisBancaireMapper.toResponseDto(updated);
    }
}
