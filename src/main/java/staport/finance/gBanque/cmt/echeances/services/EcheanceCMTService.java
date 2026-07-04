package staport.finance.gBanque.cmt.echeances.services;


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
import staport.finance.gBanque.cmt.echeances.dtos.*;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.mappers.EcheanceCMTMapper;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.specification.EcheanceCMTSpecification;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;


/**
 * Service pour la gestion des échéances d'un crédit à moyen terme (CMT).
 */
@Service
@Transactional(readOnly = true)
public class EcheanceCMTService {

    private static final Logger log = LoggerFactory.getLogger(EcheanceCMTService.class);
    private static final int SCALE = 2;

    private final EcheanceCMTRepository echeanceCmtRepository;
    private final IBanqueService banqueService;
    private final EcheanceCMTMapper echeanceCmtMapper;

    public EcheanceCMTService(EcheanceCMTRepository echeanceCmtRepository,
                              IBanqueService banqueService,
                              EcheanceCMTMapper echeanceCmtMapper) {
        this.echeanceCmtRepository = echeanceCmtRepository;
        this.banqueService = banqueService;
        this.echeanceCmtMapper = echeanceCmtMapper;
    }

    @Transactional
    public void generateEcheances(CreditMoyenTerme credit) {
        LocalDate dateDebut = credit.getDateDebut();
        List<EcheanceCMT> echeances = new ArrayList<>();

        for (int i = 1; i <= credit.getDuree(); i++) {
            LocalDate dateEcheanceCourante = dateDebut.plusMonths((long) (i - 1) * credit.getPeriodicite());

            EcheanceCMT echeance = new EcheanceCMT();
            echeance.setCreditMoyenTerme(credit);
            echeance.setNumeroEcheance(i);
            echeance.setDateEcheance(dateEcheanceCourante);
            echeance.setMontant(credit.getEcheance());
            echeance.setStatutPaiement(StatutEcheanceCMT.PREVU);
            echeances.add(echeance);
        }

        echeanceCmtRepository.saveAll(echeances);
        log.info("{} échéance(s) générée(s) pour le crédit CMT ID: {}", echeances.size(), credit.getId());
    }

