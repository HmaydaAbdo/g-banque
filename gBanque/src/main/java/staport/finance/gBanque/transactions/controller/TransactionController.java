    package staport.finance.gBanque.transactions.controller;

    import jakarta.validation.Valid;
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

        private final TransactionService transactionService;

        public TransactionController(TransactionService transactionService) {
            this.transactionService = transactionService;
        }



        @GetMapping("/history")
        public ResponseEntity<Page<TransactionResponseDto>> getTransactionHistory(
                TransactionSearchDto searchDto,
                Pageable pageable) {

            Page<TransactionResponseDto> transactions = transactionService.getTransactionHistory(searchDto, pageable);
            return new ResponseEntity<>(transactions, HttpStatus.OK);
        }

        @GetMapping("/{id}")
        public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
            TransactionResponseDto transaction = transactionService.getTransactionById(id);
            return ResponseEntity.ok(transaction);
        }

        @GetMapping("/{banqueId}/transactions")
        public ResponseEntity<AgreratedTransactionResponseDto> getBankTransactions(
                @PathVariable Long banqueId,
                @ModelAttribute TransactionSearchDto searchDto,
                Pageable pageable) {

            AgreratedTransactionResponseDto res =
                    transactionService.getTransactionsByBanque(banqueId, searchDto, pageable);

            return ResponseEntity.ok(res);
        }

        @PutMapping("/{id}")
        public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable Long id, @Valid @RequestBody TransactionUpdateRequestDto dto) {
            TransactionResponseDto updated = transactionService.updateTransaction(id, dto);
            return ResponseEntity.ok(updated);
        }

        @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteTransaction(@PathVariable  long id){
            transactionService.deleteTransactionById(id);
            return ResponseEntity.noContent().build();
        }
    }
