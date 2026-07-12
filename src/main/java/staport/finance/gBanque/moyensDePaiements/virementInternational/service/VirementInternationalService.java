package staport.finance.gBanque.moyensDePaiements.virementInternational.service;

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
import staport.finance.gBanque.moyensDePaiements.virementInternational.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.exceptions.VirementInternationalBusinessException;
import staport.finance.gBanque.moyensDePaiements.virementInternational.mapper.VirementInternationalMapper;
import staport.finance.gBanque.moyensDePaiements.virementInternational.metrics.VirementInternationalMetrics;
import staport.finance.gBanque.moyensDePaiements.virementInternational.repositories.VirementInternationalRepository;
import staport.finance.gBanque.moyensDePaiements.virementInternational.specs.VirementInternationalSpec;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Service métier du module Virement International.
 *
 * <p>Façade publique du domaine {@code virementInternational} (architecture.md — Encapsulation
 * des domaines) : les autres modules doivent passer par ce service, jamais par
 * {@code VirementInternationalRepository} directement.
 *
 * <p>Particularité métier : contrairement à Chèque/LCN/Virement, ce module n'a aucune
 * synchronisation FunPaiement — encaissements et décaissements sont tous deux saisis
 * manuellement (aucun {@code VirementInternationalPaymentProcessor} n'existe dans le module
 * {@code synchronisation}).
 */
@Service
public class VirementInternationalService {

    private static final Logger log = LoggerFactory.getLogger(VirementInternationalService.class);
    private final VirementInternationalRepository virementInternationalRepository;
    private final IBanqueService banqueService;
    private final VirementInternationalMapper virementInternationalMapper;
    private final VirementInternationalMetrics virementInternationalMetrics;

    public VirementInternationalService(
            VirementInternationalRepository virementInternationalRepository,
            IBanqueService banqueService,
            VirementInternationalMapper virementInternationalMapper,
            VirementInternationalMetrics virementInternationalMetrics) {
        this.virementInternationalRepository = virementInternationalRepository;
        this.banqueService = banqueService;
        this.virementInternationalMapper = virementInternationalMapper;
        this.virementInternationalMetrics = virementInternationalMetrics;
    }

    /**
     * Crée un nouveau virement international (ENCAISSEMENT ou DECAISSEMENT).
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = VirementInternational.class)
    public VirementInternationalResponseDto createVirementInternational(VirementInternationalRequestDto dto) {
        log.info("Création d'un nouveau virement international de type {}: {}", dto.getType(), dto);

        Banque banque = banqueService.findOrThrow(dto.getBanqueId());
        VirementInternational virement = virementInternationalMapper.toEntity(dto, banque);
        virement = virementInternationalRepository.save(virement);
        virementInternationalMetrics.incrementerCreation(virement.getType());

        log.info("Virement international créé avec succès avec l'ID: {} (Type: {})", virement.getId(), virement.getType());
        return virementInternationalMapper.toDto(virement);
    }

    /**
     * Modifie un virement international existant. La modification n'est autorisée que si le
     * virement n'est pas déjà réglé.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = VirementInternational.class)
    public VirementInternationalResponseDto editVirementInternational(Long id, VirementInternationalRequestDto dto) {
        log.info("Modification du virement international ID={} avec les données={}", id, dto);

        VirementInternational existingVirement = trouverOuLeverException(id);

        if (existingVirement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un virement international déjà réglé, ID={}", id);
            throw new VirementInternationalBusinessException("Modification interdite: le virement international with id " + id + " est déjà réglé.");
        }

        Banque banque = dto.getBanqueId() != null && !dto.getBanqueId().equals(existingVirement.getBanque().getId())
                ? banqueService.findOrThrow(dto.getBanqueId())
                : existingVirement.getBanque();

        VirementInternational updatedVirement = virementInternationalMapper.updateEntity(existingVirement, dto, banque);

        updatedVirement = virementInternationalRepository.save(updatedVirement);
        log.info("Virement international ID={} mis à jour avec succès", id);
        return virementInternationalMapper.toDto(updatedVirement);
    }

    @Transactional(readOnly = true)
    public VirementInternationalResponseDto getVirementInternationalById(Long id) {
        log.debug("Récupération du virement international avec l'ID={}", id);
        return virementInternationalMapper.toDto(trouverOuLeverException(id));
    }

    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements internationaux avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto);
        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} virement(s) international(aux) trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(virementInternationalMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getEncaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements internationaux d'encaissement avec les filtres={} et pagination={}",
                searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto)
                .and(VirementInternationalSpec.hasType(Type.ENCAISSEMENT));
        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} virement(s) international(aux) d'encaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(virementInternationalMapper::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementInternationalResponseDto> getDecaissementVirementsInternationaux(
            VirementInternationalSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements internationaux de décaissement avec les filtres={} et pagination={}",
                searchDto, pageable);

        Specification<VirementInternational> spec = VirementInternationalSpec.bySearchDto(searchDto)
                .and(VirementInternationalSpec.hasType(Type.DECAISSEMENT));
        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        log.debug("{} virement(s) international(aux) de décaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(virementInternationalMapper::toDto);
    }

    /**
     * Bascule le statut ENCOURS/REGLE d'un virement international.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = VirementInternational.class)
    public VirementInternationalResponseDto toggleVirementStatus(Long id) {
        log.info("Basculement du statut pour le virement international ID={}", id);

        VirementInternational virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un virement international annulé, ID={}", id);
            throw new VirementInternationalBusinessException("Impossible de basculer le statut d'un virement international annulé (ID=" + id + ").");
        }
        virement.setStatut(virement.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        virement = virementInternationalRepository.save(virement);
        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            virementInternationalMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour le virement international ID={} — nouveau statut={}", id, virement.getStatut());
        return virementInternationalMapper.toDto(virement);
    }

    /**
     * Encours de décaissement pour une banque donnée : virements ENCOURS de type DECAISSEMENT
     * dont la date d'émission ne dépasse pas la fin du mois en cours.
     */
    @Transactional(readOnly = true)
    public EncoursVirementInternationalResponseDto getEncoursDecaissement(
            EncoursVirementsInternationalSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours de décaissement pour la banque ID={} avec les filtres={}",
                banqueId, searchDto);

