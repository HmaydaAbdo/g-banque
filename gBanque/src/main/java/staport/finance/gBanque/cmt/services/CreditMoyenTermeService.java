package staport.finance.gBanque.cmt.services;

import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cmt.dtos.*;
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

    public CreditMoyenTermeResponseDto findById(Long id) {
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + id));
        return creditMoyenTermeMapper.toResponseDto(credit);
    }

    public Page<CreditMoyenTermeResponseDto> searchOnActifOnly(CreditMoyenTermeSearchDto searchDto, Pageable pageable) {
        // 1. Crée une spécification à partir des critères de recherche existants
        Specification<CreditMoyenTerme> spec = CreditMoyenTermeSpecification.withSearchCriteria(searchDto);

        // 2. Ajoute un critère pour ne récupérer que les crédits non-terminés (completed = false)
        spec = spec.and((root, query, builder) -> builder.isFalse(root.get("completed")));

        // 3. Exécute la requête avec la spécification combinée
        Page<CreditMoyenTerme> creditsPage = creditMoyenTermeRepository.findAll(spec, pageable);

        // 4. Mappe le résultat en DTO
        return creditsPage.map(creditMoyenTermeMapper::toResponseDto);
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


    @EventListener
    @Transactional
    public void handleCreditCompletedEvent(CreditCompletedEvent event) {
        Long creditId = event.getCreditId();

        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId));

        if (!credit.isCompleted()) {
            credit.setCompleted(true);
            creditMoyenTermeRepository.save(credit);
        }
    }
}