    @Transactional
    public void regenerateEcheances(CreditMoyenTerme credit) {
        if (hasPaidEcheances(credit.getId())) {
            throw new ValidationException("Impossible de régénérer les échéances car certaines ont déjà été payées.");
        }

        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);
        generateEcheances(credit);
        log.info("Échéances régénérées pour le crédit CMT ID: {}", credit.getId());
    }

    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceCMT.class)
    public EcheanceCMTResponseDto toggleStatus(Long echeanceId) {
        EcheanceCMT echeance = echeanceCmtRepository.findById(echeanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance avec l'ID " + echeanceId + " introuvable."));

        StatutEcheanceCMT newStatus = echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE
                ? StatutEcheanceCMT.PREVU
                : StatutEcheanceCMT.PAYE;

        echeance.setStatutPaiement(newStatus);
        echeance = echeanceCmtRepository.save(echeance);

        log.info("Statut de l'échéance CMT ID: {} modifié — nouveau statut: {}", echeanceId, newStatus);
        return echeanceCmtMapper.toResponseDto(echeance);
    }

    /**
     * Met à jour les champs d'une échéance dont le statut est {@code PREVU}.
     * Toute tentative de modification d'une échéance {@code PAYE} lève une {@link ValidationException}.
     *
     * @param id  L'identifiant de l'échéance à modifier
     * @param dto Les nouvelles valeurs des champs
     * @return Le DTO de l'échéance mise à jour
     */
    @Transactional
    @AuditableAction(operation = AuditOperation.UPDATE, entity = EcheanceCMT.class)
    public EcheanceCMTResponseDto updateEcheance(Long id, EcheanceCMTUpdateRequestDto dto) {
        EcheanceCMT echeance = echeanceCmtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance avec l'ID " + id + " introuvable."));

        if (echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE) {
            log.warn("Tentative de modification d'une échéance CMT déjà payée — ID: {}", id);
            throw new ValidationException("Impossible de modifier une échéance dont le statut est PAYE.");
        }

        echeance.setDateEcheance(dto.dateEcheance());
        echeance.setMontant(dto.montant());
        echeance.setDateOperation(dto.dateOperation());
        echeance.setDateValeur(dto.dateValeur());
        if (dto.observation() != null) echeance.setObservation(dto.observation());
        if (dto.libelle() != null) echeance.setLibelle(dto.libelle());

        EcheanceCMT saved = echeanceCmtRepository.save(echeance);
        log.info("Échéance CMT ID: {} mise à jour avec succès", id);
        return echeanceCmtMapper.toResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public TableauAmortissementCMTResponseDto getTableauAmortissementForCMT(CreditMoyenTerme credit, EcheanceCMTSearchDto searchDto, Pageable pageable) {
        Long creditId = credit.getId();
        List<EcheanceCMT> allEcheancesForTotals = echeanceCmtRepository.findByCreditMoyenTerme(credit);

        BigDecimal montantTotalAPayer = allEcheancesForTotals.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montantTotalDejaPaye = allEcheancesForTotals.stream()
                .filter(e -> e.getStatutPaiement() == StatutEcheanceCMT.PAYE)
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal montantRestantAPayer = montantTotalAPayer.subtract(montantTotalDejaPaye);

        EcheanceCMTSearchDto effectiveSearchDto = (searchDto == null ? new EcheanceCMTSearchDto() : searchDto)
                .withCreditMoyenTermeId(creditId);
        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.searchByCriteria(effectiveSearchDto);

        Page<EcheanceCMT> echeancesPage = echeanceCmtRepository.findAll(spec, defaultSort(pageable));
        Page<EcheanceCMTResponseDto> echeancesDtoPage = echeancesPage.map(echeanceCmtMapper::toResponseDto);

        log.info("Tableau d'amortissement récupéré pour le crédit CMT ID: {} — {} échéance(s)", creditId, echeancesPage.getTotalElements());

        return new TableauAmortissementCMTResponseDto(
                credit.getId(),
                credit.getDesignation(),
                montantTotalAPayer,
                montantTotalDejaPaye,
                montantRestantAPayer,
                echeancesDtoPage
        );
    }

    public boolean hasPaidEcheances(Long creditId) {
        return echeanceCmtRepository.existsByCreditMoyenTermeIdAndStatutPaiement(creditId, StatutEcheanceCMT.PAYE);
    }

    @Transactional(readOnly = true)
    public EncoursCMTResponseDTO getEncoursCMTForBanque(EncoursCMTRequestDTO requestDTO, Pageable pageable) {
        Banque banque = banqueService.findOrThrow(requestDTO.banqueId());

        LocalDate finDeMoisCourant = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.byCreditBanqueId(requestDTO.banqueId())
                .and(EcheanceCMTSpecification.outstandingUntil(finDeMoisCourant))
                .and(EcheanceCMTSpecification.byCreditDesignationLike(requestDTO.designation()))
                .and(EcheanceCMTSpecification.byMontant(requestDTO.montant()));

        Page<EcheanceCMT> encoursEcheancesPage = echeanceCmtRepository.findAll(spec, defaultSort(pageable));
        Page<EcheanceCMTResponseDto> echeancesDtoPage = encoursEcheancesPage.map(echeanceCmtMapper::toResponseDto);

        List<EcheanceCMT> allOutstandingEcheances = echeanceCmtRepository.findAll(spec);
        BigDecimal totalEncours = allOutstandingEcheances.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Encours CMT calculés — banqueId: {}, total: {}", banque.getId(), totalEncours);

        return new EncoursCMTResponseDTO(
                banque.getId(),
                banque.getNom(),
                totalEncours,
                echeancesDtoPage
        );
    }


    /**
     * Supprime toutes les échéances d'un crédit à moyen terme.
     * Utilisé lors de la suppression complète d'un crédit CMT.
     *
     * @param credit Le crédit dont les échéances doivent être supprimées
     * @return Le nombre d'échéances supprimées
     */
    @Transactional
    public int deleteEcheancesByCreditMoyenTerme(CreditMoyenTerme credit) {
        List<EcheanceCMT> echeances = echeanceCmtRepository.findByCreditMoyenTerme(credit);
        int nombreEcheances = echeances.size();

        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);
        log.info("{} échéance(s) supprimée(s) pour le crédit CMT ID : {}", nombreEcheances, credit.getId());

        return nombreEcheances;
    }

    private Pageable defaultSort(Pageable pageable) {
        return pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id"));
    }
}
