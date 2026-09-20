package staport.finance.gBanque.validation.controler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import staport.finance.gBanque.validation.dtos.lot.BatchValidationRequestDto;
import staport.finance.gBanque.validation.dtos.lot.BatchValidationResponseDto;
import staport.finance.gBanque.validation.services.BatchValidationService;
import staport.finance.gBanque.validation.services.ReverseTransactionService;

/**
 * Point d'entrée unique pour la validation (rapprochement) et son annulation, toutes sources
 * confondues — voir {@code conventions/architecture.md}, Règle 1 (révisée le 19/09/2026) : valider
 * une opération n'est qu'un cas particulier de valider un lot de taille 1, il n'existe donc plus
 * d'endpoint dédié par source. La logique métier de chaque source reste dans son propre module
 * ({@code XxxValidationService}) ; {@link BatchValidationService} ne fait que dispatcher.
 */
@Tag(name = "Validation", description = "Validation (rapprochement) des opérations de tous les modules source")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/rapprochement")
@PreAuthorize(ValidationController.FINANCE_OR_ADMIN)
public class ValidationController {

    static final String FINANCE_OR_ADMIN =
            "hasAnyAuthority('DEPT_Finance:MEMBER', 'DEPT_Finance:DIRECTOR', 'SYSTEM_ADMIN')";

    private final BatchValidationService batchValidationService;
    private final ReverseTransactionService reverseTransactionService;

    public ValidationController(BatchValidationService batchValidationService,
                                 ReverseTransactionService reverseTransactionService) {
        this.batchValidationService = batchValidationService;
        this.reverseTransactionService = reverseTransactionService;
    }

    /**
     * Valide une ou plusieurs opérations de même source (et même sens pour les sources qui le
     * distinguent) : {@code ids} à un élément pour une opération unique, à plusieurs pour un lot.
     * Délègue à {@link BatchValidationService}, qui dispatche vers le
     * {@code BatchValidationHandler} de la source concernée — voir sa Javadoc pour le détail du
     * dispatch et le comportement tout-ou-rien.
     */
    @Operation(summary = "Valider une ou plusieurs opérations de même source")
    @PostMapping("/process")
    public ResponseEntity<BatchValidationResponseDto> processBatch(
            @Valid @RequestBody BatchValidationRequestDto requestDto) {
        BatchValidationResponseDto responseDto = batchValidationService.processBatch(requestDto);
        return new ResponseEntity<>(responseDto, HttpStatus.CREATED);
    }

    @Operation(summary = "Annuler une transaction (rapprochement non clôturé définitivement)")
    @DeleteMapping("/annuler/{transactionId}")
    public void annulerTransaction(@PathVariable Long transactionId) {
        reverseTransactionService.annulerTransaction(transactionId);
    }

}
