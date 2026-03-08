package staport.finance.gBanque.transactions.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;
import staport.finance.gBanque.transactions.service.TransactionService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);
    private final TransactionService transactionService;


    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionHistory(
            TransactionSearchDto searchDto,
            Pageable pageable) {

        log.info("Requête API : Récupération de l'historique des transactions [Critères: {}, Page: {}]",
                searchDto, pageable.getPageNumber());

        Page<TransactionResponseDto> transactions = transactionService.getTransactionHistory(searchDto, pageable);

        log.info("Réponse API : {} transactions envoyées", transactions.getTotalElements());
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        log.info("Requête API : Recherche de la transaction ID: {}", id);

        TransactionResponseDto transaction = transactionService.getTransactionById(id);

        log.info("Réponse API : Transaction ID: {} trouvée avec succès", id);
        return ResponseEntity.ok(transaction);
    }

    @GetMapping("/{banqueId}/transactions")
    public ResponseEntity<AgreratedTransactionResponseDto> getBankTransactions(
            @PathVariable Long banqueId,
            @ModelAttribute TransactionSearchDto searchDto,
            Pageable pageable) {

        log.info("Requête API : Récupération des transactions agrégées pour la banque ID: {} [Filtres: {}]",
                banqueId, searchDto);

        AgreratedTransactionResponseDto res =
                transactionService.getTransactionsByBanque(banqueId, searchDto, pageable);

        log.info("Réponse API : Agrégation calculée pour la banque {}. Solde agrégé prêt.", banqueId);
        return ResponseEntity.ok(res);
    }

    @PatchMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable Long transactionId, @RequestBody TransactionUpdateRequestDto updateRequestDto){
        return ResponseEntity.ok(transactionService.updateTransactionById(transactionId,updateRequestDto));
    }


}