package staport.finance.gBanque.recapitulatif.decaissement.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.recapitulatif.decaissement.dtos.RecaputilatifDecaissementItem;
import staport.finance.gBanque.recapitulatif.decaissement.services.RecapitulatifDecaissementService;

/**
 * REST Controller for the disbursement summary.
 * Exposes a paginated and sortable endpoint to retrieve a consolidated view
 * of all expected payments for the current month.
 */
@RestController
@RequestMapping("/recapitulatif-decaissement")
public class RecapitulatifDecaissementController {

    private final RecapitulatifDecaissementService recapitulatifDecaissementService;

    public RecapitulatifDecaissementController(RecapitulatifDecaissementService recapitulatifDecaissementService) {
        this.recapitulatifDecaissementService = recapitulatifDecaissementService;
    }

    /**
     * Endpoint to retrieve the list of upcoming disbursements for a specific bank.
     * The banqueId is a mandatory path variable.
     * @param banqueId The ID of the bank to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of disbursement items.
     */
    @GetMapping("/banque/{banqueId}")
    public ResponseEntity<Page<RecaputilatifDecaissementItem>> getRecapitulatifDecaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId) {

        Page<RecaputilatifDecaissementItem> decaissementItems =
                recapitulatifDecaissementService.getRecapitulatifDecaissementForCurrentMonthForGivenBanque(pageable,banqueId);

        return new ResponseEntity<>(decaissementItems, HttpStatus.OK);
    }
}

