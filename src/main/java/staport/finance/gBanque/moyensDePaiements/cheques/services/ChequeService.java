package staport.finance.gBanque.moyensDePaiements.cheques.services;

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
import staport.finance.gBanque.moyensDePaiements.cheques.dtos.*;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.exceptions.ChequeBusinessException;
import staport.finance.gBanque.moyensDePaiements.cheques.mappers.ChequeMapper;
import staport.finance.gBanque.moyensDePaiements.cheques.metrics.ChequeMetrics;
import staport.finance.gBanque.moyensDePaiements.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.moyensDePaiements.cheques.specs.ChequeSpec;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import staport.finance.gBanque.moyensDePaiements.synchronisation.enums.StatutMoyenPaiement;

/**
 * Service métier du module Chèque.
 *
 * <p>Façade publique du domaine {@code cheques} (architecture.md — Encapsulation des domaines) :
 * les autres modules doivent passer par ce service, jamais par {@code ChequeRepository} directement.
 */
@Service
public class ChequeService {

    private static final Logger log = LoggerFactory.getLogger(ChequeService.class);
    private final ChequeRepository chequeRepository;
    private final IBanqueService banqueService;
    private final ChequeMapper chequeMapper;
    private final ChequeMetrics chequeMetrics;

    public ChequeService(ChequeRepository chequeRepository, IBanqueService banqueService, ChequeMapper chequeMapper, ChequeMetrics chequeMetrics) {
        this.chequeRepository = chequeRepository;
        this.banqueService = banqueService;
        this.chequeMapper = chequeMapper;
        this.chequeMetrics = chequeMetrics;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = Cheque.class)
    public ChequeResponseDto createEncaissementCheque(ChequeRequestDto dto) {
        log.info("Création d'un chèque d'encaissement — numero: {}", dto.getNumero());

        Banque banque = banqueService.findOrThrow(dto.getBanqueId());
        Cheque cheque = chequeMapper.toEntity(dto, banque);
        cheque = chequeRepository.save(cheque);
        chequeMetrics.incrementerCreation(cheque.getType());

        log.info("Chèque créé — id: {}", cheque.getId());
        return toDto(cheque);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Cheque.class)
    public ChequeResponseDto editEncaissementCheque(Long id, ChequeRequestDto dto) {
        log.info("Modification chèque encaissement — id: {}", id);

        Cheque existingCheque = trouverOuLeverException(id);

        if (existingCheque.getType() != Type.ENCAISSEMENT) {
            log.warn("Tentative de modification d'un chèque non ENCAISSEMENT — id: {}", id);
            throw new ChequeBusinessException("Modification interdite : le chèque id " + id + " n'est pas de type ENCAISSEMENT.");
        }

        if (existingCheque.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de modification d'un chèque déjà réglé — id: {}", id);
            throw new ChequeBusinessException("Modification interdite : le chèque id " + id + " est déjà réglé.");
        }

        if (dto.getNumero() != null) {
            existingCheque.setNumero(dto.getNumero());
        }
        if (dto.getDateEmission() != null) {
            existingCheque.setDateEmission(dto.getDateEmission());
        }
        if (dto.getDateEcheance() != null) {
            existingCheque.setDateEcheance(dto.getDateEcheance());
        }
        if (dto.getBanqueId() != null) {
            existingCheque.setBanque(banqueService.findOrThrow(dto.getBanqueId()));
        }
        if (dto.getMontant() != null) {
            existingCheque.setMontant(dto.getMontant());
        }
        if (dto.getEmetteur() != null) {
            existingCheque.setEmetteur(dto.getEmetteur());
        }

        existingCheque = chequeRepository.save(existingCheque);
        log.info("Chèque modifié — id: {}", id);
        return toDto(existingCheque);
    }

    @Transactional(readOnly = true)
    public ChequeResponseDto getChequeById(Long id) {
        log.debug("Récupération chèque — id: {}", id);
        return toDto(trouverOuLeverException(id));
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = Cheque.class)
    public void deleteCheque(Long id) {
        log.info("Suppression chèque — id: {}", id);

        Cheque cheque = trouverOuLeverException(id);

        if (cheque.getStatut() == StatutMoyenPaiement.REGLE) {
            log.warn("Tentative de suppression d'un chèque déjà réglé — id: {}", id);
            throw new ChequeBusinessException("Suppression interdite : le chèque id " + id + " est déjà réglé.");
        }

        chequeRepository.deleteById(id);
        log.info("Chèque supprimé — id: {}", id);
    }

