package staport.finance.gBanque.moyensDePaiements.virements.service;

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
import staport.finance.gBanque.moyensDePaiements.virements.dtos.*;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.exceptions.VirementBusinessException;
import staport.finance.gBanque.moyensDePaiements.virements.mapper.VirementMapper;
import staport.finance.gBanque.moyensDePaiements.virements.metrics.VirementMetrics;
import staport.finance.gBanque.moyensDePaiements.virements.repositories.VirementRepository;
import staport.finance.gBanque.moyensDePaiements.virements.specs.VirementSpec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Service métier du module Virement.
 *
 * <p>Façade publique du domaine {@code virements} (architecture.md — Encapsulation des domaines) :
 * les autres modules doivent passer par ce service, jamais par {@code VirementRepository} directement.
 */
@Service
public class VirementService {

    private static final Logger log = LoggerFactory.getLogger(VirementService.class);
    private final VirementRepository virementRepository;
    private final IBanqueService banqueService;
    private final VirementMapper virementMapper;
    private final VirementMetrics virementMetrics;

    public VirementService(VirementRepository virementRepository, IBanqueService banqueService,
                            VirementMapper virementMapper, VirementMetrics virementMetrics) {
        this.virementRepository = virementRepository;
        this.banqueService = banqueService;
        this.virementMapper = virementMapper;
        this.virementMetrics = virementMetrics;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Virement.class)
    public VirementResponseDto createEncaissementVirement(VirementRequestDto dto) {
        log.info("Création d'un nouveau virement d'encaissement: {}", dto);

        Banque banque = banqueService.findOrThrow(dto.getBanqueBeneficiaireId());
        Virement virement = virementMapper.toEntity(dto, banque);
        virement = virementRepository.save(virement);
        virementMetrics.incrementerCreation(virement.getType());

        log.info("Virement créé avec succès avec l'ID: {}", virement.getId());
        return toDto(virement);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Virement.class)
    public VirementResponseDto editEncaissementVirement(Long id, VirementRequestDto dto) {
        log.info("Modification du virement d'encaissement ID={} avec les données={}", id, dto);

        Virement existingVirement = trouverOuLeverException(id);

        if (existingVirement.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un virement qui n'est pas de type ENCAISSEMENT, ID={}", id);
            throw new VirementBusinessException("Modification interdite: le virement with id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingVirement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un virement déjà réglé, ID={}", id);
            throw new VirementBusinessException("Modification interdite: le virement with id " + id + " est déjà réglé.");
        }

        if (dto.getMontant() != null) {
            existingVirement.setMontant(dto.getMontant());
        }
        if (dto.getDateReception() != null) {
            existingVirement.setDateEmission(dto.getDateReception());
        }
        if (dto.getEmetteur() != null) {
            existingVirement.setEmetteur(dto.getEmetteur());
        }
        if (dto.getBanqueBeneficiaireId() != null) {
            existingVirement.setBanqueBeneficiare(banqueService.findOrThrow(dto.getBanqueBeneficiaireId()));
        }

        existingVirement = virementRepository.save(existingVirement);
        log.info("Virement ID={} mis à jour avec succès", id);
        return toDto(existingVirement);
    }

