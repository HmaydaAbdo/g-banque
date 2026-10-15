package staport.finance.gBanque.recapitulatif.services;

import com.lowagie.text.DocumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.LigneFonctionnelle.generalLignes.services.ILigneFonctionnelleService;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanBanqueDto;
import staport.finance.gBanque.recapitulatif.dtos.response.PlanTresorerieDto;
import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;
import staport.finance.gBanque.recapitulatif.repositories.RecapitulatifEncoursRepository;
import staport.finance.gBanque.recapitulatif.services.PlanTresorerieCalculateur.FluxPrevu;
import staport.finance.gBanque.recapitulatif.services.PlanTresorerieCalculateur.SoldeBanque;
import staport.finance.gBanque.recapitulatif.services.interfaces.ITresoreriePrevisionnelleService;
import staport.finance.gBanque.reports.service.StaportPdfReportService;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.services.NatureLigneFonctionnelleService.FC_CODE;

/**
 * Trésorerie prévisionnelle : rassemble le solde actuel et le plafond de facilité de caisse de
 * chaque banque, et les encours non validés jusqu'à l'horizon (vue {@code v_recapitulatif_encours},
 * même source que l'écran Récapitulatifs), puis délègue le calcul à {@link PlanTresorerieCalculateur}.
 *
 * <p>Solde et plafond sont lus exactement comme pour les cartes de l'accueil
 * ({@code BankAccueilAggregatorService}) : une requête groupée pour toutes les banques, un plafond
 * absent (aucune ligne FC active) valant zéro — le solde prévu du plan est donc celui des cartes.</p>
 */
@Service
@Transactional(readOnly = true)
public class TresoreriePrevisionnelleServiceImpl implements ITresoreriePrevisionnelleService {

    private static final Logger log = LoggerFactory.getLogger(TresoreriePrevisionnelleServiceImpl.class);

    private final IBanqueService banqueService;
    private final ITransactionService transactionService;
    private final ILigneFonctionnelleService ligneFonctionnelleService;
    private final RecapitulatifEncoursRepository encoursRepository;
    private final StaportPdfReportService pdfReportService;

    public TresoreriePrevisionnelleServiceImpl(IBanqueService banqueService,
                                               ITransactionService transactionService,
                                               ILigneFonctionnelleService ligneFonctionnelleService,
                                               RecapitulatifEncoursRepository encoursRepository,
                                               StaportPdfReportService pdfReportService) {
        this.banqueService = banqueService;
        this.transactionService = transactionService;
        this.ligneFonctionnelleService = ligneFonctionnelleService;
        this.encoursRepository = encoursRepository;
        this.pdfReportService = pdfReportService;
    }

    @Override
    public PlanTresorerieDto getPlan(PeriodeRecap periode) {
        LocalDate aujourdHui = LocalDate.now();
        LocalDate horizon = periode.horizon(aujourdHui);
        log.debug("Plan de trésorerie — horizon {} ({})", periode, horizon);

        List<Banque> entites = banqueService.getAllBanqueEntities();
        List<Long> ids = entites.stream().map(Banque::getId).toList();
        Map<Long, BigDecimal> soldes = transactionService.computeBalancesForBanks(ids);
        Map<Long, BigDecimal> plafonds = ligneFonctionnelleService.getActivePlafondsForBanks(ids, FC_CODE);
        List<SoldeBanque> banques = entites.stream()
                .map(b -> new SoldeBanque(b.getId(), b.getNom(), b.getDevise() != null ? b.getDevise().name() : null,
                        soldes.get(b.getId()), plafonds.get(b.getId())))
                .toList();

        List<FluxPrevu> flux = encoursRepository.sumParBanqueSourceSensEtDateUntil(horizon).stream()
                .map(row -> new FluxPrevu(
                        (Long) row[0], (TransactionSource) row[1], (Type) row[2], (LocalDate) row[3], (BigDecimal) row[4]))
                .toList();

        return PlanTresorerieCalculateur.calculer(periode, aujourdHui, banques, flux);
    }

    @Override
    public byte[] exportPdf(PeriodeRecap periode, Long banqueId, String devise) {
        PlanTresorerieDto plan = getPlan(periode);
        PlanBanqueDto perimetre = banqueId == null ? groupe(plan, devise) : plan.banques().stream()
                .filter(b -> Objects.equals(b.banqueId(), banqueId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Banque introuvable : " + banqueId));
        try {
            return pdfReportService.generatePlanTresorerieReport(plan, perimetre);
        } catch (DocumentException e) {
            throw new IllegalStateException("Erreur lors de la génération du PDF du plan de trésorerie.", e);
        }
    }

    /** Total de la devise demandée ; sans devise, le premier (le dirham quand il existe). */
    private PlanBanqueDto groupe(PlanTresorerieDto plan, String devise) {
        if (devise == null || devise.isBlank()) {
            return plan.groupes().get(0);
        }
        return plan.groupes().stream()
                .filter(g -> g.devise().equalsIgnoreCase(devise))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Devise introuvable : " + devise));
    }
}