    @Transactional(readOnly = true)
    public Page<ChequeResponseDto> getAllDecaissement(ChequeSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche chèques décaissement — critères: {}", searchDto);

        Specification<Cheque> spec = Specification.where(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.getStatut()))
                .and(ChequeSpec.hasType(Type.DECAISSEMENT));

        Page<Cheque> chequePage = chequeRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} chèque(s) de décaissement trouvé(s)", chequePage.getTotalElements());

        return chequePage.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ChequeResponseDto> getAllEncaissement(ChequeSearchDto searchDto, Pageable pageable) {
        log.debug("Recherche chèques encaissement — critères: {}", searchDto);

        Specification<Cheque> spec = Specification.where(ChequeSpec.hasType(Type.ENCAISSEMENT))
                .and(ChequeSpec.hasNomBanque(searchDto.getNomBanque()))
                .and(ChequeSpec.hasStatut(searchDto.getStatut()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()));

        Page<Cheque> chequePage = chequeRepository.findAll(spec, stablePageable(pageable));
        log.debug("{} chèque(s) d'encaissement trouvé(s)", chequePage.getTotalElements());

        return chequePage.map(this::toDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = Cheque.class)
    public ChequeResponseDto toggleChequeStatus(Long id) {
        log.info("Basculement statut chèque — id: {}", id);

        Cheque cheque = trouverOuLeverException(id);

        if (cheque.getStatut() == StatutMoyenPaiement.ANNULE) {
            log.warn("Tentative de basculement du statut d'un chèque annulé — id: {}", id);
            throw new ChequeBusinessException("Impossible de basculer le statut d'un chèque annulé (id: " + id + ").");
        }

        cheque.setStatut(cheque.getStatut() == StatutMoyenPaiement.ENCOURS ? StatutMoyenPaiement.REGLE : StatutMoyenPaiement.ENCOURS);
        cheque = chequeRepository.save(cheque);
        if (cheque.getStatut() == StatutMoyenPaiement.REGLE) {
            chequeMetrics.incrementerPassageRegle();
        }

        log.info("Statut basculé — id: {}, nouveau statut: {}", id, cheque.getStatut());
        return toDto(cheque);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursDecaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Encours décaissement chèques — banqueId: {}, critères: {}", banqueId, searchDto);

        Specification<Cheque> spec = ChequeSpec.encoursDecaissements(banqueId, finDuMoisCourant())
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()));

        Page<Cheque> page = chequeRepository.findAll(spec, stablePageable(pageable));
        Page<ChequeResponseDto> chequePage = page.map(this::toDto);
        BigDecimal total = chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total encours décaissement — banqueId: {}, total: {}", banqueId, total);
        return new EncoursChequeResponseDto(total, chequePage);
    }

    @Transactional(readOnly = true)
    public EncoursChequeResponseDto getEncoursEncaissementForCheques(EncoursChequeSearchDto searchDto, Long banqueId, Pageable pageable) {
        log.debug("Encours encaissement chèques — banqueId: {}, critères: {}", banqueId, searchDto);

        Specification<Cheque> spec = ChequeSpec.encoursEncaissements(banqueId, finDuMoisCourant())
                .and(ChequeSpec.hasBeneficiaire(searchDto.getBeneficiaire()))
                .and(ChequeSpec.hasEmetteur(searchDto.getEmetteur()))
                .and(ChequeSpec.hasMontant(searchDto.getMontant()))
                .and(ChequeSpec.hasNumero(searchDto.getNumero()))
                .and(ChequeSpec.hasDateEmission(searchDto.getDateEmission()));

        Page<Cheque> page = chequeRepository.findAll(spec, stablePageable(pageable));
        Page<ChequeResponseDto> chequePage = page.map(this::toDto);
        BigDecimal total = chequeRepository.findAll(spec).stream().map(Cheque::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);

        log.debug("Total encours encaissement — banqueId: {}, total: {}", banqueId, total);
        return new EncoursChequeResponseDto(total, chequePage);
    }

    // ── Helpers internes ──────────────────────────────────────────────────────

    private Cheque trouverOuLeverException(Long id) {
        return chequeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cheque with id " + id + " not found."));
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
     * Mappe l'entité vers le DTO puis applique les libellés d'affichage calculés
     * (règle métier propre au service — jamais dans le Mapper, cf. architecture.md).
     */
    private ChequeResponseDto toDto(Cheque cheque) {
        ChequeResponseDto dto = chequeMapper.toDto(cheque);
        dto.setObservation("CHQ N°" + cheque.getNumero());
        dto.setLibelle(cheque.getType() == Type.DECAISSEMENT ? cheque.getBeneficiaire() : cheque.getEmetteur());
        return dto;
    }
}