    @Transactional(readOnly = true)
    public VirementResponseDto getVirementById(Long id) {
        log.debug("Récupération du virement avec l'ID={}", id);
        return toDto(trouverOuLeverException(id));
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getEncaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements d'encaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.ENCAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} virement(s) d'encaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<VirementResponseDto> getDecaissementVirements(VirementSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche des virements de décaissement avec les filtres={} et pagination={}", searchDto, pageable);

        Specification<Virement> spec = VirementSpec.bySearchDto(searchDto)
                .and(VirementSpec.hasType(Type.DECAISSEMENT));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} virement(s) de décaissement trouvé(s)", virementPage.getTotalElements());
        return virementPage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Virement.class)
    public VirementResponseDto toggleVirementStatus(Long id) {
        log.info("Basculement du statut pour le virement ID={}", id);

        Virement virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un virement annulé, ID={}", id);
            throw new VirementBusinessException("Impossible de basculer le statut d'un virement annulé (ID=" + id + ").");
        }
        virement.setStatut(virement.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        virement = virementRepository.save(virement);
        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            virementMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé avec succès pour le virement ID={} — nouveau statut={}", id, virement.getStatut());
        return toDto(virement);
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursDecaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours de décaissement pour la banque ID={} avec les filtres={}", banqueId, searchDto);

        Specification<Virement> spec = VirementSpec.encoursDecaissement(banqueId, finDuMoisCourant())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours de décaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this::toDto));
    }

    @Transactional(readOnly = true)
    public EncoursVirementResponseDto getEncoursEncaissementForVirements(EncoursVirementsSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Récupération des encours d'encaissement pour la banque ID={} avec les filtres={}", banqueId, searchDto);

        Banque banqueBeneficiaire = banqueService.findOrThrow(banqueId);

        Specification<Virement> spec = VirementSpec.encoursEncaissement(banqueBeneficiaire.getNom(), finDuMoisCourant())
                .and(VirementSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(VirementSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(VirementSpec.hasMontant(searchDto.getMontant()))
                .and(VirementSpec.hasEmetteur(searchDto.getEmetteur()));

        Page<Virement> virementPage = virementRepository.findAll(spec, stablePageable(pageable));

        BigDecimal totalDesEncours = virementRepository.findAll(spec).stream()
                .map(Virement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total des encours d'encaissement pour la banque ID={}: {}", banqueId, totalDesEncours);
        return new EncoursVirementResponseDto(totalDesEncours, virementPage.map(this::toDto));
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Virement.class)
    public void deleteVirement(Long id) {
        log.info("Suppression du virement ID={}", id);

        Virement virement = trouverOuLeverException(id);

        if (virement.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un virement déjà réglé, ID={}", id);
            throw new VirementBusinessException("Suppression interdite: le virement with id " + id + " est déjà réglé.");
        }

        virementRepository.delete(virement);
        log.info("Virement ID={} supprimé avec succès", id);
    }

    /**
     * Nombre de virements échus à la date donnée, groupés par banque — façade publique sur
     * {@link VirementRepository#countEchusGroupedByBanque}. Permet à d'autres domaines (ex.
     * l'agrégateur accueil de Bank) de lire cette information sans jamais injecter
     * {@code VirementRepository} directement (Backend/CLAUDE.md, "Encapsulation des domaines").
     */
    @Transactional(readOnly = true)
    public List<Object[]> countEchusGroupedByBanque(LocalDate today) {
        return virementRepository.countEchusGroupedByBanque(today);
    }

    /**
     * Nombre de virements échus pour une banque donnée — façade publique sur
     * {@link VirementRepository#countEchusByBanqueId}. Utilisé par l'agrégateur accueil (requête
     * ciblée sur une seule banque, plus efficace que {@link #countEchusGroupedByBanque}).
     */
    @Transactional(readOnly = true)
    public long countEchusByBanqueId(Long banqueId, LocalDate today) {
        return virementRepository.countEchusByBanqueId(banqueId, today);
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private Virement trouverOuLeverException(Long id) {
        return virementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Virement with id " + id + " not found."));
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
     * Mappe l'entité vers le DTO puis applique les champs d'affichage calculés
     * (règle métier propre au service — jamais dans le Mapper, cf. architecture.md) :
     * libellé, observation, et le nom de la banque bénéficiaire (référencée ou libre).
     */
    private VirementResponseDto toDto(Virement virement) {
        VirementResponseDto dto = virementMapper.toDto(virement);
        dto.setObservation(" VIREMENT N° " + virement.getNumero());
        dto.setLibelle(virement.getType() == Type.DECAISSEMENT ? virement.getBeneficiaire() : virement.getEmetteur());
        dto.setBanqueBeneficiaire(virement.getBanqueBeneficiare() != null
                ? virement.getBanqueBeneficiare().getNom()
                : virement.getNomBanqueBeneficiaireLibre());
        return dto;
    }
}
