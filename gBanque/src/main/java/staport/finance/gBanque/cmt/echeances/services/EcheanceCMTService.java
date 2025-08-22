package staport.finance.gBanque.cmt.echeances.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.cmt.echeances.dtos.*;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;
import staport.finance.gBanque.cmt.echeances.mappers.EcheanceCMTMapper;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.specification.EcheanceCMTSpecification;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.cmt.repositories.CreditMoyenTermeRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * Service pour la gestion des échéances d'un crédit à moyen terme (CMT).
 */
@Service
@Transactional(readOnly = true)
public class EcheanceCMTService {

    private final EcheanceCMTRepository echeanceCmtRepository;
    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final BanqueRepository banqueRepository;
    private final EcheanceCMTMapper echeanceCmtMapper;


    private static final int SCALE = 2;
    @Value("${application-start-date}")
    private OffsetDateTime dateDeDepartPaymentLeasing;

    public EcheanceCMTService(EcheanceCMTRepository echeanceCmtRepository,
                              CreditMoyenTermeRepository creditMoyenTermeRepository,
                              BanqueRepository banqueRepository,
                              EcheanceCMTMapper echeanceCmtMapper) {
        this.echeanceCmtRepository = echeanceCmtRepository;
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.banqueRepository = banqueRepository;
        this.echeanceCmtMapper = echeanceCmtMapper;

    }

    @Transactional
    public void generateEcheances(CreditMoyenTerme credit) {

        if (credit.getDuree() == null || credit.getDuree() <= 0 ||
                credit.getPeriodicite() == null || credit.getPeriodicite() <= 0 ||
                credit.getDateDebut() == null ||
                credit.getEcheance() == null || credit.getEcheance().signum() <= 0) {
            throw new ValidationException("Les données du crédit sont invalides pour la génération des échéances.");
        }

        List<EcheanceCMT> echeances = new ArrayList<>();
        OffsetDateTime dateEcheanceCourante = credit.getDateDebut();

        for (int i = 1; i <= credit.getDuree(); i++) {
            if (i > 1) {
                dateEcheanceCourante = dateEcheanceCourante.plusMonths(credit.getPeriodicite());
            }

            StatutEcheanceCMT statut = dateEcheanceCourante.isBefore(dateDeDepartPaymentLeasing)
                    ? StatutEcheanceCMT.PAYE
                    : StatutEcheanceCMT.PREVU;

            EcheanceCMT echeance = new EcheanceCMT();
            echeance.setCreditMoyenTerme(credit);
            echeance.setNumeroEcheance(i);
            echeance.setDateEcheance(dateEcheanceCourante);
            echeance.setMontant(credit.getEcheance());
            echeance.setStatutPaiement(statut);

            echeances.add(echeance);
        }

        echeanceCmtRepository.saveAll(echeances);
    }

    @Transactional
    public void regenerateEcheances(Long creditId) {
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId));

        if (hasPaidEcheances(creditId)) {
            throw new ValidationException("Impossible de régénérer les échéances car certaines ont déjà été payées.");
        }

        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);
        generateEcheances(credit);
    }

    @Transactional
    public EcheanceCMTResponseDto markEcheanceAsPaid(Long echeanceId) {
        EcheanceCMT echeance = echeanceCmtRepository.findById(echeanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Echeance avec l'ID " + echeanceId + " introuvable."));

        if (echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE) {
            throw new ValidationException("L'échéance avec l'ID " + echeanceId + " est déjà marquée comme payée.");
        }

        CreditMoyenTerme credit = echeance.getCreditMoyenTerme();
        if (credit == null) {
            throw new ResourceNotFoundException("L'échéance " + echeanceId + " n'est associée à aucun crédit.");
        }

        Banque banqueDePrelevement = credit.getBanque();
        if (banqueDePrelevement == null) {
            throw new ResourceNotFoundException("Le crédit " + credit.getId() + " n'a pas de banque de prélèvement associée.");
        }

        echeance.setStatutPaiement(StatutEcheanceCMT.PAYE);
        echeance = echeanceCmtRepository.save(echeance);

        banqueDePrelevement.setSoldeActuel(banqueDePrelevement.getSoldeActuel().subtract(echeance.getMontant()));
        banqueRepository.save(banqueDePrelevement);



        return echeanceCmtMapper.toResponseDto(echeance);
    }

    public TableauAmortissementCMTResponseDto getTableauAmortissementForCMT(Long creditId, EcheanceCMTSearchDto searchDto, Pageable pageable) {
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId));

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

        if (searchDto == null) {
            searchDto = new EcheanceCMTSearchDto();
        }
        searchDto.setCreditMoyenTermeId(creditId);
        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.withSearchCriteria(searchDto);

        Page<EcheanceCMT> echeancesPage = echeanceCmtRepository.findAll(spec, pageable);

        Page<EcheanceCMTResponseDto> echeancesDtoPage = echeanceCmtMapper.toResponseDtoPage(echeancesPage);

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
        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId));
        return echeanceCmtRepository.findByCreditMoyenTerme(credit).stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE);
    }

    @Transactional(readOnly = true)
    public EncoursCMTResponseDTO getEncoursCMTForBanque(EncoursCMTRequestDTO requestDTO, Pageable pageable) {
        Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                .orElseThrow(() -> new ResourceNotFoundException("Banque avec l'ID " + requestDTO.getBanqueId() + " introuvable."));

        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.byCreditBanqueId(requestDTO.getBanqueId())
                .and(EcheanceCMTSpecification.outstandingUntil(requestDTO.getPeriode(), OffsetDateTime.now()));

        Page<EcheanceCMT> encoursEcheancesPage = echeanceCmtRepository.findAll(spec, pageable);
        Page<EcheanceCMTResponseDto> echeancesDtoPage = encoursEcheancesPage.map(echeanceCmtMapper::toResponseDto);

        List<EcheanceCMT> allOutstandingEcheances = echeanceCmtRepository.findAll(spec);

        BigDecimal totalEncours = allOutstandingEcheances.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        long nombreCreditsConcernes = allOutstandingEcheances.stream()
                .map(echeance -> echeance.getCreditMoyenTerme().getId())
                .distinct()
                .count();

        long nombreEcheancesEnAttente = allOutstandingEcheances.size();

        return new EncoursCMTResponseDTO(
                banque.getId(),
                banque.getNom(),
                totalEncours,
                nombreEcheancesEnAttente,
                nombreCreditsConcernes,
                echeancesDtoPage
        );
    }


}