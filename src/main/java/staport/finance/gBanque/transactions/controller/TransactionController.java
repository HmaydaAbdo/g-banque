package staport.finance.gBanque.transactions.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import staport.finance.gBanque.transactions.dtos.AgreratedTransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.dtos.TransactionSearchDto;
import staport.finance.gBanque.transactions.dtos.TransactionUpdateRequestDto;
import staport.finance.gBanque.transactions.service.ITransactionService;

import java.time.LocalDate;

/**
 * Contrôleur REST pour le module Transactions.
 *
 * <p>Injecte {@link ITransactionService} (DIP — jamais la classe concrète). Aucune logique
 * métier ni journalisation ici — les logs métier vivent exclusivement dans le Service
 * (voir {@code conventions/logging.md}).
 *
 * <p>Autorisations (cf. {@code docs/rbac/transactions.md}) : lecture et modification réservées à
 * Finance (membre ou directeur) ou {@code SYSTEM_ADMIN}.
 */
@Tag(name = "Transactions", description = "Historique et suivi des mouvements bancaires")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/transactions")
@PreAuthorize(TransactionController.FINANCE_OR_ADMIN)
public class TransactionController {

    static final String FINANCE_OR_ADMIN =
            "hasAuthority('SYSTEM_ADMIN') or hasAuthority('DEPT_Finance:MEMBER') or hasAuthority('DEPT_Finance:DIRECTOR')";

    private final ITransactionService transactionService;

    public TransactionController(ITransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Operation(summary = "Historique global des transactions")
    @GetMapping("/history")
    public ResponseEntity<Page<TransactionResponseDto>> getTransactionHistory(
            TransactionSearchDto searchDto,
            Pageable pageable) {
        return new ResponseEntity<>(transactionService.getTransactionHistory(searchDto, pageable), HttpStatus.OK);
    }

    @Operation(summary = "Exporter en PDF l'historique des transactions",
            description = "Mêmes critères que GET /transactions/history — sans pagination")
    @GetMapping("/history/export/pdf")
    public ResponseEntity<byte[]> exportTransactionHistoryPdf(TransactionSearchDto searchDto) {
        byte[] pdf = transactionService.exportTransactionHistoryPdf(searchDto);
        String filename = "historique_transactions_" + LocalDate.now() + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @Operation(summary = "Récupérer une transaction par ID")
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDto> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @Operation(summary = "Transactions agrégées d'une banque")
    @GetMapping("/{banqueId}/transactions")
    public ResponseEntity<AgreratedTransactionResponseDto> getBankTransactions(
            @PathVariable Long banqueId,
            @ModelAttribute TransactionSearchDto searchDto,
            Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactionsByBanque(banqueId, searchDto, pageable));
    }

    @Operation(summary = "Modifier dates et TVA d'une transaction")
    @PatchMapping("/{transactionId}")
    public ResponseEntity<TransactionResponseDto> updateTransaction(@PathVariable Long transactionId, @Valid @RequestBody TransactionUpdateRequestDto updateRequestDto){
        return ResponseEntity.ok(transactionService.updateTransactionById(transactionId, updateRequestDto));
    }


}