        banqueService.findOrThrow(banqueId);

        Specification<VirementInternational> spec = VirementInternationalSpec.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(VirementInternationalSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementInternationalSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementInternationalSpec.hasMontant(searchDto.getMontant()))
                .and(VirementInternationalSpec.hasEmetteur(searchDto.getEmetteur()));

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        BigDecimal totalDesEncours = virementInternationalRepository.findAll(spec).stream()
                .map(VirementInternational::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursVirementInternationalResponseDto(totalDesEncours, virementPage.map(virementInternationalMapper::toDto));
    }

    /**
     * Encours d'encaissement pour une banque donnée : virements ENCOURS de type ENCAISSEMENT
     * dont la date d'émission ne dépasse pas la fin du mois en cours.
     */
    @Transactional(readOnly = true)
    public EncoursVirementInternationalResponseDto getEncoursEncaissement(
            EncoursVirementsInternationalSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours d'encaissement pour la banque ID={} avec les filtres={}",
                banqueId, searchDto);

        banqueService.findOrThrow(banqueId);

        Specification<VirementInternational> spec = VirementInternationalSpec.encoursEncaissement(banqueId, finDuMoisCourant())
                .and(VirementInternationalSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementInternationalSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementInternationalSpec.hasMontant(searchDto.getMontant()))
                .and(VirementInternationalSpec.hasEmetteur(searchDto.getEmetteur()));

        Page<VirementInternational> virementPage = virementInternationalRepository.findAll(spec, stablePageable(pageable));

        BigDecimal totalDesEncours = virementInternationalRepository.findAll(spec).stream()
                .map(VirementInternational::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursVirementInternationalResponseDto(totalDesEncours, virementPage.map(virementInternationalMapper::toDto));
    }

    /**
     * Supprime un virement international. La suppression n'est autorisée que si le virement n'est
     * pas déjà réglé.
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = VirementInternational.class)
    public void deleteVirementInternational(Long id) {
        log.info("Suppression du virement international ID={}", id);

        VirementInternational virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un virement international déjà réglé, ID={}", id);
            throw new VirementInternationalBusinessException("Suppression interdite: le virement international with id " + id + " est déjà réglé.");
        }

        virementInternationalRepository.delete(virement);
        log.info("Virement international ID={} supprimé avec succès", id);
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private VirementInternational trouverOuLeverException(Long id) {
        return virementInternationalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VirementInternational with id " + id + " not found."));
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
}
