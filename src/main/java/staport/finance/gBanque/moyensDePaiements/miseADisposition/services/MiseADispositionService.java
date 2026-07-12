package staport.finance.gBanque.moyensDePaiements.miseADisposition.services;

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
import staport.finance.gBanque.moyensDePaiements.miseADisposition.dtos.*;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.exceptions.MiseADispositionBusinessException;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.mapper.MiseADispositionMapper;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.metrics.MiseADispositionMetrics;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Service métier du module Mise à Disposition.
 *
 * <p>Façade publique du domaine {@code miseADisposition} (architecture.md — Encapsulation des
 * domaines) : les autres modules doivent passer par ce service, jamais par
 * {@code MiseADispositionRepository} directement.
 */
@Service
public class MiseADispositionService {

    private static final Logger log = LoggerFactory.getLogger(MiseADispositionService.class);
    private final MiseADispositionRepository miseADispositionRepository;
    private final IBanqueService banqueService;
    private final MiseADispositionMapper miseADispositionMapper;
    private final MiseADispositionMetrics miseADispositionMetrics;

    public MiseADispositionService(MiseADispositionRepository miseADispositionRepository, IBanqueService banqueService,
                                    MiseADispositionMapper miseADispositionMapper, MiseADispositionMetrics miseADispositionMetrics) {
        this.miseADispositionRepository = miseADispositionRepository;
        this.banqueService = banqueService;
        this.miseADispositionMapper = miseADispositionMapper;
        this.miseADispositionMetrics = miseADispositionMetrics;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = MiseADisposition.class)
    public MiseADispositionResponseDto createEncaissementMiseAdisposition(MiseAdispositionRequestDto dto) {
        log.info("Création d'une nouvelle mise à disposition d'encaissement: {}", dto);

        Banque banque = banqueService.findOrThrow(dto.getBanqueId());
        MiseADisposition miseADisposition = miseADispositionMapper.toEntity(dto, banque);

        log.debug("Enregistrement de l'entité mise à disposition: {}", miseADisposition);
        miseADisposition = miseADispositionRepository.save(miseADisposition);
        miseADispositionMetrics.incrementerCreation();

        log.info("Mise à disposition créée avec succès avec l'ID: {}", miseADisposition.getId());
        return toDto(miseADisposition);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = MiseADisposition.class)
    public MiseADispositionResponseDto editEncaissementMiseAdisposition(Long id, MiseAdispositionRequestDto dto) {
        log.info("Modification de la mise à disposition d'encaissement ID={} avec les données={}", id, dto);

        MiseADisposition existingMiseAdisposition = trouverOuLeverException(id);

        if (existingMiseAdisposition.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'une mise à disposition qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new MiseADispositionBusinessException("Modification interdite: la MiseADisposition with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingMiseAdisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'une mise à disposition déjà réglée, ID={}", id);
            throw new MiseADispositionBusinessException("Modification interdite: la MiseADisposition with id " + id + " est déjà réglée.");
        }

        if (dto.getDateEmission() != null) {
            existingMiseAdisposition.setDateEmission(dto.getDateEmission());
        }
        if (dto.getBanqueId() != null) {
            existingMiseAdisposition.setBanque(banqueService.findOrThrow(dto.getBanqueId()));
        }
        if (dto.getMontant() != null) {
            existingMiseAdisposition.setMontant(dto.getMontant());
        }
        if (dto.getEmetteur() != null) {
            existingMiseAdisposition.setEmetteur(dto.getEmetteur());
        }

        existingMiseAdisposition = miseADispositionRepository.save(existingMiseAdisposition);
        log.info("Mise à disposition ID={} mise à jour avec succès", id);
        return toDto(existingMiseAdisposition);
    }

    @Transactional(readOnly = true)
    public MiseADispositionResponseDto getMiseAdispositionById(Long id) {
        log.debug("Récupération de la mise à disposition avec l'ID={}", id);
        return toDto(trouverOuLeverException(id));
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = MiseADisposition.class)
    public void deleteMiseAdispositionById(Long id) {
        log.info("Suppression de la mise à disposition avec l'ID={}", id);

        MiseADisposition miseADisposition = trouverOuLeverException(id);

        if (miseADisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'une mise à disposition déjà réglée, ID={}", id);
            throw new MiseADispositionBusinessException("Suppression interdite: la mise à dispo with id " + id + " est déjà réglée.");
        }

        miseADispositionRepository.deleteById(id);
        log.info("Mise à disposition ID={} supprimée avec succès", id);
    }

