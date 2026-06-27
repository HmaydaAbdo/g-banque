package staport.finance.gBanque.Bank.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.dtos.response.BankAccueilResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.mappers.BanqueMapper;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.INatureLigneFonctionnelleService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.ILigneFonctionnelleService;
import staport.finance.gBanque.moyensDePaiements.lcn.services.LcnService;
import staport.finance.gBanque.moyensDePaiements.virements.service.VirementService;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;
import staport.finance.gBanque.rapprochement.services.RapprochementService;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService.FC_CODE;

/**
 * Agrège les données du tableau de bord accueil pour le module Bank — solde live,
 * plafond Facilité de Caisse, solde de début du rapprochement EN_COURS, échus LCN/Virements.
 *
 * <p>Cette agrégation traverse 5 domaines (Bank, LigneFonctionnelle, Rapprochement, Lcn,
 * Virement). Elle est volontairement séparée de {@link BanqueServiceImpl} (qui ne porte que
 * le CRUD pur de Banque) pour éviter une dépendance circulaire : {@code RapprochementService}
 * dépend déjà de {@link IBanqueService}, donc {@code BanqueServiceImpl} ne peut jamais
 * dépendre de {@code RapprochementService} en retour. Cette classe est un consommateur
 * terminal — rien ne dépend d'elle — donc aucun cycle.
 *
 * <p>Dépend exclusivement des Services des autres domaines, jamais de leurs Repository
 * (CLAUDE.md, "Encapsulation des domaines"). Sans interface dédiée, comme
 * {@code RecapitulatifService}/{@code AgiosOrchestrateurService} (même style dans ce repo
 * pour les agrégateurs cross-domaines).
 */
@Service
@Transactional(readOnly = true)
public class BankAccueilAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(BankAccueilAggregatorService.class);

    private final IBanqueService bankService;
    private final ITransactionService transactionService;
    private final ILigneFonctionnelleService ligneFonctionnelleService;
    private final INatureLigneFonctionnelleService natureLigneFonctionnelleService;
    private final RapprochementService rapprochementService;
    private final LcnService lcnService;
    private final VirementService virementService;
    private final BanqueMapper banqueMapper;

    public BankAccueilAggregatorService(IBanqueService bankService,
                                        ITransactionService transactionService,
                                        ILigneFonctionnelleService ligneFonctionnelleService,
                                        INatureLigneFonctionnelleService natureLigneFonctionnelleService,
                                        RapprochementService rapprochementService,
                                        LcnService lcnService,
                                        VirementService virementService,
                                        BanqueMapper banqueMapper) {
        this.bankService = bankService;
        this.transactionService = transactionService;
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.natureLigneFonctionnelleService = natureLigneFonctionnelleService;
        this.rapprochementService = rapprochementService;
        this.lcnService = lcnService;
        this.virementService = virementService;
        this.banqueMapper = banqueMapper;
    }

    /** Cartes récapitulatives accueil pour TOUTES les banques. */
    public List<BankAccueilResponse> getAccueilBankInfo() {
        log.debug("Agrégation accueil — toutes les banques");
        List<Banque> banques = bankService.getAllBanqueEntities();
        List<Long> ids = banques.stream().map(Banque::getId).toList();

        Map<Long, BigDecimal> balanceMap = transactionService.computeBalancesForBanks(ids);
        Map<Long, BigDecimal> plafondMap = ligneFonctionnelleService.getActivePlafondsForBanks(ids, FC_CODE);

        Map<Long, BigDecimal> soldeDebutMap = rapprochementService.findAllEnCours()
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getBanque().getId(),
                        r -> r.getSoldeDebut() != null ? r.getSoldeDebut() : BigDecimal.ZERO
                ));

        LocalDate today = LocalDate.now();
        Map<Long, Long> lcnCountMap = new HashMap<>();
        Map<Long, Long> virementCountMap = new HashMap<>();

        lcnService.countEchusGroupedByBanque(today)
                .forEach(row -> lcnCountMap.put((Long) row[0], (Long) row[1]));

        virementService.countEchusGroupedByBanque(today)
                .forEach(row -> virementCountMap.put((Long) row[0], (Long) row[1]));

        return banques.stream()
                .map(b -> {
                    Long bid = b.getId();
                    return banqueMapper.toAccueilDto(b,
                            balanceMap.get(bid),
                            plafondMap.get(bid),
                            soldeDebutMap.getOrDefault(bid, BigDecimal.ZERO),
                            lcnCountMap.getOrDefault(bid, 0L),
                            virementCountMap.getOrDefault(bid, 0L));
                })
                .collect(Collectors.toList());
    }

    /** Carte récapitulative accueil pour une seule banque. */
    public BankAccueilResponse getAccueilBankInfoById(Long banqueId) {
        log.debug("Agrégation accueil — banque ID: {}", banqueId);
        Banque banque = bankService.findOrThrow(banqueId);
        Long fcNatureId = natureLigneFonctionnelleService.findFaciliteDeCaisseNatureId();

        LocalDate today = LocalDate.now();

        BigDecimal balance   = transactionService.computeBalance(banqueId);
        BigDecimal plafond   = ligneFonctionnelleService.getPlafondFCInternal(banqueId, fcNatureId);
        BigDecimal soldeDebut = rapprochementService.findEnCoursByBanqueId(banqueId)
                .map(Rapprochement::getSoldeDebut)
                .map(s -> s != null ? s : BigDecimal.ZERO)
                .orElse(BigDecimal.ZERO);

        // Requêtes ciblées sur la banque — évite de charger les échus de toutes les banques
        long lcnsCount      = lcnService.countEchusByBanqueId(banqueId, today);
        long virementsCount = virementService.countEchusByBanqueId(banqueId, today);

        return banqueMapper.toAccueilDto(banque, balance, plafond, soldeDebut, lcnsCount, virementsCount);
    }
}
