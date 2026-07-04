package staport.finance.gBanque.cmt.services;

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
import staport.finance.gBanque.cmt.dtos.*;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.services.EcheanceCMTService;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.mappers.CreditMoyenTermeMapper;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.cmt.specifications.CreditMoyenTermeSpecification;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;

/**
 * Service pour la gestion des crédits à moyen terme (CMT).
 */
@Service
@Transactional(readOnly = true)
public class CreditMoyenTermeService {

    private static final Logger log = LoggerFactory.getLogger(CreditMoyenTermeService.class);

    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final IBanqueService banqueService;
    private final CreditMoyenTermeMapper creditMoyenTermeMapper;
    private final EcheanceCMTService echeanceCmtService;

    public CreditMoyenTermeService(CreditMoyenTermeRepository creditMoyenTermeRepository,
                                   IBanqueService banqueService,
                                   CreditMoyenTermeMapper creditMoyenTermeMapper,
                                   EcheanceCMTService echeanceCmtService) {
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.banqueService = banqueService;
        this.creditMoyenTermeMapper = creditMoyenTermeMapper;
        this.echeanceCmtService = echeanceCmtService;
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.CREATE, entity = CreditMoyenTerme.class)
    public CreditMoyenTermeResponseDto create(CreditMoyenTermeRequestDto requestDto) {
        log.info("Création d'un crédit CMT — banqueId: {}", requestDto.banqueId());

        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        CreditMoyenTerme credit = creditMoyenTermeMapper.toEntity(requestDto, banque);
        CreditMoyenTerme savedCredit = creditMoyenTermeRepository.save(credit);

        echeanceCmtService.generateEcheances(savedCredit);
        log.info("Crédit CMT créé — ID: {}", savedCredit.getId());

        return creditMoyenTermeMapper.toResponseDto(savedCredit);
    }

    @Transactional(readOnly = true)
    public CreditMoyenTermeResponseDto findById(Long id) {
        return creditMoyenTermeMapper.toResponseDto(findOrThrow(id));
    }

    /**
     * Résout l'entité par son ID ou lève une exception.
     * Point d'entrée public du domaine {@code cmt} pour les autres modules
     * (ex. {@code ValidationController}, cf. Bounded Context — architecture.md).
     */
    @Transactional(readOnly = true)
    public CreditMoyenTerme findOrThrow(Long id) {
        return creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchActiveCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getActiveCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);

        return creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), defaultSort(pageable))
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchPendingClosureCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getPendingClosureCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);

        return creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), defaultSort(pageable))
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchArchivedCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getArchivedCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.searchByCriteria(searchDto);

        return creditMoyenTermeRepository
                .findAll(Specification.where(statusSpec).and(searchSpec), defaultSort(pageable))
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = CreditMoyenTerme.class)
    public CreditMoyenTermeResponseDto update(Long id, CreditMoyenTermeRequestDto requestDto) {
        log.info("Mise à jour du crédit CMT — ID: {}", id);

        CreditMoyenTerme existingCredit = findOrThrow(id);
        Banque banque = banqueService.findOrThrow(requestDto.banqueId());

        creditMoyenTermeMapper.updateEntity(requestDto, existingCredit, banque);
        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(existingCredit);

        echeanceCmtService.regenerateEcheances(updatedCredit);
        log.info("Crédit CMT mis à jour — ID: {}", updatedCredit.getId());

        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }

    /**
     * Supprime un crédit à moyen terme ainsi que toutes ses échéances associées.
     * La suppression est bloquée si le crédit contient des échéances déjà payées.
     *
     * @param id L'ID du crédit à supprimer
     * @return Un DTO de confirmation de suppression
     * @throws ResourceNotFoundException Si le crédit n'existe pas
     * @throws ValidationException Si le crédit contient des échéances payées
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.DELETE, entity = CreditMoyenTerme.class)
    public DeleteResponseDto delete(Long id) {
        log.warn("Suppression du crédit CMT — ID: {}", id);

        CreditMoyenTerme credit = findOrThrow(id);

        if (echeanceCmtService.hasPaidEcheances(id)) {
            throw new ValidationException(
                    "Impossible de supprimer le crédit avec l'ID " + id +
                            " car certaines de ses échéances ont déjà été payées. " +
                            "Veuillez l'archiver au lieu de le supprimer.");
        }

        int echeancesDeleted = echeanceCmtService.deleteEcheancesByCreditMoyenTerme(credit);
        creditMoyenTermeRepository.delete(credit);

        log.info("Crédit CMT supprimé — ID: {}, {} échéance(s) supprimée(s)", id, echeancesDeleted);

        return new DeleteResponseDto("Credit moyen terme deleted successfully");
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = CreditMoyenTerme.class)
    public CreditMoyenTermeResponseDto markAsArchived(Long id) {
        log.info("Archivage du crédit CMT — ID: {}", id);

        CreditMoyenTerme credit = findOrThrow(id);

        if (credit.isCompleted()) {
            throw new ValidationException("Le crédit avec l'ID " + id + " est déjà archivé.");
        }

        boolean hasUnpaidInstallments = credit.getEcheanceCMTS().stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() != StatutEcheanceCMT.PAYE);

        if (hasUnpaidInstallments) {
            throw new ValidationException("Le crédit ne peut être archivé que si toutes les échéances ont été payées.");
        }

        credit.setCompleted(true);
        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(credit);

        log.info("Crédit CMT archivé — ID: {}", id);
        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }

    private Pageable defaultSort(Pageable pageable) {
        return pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }
}
