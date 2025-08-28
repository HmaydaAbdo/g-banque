package staport.finance.gBanque.lesEtatsDesEncours.rapprochement.controler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.dtos.TransactionResponseDto;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.dtos.TransactionSearchDto;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.services.TransactionService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    /**
     * Retrieves a paginated and sorted list of all transaction history.
     * It allows for flexible filtering based on various criteria from the search DTO.
     *
     * @param searchDto A DTO containing optional search criteria passed as request parameters.
     * @param pageable A Spring Data Pageable object for pagination and sorting.
     * @return A ResponseEntity containing a Page of TransactionResponseDto and an HTTP status of OK.
     */
    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionHistory(
            TransactionSearchDto searchDto,
            Pageable pageable) {

        Page<TransactionResponseDto> transactions = transactionService.getTransactionHistory(searchDto, pageable);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }


    /**
     * Retrieves a paginated and sorted list of transactions for a specific bank in the current month.
     * Supports optional multi-search via a 'libelle' parameter.
     *
     * @param banqueId The ID of the bank, passed as a path variable.
     * @param libelle An optional keyword to search for in the transaction's libelle.
     * @param pageable A Spring data Pageable object for pagination and sorting.
     * @return A ResponseEntity containing a Page of Transaction entities and an HTTP status of OK.
     */
    @GetMapping("/{banqueId}/transactions")
    public ResponseEntity<Page<TransactionResponseDto>> getBankTransactions(
            @PathVariable Long banqueId,
            @RequestParam(required = false) String libelle,
            Pageable pageable) {

        TransactionSearchDto searchDto = new TransactionSearchDto();
        searchDto.setLibelle(libelle);

        Page<TransactionResponseDto> transactions = transactionService.getTransactionsByBanque(banqueId, searchDto, pageable);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }
}
