package staport.finance.gBanque.lesEtatsDesEncours.rapprochement.controler;


import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.dtos.*;
import staport.finance.gBanque.lesEtatsDesEncours.rapprochement.services.RapprochementService;

@RestController
@RequestMapping("/rapprochement")
public class RapprochementBancaireController {

    private final RapprochementService rapprochementService;

    public RapprochementBancaireController(RapprochementService rapprochementService) {
        this.rapprochementService = rapprochementService;
    }

    /**
     * Retrieves a paginated and sorted list of transactions for all banks in the current month.
     * Supports optional multi-search via a 'libelle' parameter.
     * This is the "Récapitulatif bancaire".
     *
     * @param libelle An optional keyword to search for in the transaction's libelle.
     * @param pageable A Spring data Pageable object for pagination and sorting.
     * @return A ResponseEntity containing a Page of TransactionResponseDto and an HTTP status of OK.
     */
    @GetMapping("/recapitulatif-bancaire")
    public ResponseEntity<Page<TransactionResponseDto>> getRecapitulatifBancaire(
            @RequestParam(required = false) String libelle,
            Pageable pageable) {

        TransactionSearchDto searchDto = new TransactionSearchDto();
        searchDto.setLibelle(libelle);

        Page<TransactionResponseDto> transactions = rapprochementService.getRecapitulatifBancaire(searchDto, pageable);
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

        Page<TransactionResponseDto> transactions = rapprochementService.getTransactions(banqueId, searchDto, pageable);
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }
    /**
     * Endpoint pour traiter une transaction de rapprochement de leasing.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de leasing.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/echeance-leasing")
    public ResponseEntity<LeasingTransactionResponseDto> processLeasingTransaction(@Valid @RequestBody LeasingTransactionRequestDto requestDto) throws Exception {
        LeasingTransactionResponseDto responseDto = rapprochementService.processLeasingTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }


    /**
     * Endpoint pour traiter une transaction de rapprochement de leasing.
     * Prend en charge un DTO de requête et retourne un DTO de réponse avec les détails de la transaction créée.
     *
     * @param requestDto Le DTO de la requête de transaction de leasing.
     * @return Une ResponseEntity contenant le DTO de réponse de la transaction et un statut HTTP 201 Created.
     */
    @PostMapping("/process/echeance-cmt")
    public ResponseEntity<CmtTransactionResponseDto> processCmtTransaction(@Valid @RequestBody CmtTransactionRequestDto requestDto) throws Exception {
        CmtTransactionResponseDto responseDto = rapprochementService.processCmtTransaction(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }
}