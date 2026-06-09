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
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final EcheanceCMTRepository echeanceCmtRepository;
    private final CreditMoyenTermeRepository creditMoyenTermeRepository;
    private final BanqueRepository banqueRepository;
    private final EcheanceCMTMapper echeanceCmtMapper;
    LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());
    private static final int SCALE = 2;

    public EcheanceCMTService(EcheanceCMTRepository echeanceCmtRepository,
                              CreditMoyenTermeRepository creditMoyenTermeRepository,
                              BanqueRepository banqueRepository,
                              EcheanceCMTMapper echeanceCmtMapper) {
        this.echeanceCmtRepository = echeanceCmtRepository;
        this.creditMoyenTermeRepository = creditMoyenTermeRepository;
        this.banqueRepository = banqueRepository;
        this.echeanceCmtMapper = echeanceCmtMapper;
        log.info("Initialisation du service EcheanceCMTService");

    }


    @Transactional
    public void generateEcheances(CreditMoyenTerme credit) {

        log.info("========== DÉBUT GÉNÉRATION ÉCHÉANCES CMT ID: {} ==========", credit.getId());
        log.info("Paramètres - Durée: {} mois, Périodicité: {} mois, Montant: {}",
                credit.getDuree(), credit.getPeriodicite(), credit.getEcheance());

        // Date de début du crédit (déjà en LocalDate — pas de conversion timezone nécessaire)
        LocalDate dateDebut = credit.getDateDebut();
        log.info(" Date de début du crédit CMT : {}", dateDebut);

        List<EcheanceCMT> echeances = new ArrayList<>();

        log.info(" Génération de {} échéances...", credit.getDuree());

        for (int i = 1; i <= credit.getDuree(); i++) {

            LocalDate dateEcheanceCourante = dateDebut.plusMonths((long) (i - 1) * credit.getPeriodicite());

            //  LOG 5 : Chaque échéance générée (seulement les 5 premières et les 3 dernières)
            if (i <= 5 || i > credit.getDuree() - 3) {
                log.info("   ✓ Échéance {} : {} (calcul: dateDebut + {} mois)",
                        i,
                        dateEcheanceCourante,
                        (i - 1) * credit.getPeriodicite());
            } else if (i == 6) {
                log.info("   ... ({} échéances intermédiaires) ...", credit.getDuree() - 8);
            }

            StatutEcheanceCMT statut = StatutEcheanceCMT.PREVU;
            EcheanceCMT echeance = new EcheanceCMT();
            echeance.setCreditMoyenTerme(credit);
            echeance.setNumeroEcheance(i);
            echeance.setDateEcheance(dateEcheanceCourante);
            echeance.setMontant(credit.getEcheance());
            echeance.setStatutPaiement(statut);
            echeances.add(echeance);
        }

        echeanceCmtRepository.saveAll(echeances);
        log.info(" {} échéances sauvegardées avec succès pour le crédit ID: {}", echeances.size(), credit.getId());
        log.info("========== FIN GÉNÉRATION ÉCHÉANCES ==========\n");
    }

    @Transactional
    public void regenerateEcheances(Long creditId) {

        log.info("Début de régénération des échéances pour le crédit CMT ID: {}", creditId);

        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable lors de la régénération des échéances - ID: {}", creditId);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId);
                });

        log.debug("Vérification de l'existence d'échéances payées pour le crédit ID: {}", creditId);

        if (hasPaidEcheances(creditId)) {
            log.error("Tentative de régénération avec échéances payées - Crédit ID: {}", creditId);
            throw new ValidationException("Impossible de régénérer les échéances car certaines ont déjà été payées.");
        }

        log.debug("Suppression des anciennes échéances pour le crédit ID: {}", creditId);
        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);

        log.debug("Génération de nouvelles échéances pour le crédit ID: {}", creditId);
        generateEcheances(credit);
        log.info("Régénération des échéances terminée avec succès pour le crédit ID: {}", creditId);
    }

    @Transactional
    public EcheanceCMTResponseDto toggleStatus(Long echeanceId) {
        log.info("Changement de statut de l'échéance CMT ID: {}", echeanceId);

        EcheanceCMT echeance = echeanceCmtRepository.findById(echeanceId)
                .orElseThrow(() -> {
                    log.error("Échéance CMT introuvable lors du changement de statut - ID: {}", echeanceId);
                    return new ResourceNotFoundException("Echeance avec l'ID " + echeanceId + " introuvable.");
                });

        StatutEcheanceCMT ancienStatut = echeance.getStatutPaiement();
        StatutEcheanceCMT newStatus = ancienStatut == StatutEcheanceCMT.PAYE
                ? StatutEcheanceCMT.PREVU
                : StatutEcheanceCMT.PAYE;

        log.debug("Changement de statut - Échéance ID: {}, Ancien statut: {}, Nouveau statut: {}", echeanceId, ancienStatut, newStatus);

        echeance.setStatutPaiement(newStatus);
        echeance = echeanceCmtRepository.save(echeance);

        log.info("Statut de l'échéance modifié avec succès - ID: {}, Nouveau statut: {}", echeanceId, newStatus);

        return echeanceCmtMapper.toResponseDto(echeance);
    }

    @Transactional(readOnly = true)
    public TableauAmortissementCMTResponseDto getTableauAmortissementForCMT(Long creditId, EcheanceCMTSearchDto searchDto, Pageable pageable) {

        log.info("Récupération du tableau d'amortissement pour le crédit CMT ID: {}", creditId);
        log.debug("Critères de recherche: {}, Page: {}, Taille: {}", searchDto, pageable.getPageNumber(), pageable.getPageSize());

        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable lors de la récupération du tableau d'amortissement - ID: {}", creditId);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId);
                });

        log.debug("Calcul des totaux pour le crédit ID: {}", creditId);
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

        log.debug("Totaux calculés - Total à payer: {}, Déjà payé: {}, Restant: {}", montantTotalAPayer, montantTotalDejaPaye, montantRestantAPayer);

        if (searchDto == null) {
            searchDto = new EcheanceCMTSearchDto();
        }

        searchDto.setCreditMoyenTermeId(creditId);
        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.withSearchCriteria(searchDto);

        Page<EcheanceCMT> echeancesPage = echeanceCmtRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        log.debug("Échéances récupérées - {} échéances sur {} total", echeancesPage.getNumberOfElements(), echeancesPage.getTotalElements());

        Page<EcheanceCMTResponseDto> echeancesDtoPage = echeanceCmtMapper.toResponseDtoPage(echeancesPage);

        log.info("Tableau d'amortissement récupéré avec succès pour le crédit ID: {} - {} échéances", creditId, echeancesPage.getTotalElements());

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
        log.debug("Vérification de l'existence d'échéances payées pour le crédit ID: {}", creditId);

        CreditMoyenTerme credit = creditMoyenTermeRepository.findById(creditId)
                .orElseThrow(() -> {
                    log.error("Crédit CMT introuvable lors de la vérification des échéances payées - ID: {}", creditId);
                    return new ResourceNotFoundException("Credit moyen terme introuvable avec l'ID : " + creditId);
                });

        boolean hasPaid = echeanceCmtRepository.findByCreditMoyenTerme(credit).stream()
                .anyMatch(echeance -> echeance.getStatutPaiement() == StatutEcheanceCMT.PAYE);

        log.debug("Résultat de la vérification - Crédit ID: {}, A des échéances payées: {}", creditId, hasPaid);

        return hasPaid;
    }

    @Transactional(readOnly = true)
    public EncoursCMTResponseDTO getEncoursCMTForBanque(EncoursCMTRequestDTO requestDTO, Pageable pageable) {

        log.info("Récupération des encours CMT - Banque ID: {}", requestDTO.getBanqueId());
        log.debug("Détails de la requête: {}, Page: {}, Taille: {}", requestDTO, pageable.getPageNumber(), pageable.getPageSize());

        Banque banque = banqueRepository.findById(requestDTO.getBanqueId())
                .orElseThrow(() -> {
                    log.error("Banque introuvable lors de la récupération des encours - ID: {}", requestDTO.getBanqueId());
                    return new ResourceNotFoundException("Banque avec l'ID " + requestDTO.getBanqueId() + " introuvable.");
                });

        log.debug("Banque trouvée: {} - ID: {}", banque.getNom(), banque.getId());

        Specification<EcheanceCMT> spec = EcheanceCMTSpecification.byCreditBanqueId(requestDTO.getBanqueId())
                .and(EcheanceCMTSpecification.outstandingUntil(endOfMonth))
                .and(EcheanceCMTSpecification.byCreditDesignationLike(requestDTO.getDesignation()))
                .and(EcheanceCMTSpecification.byMontant(requestDTO.getMontant()));

        log.debug("Application des critères de recherche pour les encours");

        Page<EcheanceCMT> encoursEcheancesPage = echeanceCmtRepository.findAll(spec, pageable.getSort().isSorted() ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.ASC, "id")));
        Page<EcheanceCMTResponseDto> echeancesDtoPage = encoursEcheancesPage.map(echeanceCmtMapper::toResponseDto);

        log.debug("Échéances d'encours récupérées - {} échéances sur {} total", encoursEcheancesPage.getNumberOfElements(), encoursEcheancesPage.getTotalElements());

        List<EcheanceCMT> allOutstandingEcheances = echeanceCmtRepository.findAll(spec);

        BigDecimal totalEncours = allOutstandingEcheances.stream()
                .map(EcheanceCMT::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(SCALE, RoundingMode.HALF_UP);

        log.info("Encours CMT calculés avec succès - Banque: {}, Total encours: {}, Nombre d'échéances: {}", banque.getNom(), totalEncours, allOutstandingEcheances.size());
        log.debug("Total des encours: {}", totalEncours);

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
        log.info("Suppression de toutes les échéances pour le crédit CMT ID : {}", credit.getId());
        log.debug("Crédit concerné : {}", credit.getDesignation());

        // Récupérer le nombre d'échéances avant suppression
        List<EcheanceCMT> echeances = echeanceCmtRepository.findByCreditMoyenTerme(credit);
        int nombreEcheances = echeances.size();

        log.debug("Nombre d'échéances à supprimer : {}", nombreEcheances);

        // Supprimer toutes les échéances
        echeanceCmtRepository.deleteByCreditMoyenTerme(credit);

        log.info("{} échéance(s) supprimée(s) pour le crédit CMT ID : {}", nombreEcheances, credit.getId());

        return nombreEcheances;
    }


}