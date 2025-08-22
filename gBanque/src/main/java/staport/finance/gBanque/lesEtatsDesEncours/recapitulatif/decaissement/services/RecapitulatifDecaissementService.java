package staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import staport.finance.gBanque.cmt.echeances.dtos.PeriodeEncoursCMT;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.echeances.repositories.EcheanceCMTRepository;
import staport.finance.gBanque.cmt.echeances.specification.EcheanceCMTSpecification;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.enums.PeriodeEncours;
import staport.finance.gBanque.leasing.echeancesLeasing.repositories.EcheanceLeasingRepository;
import staport.finance.gBanque.leasing.echeancesLeasing.specification.EcheanceLeasingSpecification;
import staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.dtos.RecaputilatifDecaissementItem;
import staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.mappers.RecapitulatifDecaissementMapper;

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

    public RecapitulatifDecaissementService(
            EcheanceLeasingRepository echeanceLeasingRepository,
            EcheanceCMTRepository echeanceCMTRepository,
            RecapitulatifDecaissementMapper mapper) {
        this.echeanceLeasingRepository = echeanceLeasingRepository;
        this.echeanceCMTRepository = echeanceCMTRepository;
        this.mapper = mapper;
    }

    /**
     * Retrieves a pageable and sortable list of outstanding (Prévu) disbursements
     * for the current month.
     *
     * @param pageable A Spring data Pageable object for pagination and sorting.
     * @return A consolidated Page of disbursement items.
     */
    public Page<RecaputilatifDecaissementItem> getRecapitulatifDecaissementForCurrentMonth(Pageable pageable) {

        // Step 1: Fetch all outstanding payments for the current month using the dedicated specifications.
        List<EcheanceLeasing> outstandingLeasingPayments = echeanceLeasingRepository.findAll(EcheanceLeasingSpecification.outstandingUntil(PeriodeEncours.MOIS, OffsetDateTime.now()));
        List<EcheanceCMT> outstandingCmtPayments = echeanceCMTRepository.findAll(EcheanceCMTSpecification.outstandingUntil(PeriodeEncoursCMT.MOIS, OffsetDateTime.now()));

        // Step 2: Combine the two lists and map them to the unified DTO.
        List<RecaputilatifDecaissementItem> combinedItems = Stream.concat(
                        outstandingLeasingPayments.stream().map(mapper::mapLeasingToDto),
                        outstandingCmtPayments.stream().map(mapper::mapCmtToDto)
                )
                .collect(Collectors.toList());

        // Step 3: Sort the entire combined list based on the Pageable's sort information.
        Comparator<RecaputilatifDecaissementItem> comparator = getComparatorForSorting(pageable);
        combinedItems.sort(comparator);

        // Step 4: Manually apply pagination to the sorted list.
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

        // Step 5: Wrap the sublist in a Page object and return it.
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
                    Comparator<RecaputilatifDecaissementItem> comparator = null;
                    switch (order.getProperty()) {
                        case "dateEmission":
                            comparator = Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission);
                            break;
                        case "libelle":
                            comparator = Comparator.comparing(RecaputilatifDecaissementItem::getLibelle);
                            break;
                        case "debit":
                            comparator = Comparator.comparing(RecaputilatifDecaissementItem::getDebit);
                            break;
                        default:
                            // Fallback to the default sort to avoid a NullPointerException.
                            comparator = Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission);
                            break;
                    }
                    return order.isDescending() ? comparator.reversed() : comparator;
                })
                .reduce(Comparator::thenComparing)
                .orElse(Comparator.comparing(RecaputilatifDecaissementItem::getDateEmission));
    }
}