    @Transactional(readOnly = true)
    public Page<MiseADispositionResponseDto> getDecaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des mises à disposition de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} mise(s) à disposition de décaissement trouvée(s)", miseADispositionPage.getTotalElements());
        return miseADispositionPage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<MiseADispositionResponseDto> getEncaissementForMiseAdisposition(MiseADispositionSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des mises à disposition d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<MiseADisposition> spec = MiseADispositionSpec.bySearchDto(searchDto)
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT));
        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} mise(s) à disposition d'encaissement trouvée(s)", miseADispositionPage.getTotalElements());
        return miseADispositionPage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = MiseADisposition.class)
    public MiseADispositionResponseDto toggleMiseADispositionStatus(Long id) {
        log.info("Basculement du statut pour la mise à disposition ID={}", id);

        MiseADisposition miseADisposition = trouverOuLeverException(id);

        if (miseADisposition.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'une mise à disposition annulée, ID={}", id);
            throw new MiseADispositionBusinessException("Impossible de basculer le statut d'une mise à disposition annulée (ID=" + id + ").");
        }
        miseADisposition.setStatut(miseADisposition.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        miseADisposition = miseADispositionRepository.save(miseADisposition);
        if (miseADisposition.getStatut() == StatutMoyenPaiement.REGLE) {
            miseADispositionMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour la mise à disposition ID={}", id);
        return toDto(miseADisposition);
    }

    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursDecaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours de décaissement pour les mises à disposition de la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<MiseADisposition> spec = MiseADispositionSpec.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.DECAISSEMENT));

        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, stablePageable(pageable));
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursMiseAdisposition(totalDesEncours, miseADispositionPage.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public EncoursMiseAdisposition getEncoursEncaissementsForMiseADispositionByBank(EncoursMiseAdispositionSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours d'encaissement pour les mises à disposition de la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<MiseADisposition> spec = MiseADispositionSpec.encoursEncaissement(banqueId, finDuMoisCourant())
                .and(MiseADispositionSpec.hasNumero(searchDto.getNumero()))
                .and(MiseADispositionSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(MiseADispositionSpec.hasMontant(searchDto.getMontant()))
                .and(MiseADispositionSpec.hasType(Type.ENCAISSEMENT))
                .and(MiseADispositionSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(MiseADispositionSpec.hasDateEmission(searchDto.getDateEmission()));

        Page<MiseADisposition> miseADispositionPage = miseADispositionRepository.findAll(spec, stablePageable(pageable));
        BigDecimal totalDesEncours = miseADispositionRepository.findAll(spec).stream().map(MiseADisposition::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursMiseAdisposition(totalDesEncours, miseADispositionPage.map(this::toDto));
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private MiseADisposition trouverOuLeverException(Long id) {
        return miseADispositionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mise à disposition with id " + id + " not found."));
    }

    /**
     * Fin du mois courant — recalculée à chaque appel (et non mise en cache) pour que
     * les calculs d'encours restent corrects après le premier jour suivant le déploiement.
     */
    private LocalDate finDuMoisCourant() {
        return LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    }

    private Pageable stablePageable(Pageable pageable) {
        return pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }

    /**
     * Mappe l'entité vers le DTO puis applique les champs d'affichage calculés (règle métier
     * propre au service — jamais dans le Mapper, cf. architecture.md) : observation (" MAD " +
     * numéro) et libellé (bénéficiaire) — comportement identique à l'ancien mapper statique.
     */
    private MiseADispositionResponseDto toDto(MiseADisposition miseADisposition) {
        MiseADispositionResponseDto dto = miseADispositionMapper.toDto(miseADisposition);
        dto.setObservation(" MAD " + miseADisposition.getNumero());
        dto.setLibelle(miseADisposition.getBeneficiaire());
        return dto;
    }
}
