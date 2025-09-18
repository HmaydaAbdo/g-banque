package staport.finance.gBanque.recapitulatif.controllers;


import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.recapitulatif.dtos.AgregatedRecapDto;
import staport.finance.gBanque.recapitulatif.dtos.DecaissementSearchDto;
import staport.finance.gBanque.recapitulatif.services.RecapitulatifService;

/**
 * REST Controller for the disbursement summary.
 * Exposes a paginated and sortable endpoint to retrieve a consolidated view
 * of all expected payments for the current month.
 */
@RestController
@RequestMapping("/recapitulatif")
public class RecapitulatifController {

    private final RecapitulatifService recapitulatifService;

    public RecapitulatifController(RecapitulatifService recapitulatifService) {
        this.recapitulatifService = recapitulatifService;
    }

    /**
     * Endpoint to retrieve the list of upcoming disbursements for a specific bank.
     * The banqueId is a mandatory path variable.
     * @param banqueId The ID of the bank to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of disbursement items.
     */
    @GetMapping("/decaissement/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatifDecaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId,@ModelAttribute DecaissementSearchDto searchDto) {

        searchDto.setBanqueId(banqueId);
        AgregatedRecapDto res =
                recapitulatifService.getRecapitulatifDecaissementForCurrentMonthForGivenBanque(pageable,searchDto);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    /**
     * Endpoint to retrieve the list of upcoming encaissement for a specific bank.
     * The banqueId is a mandatory path variable.
     * @param banqueId The ID of the bank to filter by.
     * @param pageable Pagination and sorting information.
     * @return A paginated list of disbursement items.
     */
    @GetMapping("/encaissement/banque/{banqueId}")
    public ResponseEntity<AgregatedRecapDto> getRecapitulatifEncaissement(
            @PageableDefault(sort = "dateEmission", direction = Sort.Direction.ASC) Pageable pageable,
            @PathVariable Long banqueId,@ModelAttribute DecaissementSearchDto searchDto) {

        searchDto.setBanqueId(banqueId);
        AgregatedRecapDto res = recapitulatifService.getRecapitulatifEncaissementForCurrentMonthForGivenBanque(pageable,searchDto);

        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}

