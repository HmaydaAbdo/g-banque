package staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.controllers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.dtos.RecaputilatifDecaissementItem;
import staport.finance.gBanque.lesEtatsDesEncours.recapitulatif.decaissement.services.RecapitulatifDecaissementService;

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
     * Retrieves a pageable and sortable summary of all expected disbursements
     * for the current month.
     *
     * @param pageable A Spring Data Pageable object, automatically created from
     * URL query parameters like `page`, `size`, and `sort`.
     * @return A ResponseEntity containing a consolidated Page of disbursement items and an HTTP status of OK.
     */
    @GetMapping
    public ResponseEntity<Page<RecaputilatifDecaissementItem>> getRecapitulatifDecaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<RecaputilatifDecaissementItem> decaissementItems =
                recapitulatifDecaissementService.getRecapitulatifDecaissementForCurrentMonth(pageable);

        return new ResponseEntity<>(decaissementItems, HttpStatus.OK);
    }
}

