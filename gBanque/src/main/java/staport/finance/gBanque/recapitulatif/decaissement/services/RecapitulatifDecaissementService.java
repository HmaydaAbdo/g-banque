package staport.finance.gBanque.recapitulatif.decaissement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.cheques.entities.Cheque;
import staport.finance.gBanque.cheques.repositories.ChequeRepository;
import staport.finance.gBanque.cheques.specs.ChequeSpec;
import staport.finance.gBanque.cmt.echeances.dtos.PeriodeEncoursCMT;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.specification.EcheanceCMTSpecification;
import staport.finance.gBanque.lcn.entities.Lcn;
import staport.finance.gBanque.lcn.repositories.LcnRepository;
import staport.finance.gBanque.lcn.specs.LcnSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.PeriodeEncours;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
import staport.finance.gBanque.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.miseADisposition.repositories.MiseADispositionRepository;
import staport.finance.gBanque.miseADisposition.specs.MiseADispositionSpec;
import staport.finance.gBanque.recapitulatif.decaissement.dtos.RecaputilatifDecaissementItem;
import staport.finance.gBanque.recapitulatif.decaissement.mappers.RecapitulatifDecaissementMapper;
import staport.finance.gBanque.virements.entities.Virement;
import staport.finance.gBanque.virements.repositories.VirementRepository;
import staport.finance.gBanque.virements.specs.VirementSpec;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Service to generate a consolidated summary of all expected disbursements
 * (leasing and CMT installments) for the current month.
 */
@Service
@Transactional(readOnly = true)
public class RecapitulatifDecaissementService {

    private final EcheanceLeasingRepository echeanceLeasingRepository;
    private final EcheanceCMTRepository echeanceCMTRepository;
    private final RecapitulatifDecaissementMapper mapper;
    private final ChequeRepository chequeRepository;
    private final LcnRepository lcnRepository;
    private final VirementRepository virementRepository;
    private final MiseADispositionRepository miseADispositionRepository;
    public RecapitulatifDecaissementService(
            EcheanceLeasingRepository echeanceLeasingRepository,
            EcheanceCMTRepository echeanceCMTRepository,
            RecapitulatifDecaissementMapper mapper, ChequeRepository chequeRepository, LcnRepository lcnRepository, VirementRepository virementRepository, MiseADispositionRepository miseADispositionRepository) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.mapper = mapper;
        this.chequeRepository = chequeRepository;
        this.lcnRepository = lcnRepository;
        this.virementRepository = virementRepository;
        this.miseADispositionRepository = miseADispositionRepository;
    }

    /**
     * Retrieves a pageable and sortable list of outstanding (Prévu) disbursements
     * for the current month.
     *
     * @param banqueId The id of the bank to filter by.
     * @param pageable A Spring data Pageable object for pagination and sorting.
     * @return A consolidated Page of disbursement items.
     */
    public Page<RecaputilatifDecaissementItem> getRecapitulatifDecaissementForCurrentMonthForGivenBanque(Pageable pageable,Long banqueId) {

        // Step 1: Create specifications for each entity type, filtering by bank and current month.
        Specification<EcheanceLeasing> leasingSpec = Specification.where(EcheanceLeasingSpecification.outstandingUntil(PeriodeEncours.MOIS, OffsetDateTime.now()))
                .and(EcheanceLeasingSpecification.byContratBanqueId(banqueId));

        Specification<EcheanceCMT> cmtSpec = Specification.where(EcheanceCMTSpecification.outstandingUntil(PeriodeEncoursCMT.MOIS, OffsetDateTime.now()))
                .and(EcheanceCMTSpecification.byCreditBanqueId(banqueId));

        Specification<Cheque> chequeSpec = ChequeSpec.activeForBankAndUntilEndOfMonth(banqueId);
        Specification<Lcn> lcnSpec = LcnSpecification.activeForBankAndUntilEndOfMonth(banqueId);
        Specification<Virement> virementSpec = VirementSpec.activeForBankAndUntilEndOfMonth(banqueId);
        Specification<MiseADisposition> madSpec = MiseADispositionSpec.activeForBankAndUntilEndOfMonth(banqueId);

        // Step 2: Fetch all relevant data for the current month from all repositories.
        List<EcheanceLeasing> outstandingLeasingPayments = echeanceLeasingRepository.findAll(leasingSpec);
        List<EcheanceCMT> outstandingCmtPayments = echeanceCMTRepository.findAll(cmtSpec);
        List<Cheque> activeCheques = chequeRepository.findAll(chequeSpec);
        List<Lcn> activeLcns = lcnRepository.findAll(lcnSpec);
        List<Virement> activeVirements = virementRepository.findAll(virementSpec);
        List<MiseADisposition> activeMisesADisposition = miseADispositionRepository.findAll(madSpec);

        // Step 3: Combine all lists and map them to the unified DTO.
        List<RecaputilatifDecaissementItem> combinedItems = Stream.concat(
                        outstandingLeasingPayments.stream().map(mapper::mapLeasingToDto),
                        outstandingCmtPayments.stream().map(mapper::mapCmtToDto)
                )
                .collect(Collectors.toList());

        // Add items from the new data sources.
        combinedItems.addAll(activeCheques.stream().map(mapper::mapChequeToDto).toList());
        combinedItems.addAll(activeLcns.stream().map(mapper::mapLcnToDto).toList());
        combinedItems.addAll(activeVirements.stream().map(mapper::mapVirementToDto).toList());
        combinedItems.addAll(activeMisesADisposition.stream().map(mapper::mapMiseADispositionToDto).toList());

        // Step 4: Sort the entire consolidated list based on the Pageable's sort information.
        Comparator<RecaputilatifDecaissementItem> comparator = getComparatorForSorting(pageable);
        combinedItems.sort(comparator);

        // Step 5: Manually apply pagination to the sorted list.
        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;
        List<RecaputilatifDecaissementItem> pagedItems;



        if (combinedItems.size() < startItem) {
            pagedItems = List.of();
        } else {
            int toIndex = Math.min(startItem + pageSize, combinedItems.size());
            pagedItems = combinedItems.subList(startItem, toIndex);
        }

        // Step 6: Wrap the sublist in a Page object and return it.
        return new PageImpl<>(pagedItems, pageable, combinedItems.size());
    }

    /**
     * Helper method to create a Comparator from the Pageable's Sort object.
     * This is required for sorting the combined list manually.
     */
    private Comparator<RecaputilatifDecaissementItem> getComparatorForSorting(Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            // Default sort if no sort is specified.
            return Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission);
        }

        return pageable.getSort().stream()
                .map(order -> {
                    Comparator<RecaputilatifDecaissementItem> comparator = switch (order.getProperty()) {
                        case "dateEmission" -> Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission);
                        case "libelle" -> Comparator.comparing(RecaputilatifDecaissementItem::getLibelle);
                        case "debit" -> Comparator.comparing(RecaputilatifDecaissementItem::getDebit);
                        default ->
                            // Fallback to the default sort to avoid a NullPointerException.
                                Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission);
                    };
                    return order.isDescending() ? comparator.reversed() : comparator;
                })
                .reduce(Comparator::thenComparing)
                .orElse(Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission));
    }
}
