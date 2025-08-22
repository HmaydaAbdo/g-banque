package staport.finance.gBanque.cmt.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
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

    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final BanqueRepository banqueRepository;
    private final CreditMoyenTermeMapper creditMoyenTermeMapper;
    private final EcheanceCMTService echeanceCmtService;

    public CreditMoyenTermeService(CreditMoyenTermeRepository creditMoyenTermeRepository,
                                   BanqueRepository banqueRepository,
                                   CreditMoyenTermeMapper creditMoyenTermeMapper,
                                   EcheanceCMTService echeanceCmtService) {
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.banqueRepository = banqueRepository;
        this.creditMoyenTermeMapper = creditMoyenTermeMapper;
        this.echeanceCmtService = echeanceCmtService;
    }

    @Transactional
    public CreditMoyenTermeResponseDto create(CreditMoyenTermeRequestDto requestDto) {
        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque introuvable avec l'ID : " + requestDto.getBanqueId()));

        CreditMoyenTerme credit = creditMoyenTermeMapper.toEntity(requestDto, banque);
        CreditMoyenTerme savedCredit = creditMoyenTermeRepository.save(credit);

        echeanceCmtService.generateEcheances(savedCredit);

        return creditMoyenTermeMapper.toResponseDto(savedCredit);
    }

    @Transactional(readOnly = true)
    public CreditMoyenTermeResponseDto findById(Long id) {
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));
        return creditMoyenTermeMapper.toResponseDto(credit);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchActiveCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getActiveCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);
        return creditMoyenTermeRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchPendingClosureCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getPendingClosureCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);
        return creditMoyenTermeRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional(readOnly = true)
    public Page<CreditMoyenTermeResponseDto> searchArchivedCredits(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        Specification<CreditMoyenTerme> statusSpec = CreditMoyenTermeSpecification.getArchivedCredits();
        Specification<CreditMoyenTerme> searchSpec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);
        return creditMoyenTermeRepository.findAll(Specification.where(statusSpec).and(searchSpec), pageable)
                .map(creditMoyenTermeMapper::toResponseDto);
    }

    @Transactional
    public CreditMoyenTermeResponseDto update(Long id, CreditMoyenTermeRequestDto requestDto) {
        CreditMoyenTerme existingCredit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));

        boolean changesAffectEcheances =
                !existingCredit.getDuree().equals(requestDto.getDuree()) ||
                        !existingCredit.getPeriodicite().equals(requestDto.getPeriodicite()) ||
                        !existingCredit.getEcheance().equals(requestDto.getEcheance()) ||
                        !existingCredit.getDateDebut().toInstant().equals(requestDto.getDateDebut().toInstant());

        Banque banque = banqueRepository.findById(requestDto.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque introuvable avec l'ID : " + requestDto.getBanqueId()));

        creditMoyenTermeMapper.updateEntity(requestDto, existingCredit, banque);

        CreditMoyenTerme updatedCredit = creditMoyenTermeRepository.save(existingCredit);

        if (changesAffectEcheances) {
            echeanceCmtService.regenerateEcheances(updatedCredit.getId());
        }

        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }

    @Transactional
    public DeleteResponceDto delete(Long id) {
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));

        if (echeanceCmtService.hasPaidEcheances(id)) {
            throw new ValidationException("Impossible de supprimer le crédit car certaines de ses échéances ont déjà été payées.");
        }

        creditMoyenTermeRepository.delete(credit);
        return new DeleteResponceDto("Credit moyen terme deleted");
    }


    @Transactional
    public CreditMoyenTermeResponseDto markAsArchived(Long id) {
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));

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

        return creditMoyenTermeMapper.toResponseDto(updatedCredit);
    }
}