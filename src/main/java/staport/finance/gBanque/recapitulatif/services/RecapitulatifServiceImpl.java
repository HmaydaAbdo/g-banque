package staport.finance.gBanque.recapitulatif.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.services.IBanqueService;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.recapitulatif.dtos.request.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.dtos.response.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifGlobal;
import staport.finance.gBanque.recapitulatif.dtos.response.RecaputilatifItem;
import staport.finance.gBanque.recapitulatif.entities.RecapitulatifEncoursView;
import staport.finance.gBanque.recapitulatif.mappers.RecapitulatifEncoursMapper;
import staport.finance.gBanque.recapitulatif.repositories.RecapitulatifEncoursRepository;
import staport.finance.gBanque.recapitulatif.services.interfaces.IRecapitulatifService;
import staport.finance.gBanque.recapitulatif.specs.RecapitulatifEncoursSpecification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service pour générer un récapitulatif consolidé de tous les décaissements/encaissements
 * prévisionnels (échéances leasing/CMT, moyens de paiement, agios, commissions, etc.) pour
 * le mois en cours.
 *
 * <p>Interroge {@link RecapitulatifEncoursRepository}, adossé à la vue SQL
 * {@code v_recapitulatif_encours} — pagination, tri et agrégation réels en base, remplaçant
 * l'ancienne implémentation qui chargeait ~17 repositories en mémoire (voir historique dans
 * {@code modules_sweep.md}, sweep États des encours / Récapitulatif, Phase 2, 17/07/2026).</p>
 */
@Service
@Transactional(readOnly = true)
public class RecapitulatifServiceImpl implements IRecapitulatifService {

    private final RecapitulatifEncoursRepository encoursRepository;
    private final RecapitulatifEncoursMapper mapper;
    private final IBanqueService banqueService;

    private final LocalDate endOfMonth = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

    public RecapitulatifServiceImpl(
            RecapitulatifEncoursRepository encoursRepository,
            RecapitulatifEncoursMapper mapper,
            IBanqueService banqueService) {
        this.encoursRepository = encoursRepository;
        this.mapper = mapper;
        this.banqueService = banqueService;
    }

    @Override
    public AgregatedRecapDto getRecapitulatifEncaissementForCurrentMonthForGivenBanque(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId) {
        return buildAgregatedRecap(pageable, searchDto, banqueId, Type.ENCAISSEMENT);
    }

    @Override
    public AgregatedRecapDto getRecapitulatifDecaissementForCurrentMonthForGivenBanque(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId) {
        return buildAgregatedRecap(pageable, searchDto, banqueId, Type.DECAISSEMENT);
    }

    private AgregatedRecapDto buildAgregatedRecap(
            Pageable pageable, DecaissementSearchDto searchDto, Long banqueId, Type sens) {

        Specification<RecapitulatifEncoursView> spec =
                RecapitulatifEncoursSpecification.searchByCriteria(searchDto, banqueId, sens, endOfMonth);

        Page<RecaputilatifItem> page = encoursRepository.findAll(spec, normalizeSort(pageable))
                .map(mapper::toDto);

        BigDecimal total = encoursRepository.sumMontant(spec);
        BigDecimal totalDebit = sens == Type.DECAISSEMENT ? total : BigDecimal.ZERO;
        BigDecimal totalCredit = sens == Type.ENCAISSEMENT ? total : BigDecimal.ZERO;

        return new AgregatedRecapDto(totalDebit, totalCredit, page);
    }

    /**
     * Les colonnes triables côté Angular (recap-decaissement/recap-encaissements) incluent
     * "debit"/"credit"/"dateEcheance", qui n'existent pas comme propriétés distinctes sur
     * {@link RecapitulatifEncoursView} (une seule colonne "montant", direction déjà fixée par le
     * filtre "sens" ; "dateEcheance" est exposée sous "dateEcheanceAffichage"). On les retraduit
     * ici plutôt que de les exposer tel quel côté base pour ne pas changer le contrat frontend.
     */
    private Pageable normalizeSort(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            return pageable;
        }
        Sort normalized = Sort.by(pageable.getSort().stream()
                .map(order -> new Sort.Order(order.getDirection(), mapSortProperty(order.getProperty())))
                .toList());
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), normalized);
    }

    private String mapSortProperty(String property) {
        return switch (property) {
            case "debit", "credit" -> "montant";
            case "dateEcheance" -> "dateEcheanceAffichage";
            default -> property;
        };
    }

    @Override
    public RecaputilatifGlobal getRecapitulatifGlobal(Long banqueId) {
        BanqueResponse banque = this.banqueService.getBanqueById(banqueId);
        BigDecimal plafondFc = banqueService.getPlafondFC(banqueId);

        List<RecaputilatifItem> decaissements = fetchOutstanding(banqueId, Type.DECAISSEMENT);
        List<RecaputilatifItem> encaissements = fetchOutstanding(banqueId, Type.ENCAISSEMENT);

        return new RecaputilatifGlobal(
                banque.id(),
                banque.nom(),
                banque.soldeActuel(),
                plafondFc.negate(),
                encaissements,
                decaissements
        );
    }

    private List<RecaputilatifItem> fetchOutstanding(Long banqueId, Type sens) {
        Specification<RecapitulatifEncoursView> spec = Specification
                .where(RecapitulatifEncoursSpecification.byBanqueId(banqueId))
                .and(RecapitulatifEncoursSpecification.bySens(sens))
                .and(RecapitulatifEncoursSpecification.outstandingUntil(endOfMonth));

        return encoursRepository.findAll(spec).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public List<RecaputilatifGlobal> getAllRecapitulatifsGlobaux() {
        List<Long> allBankIds = this.banqueService.getAllBanqueIds();

        return allBankIds.stream()
                .map(this::getRecapitulatifGlobal)
                .collect(Collectors.toList());
    }
}
