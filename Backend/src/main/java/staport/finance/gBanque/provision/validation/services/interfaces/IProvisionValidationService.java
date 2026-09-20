package staport.finance.gBanque.provision.validation.services.interfaces;

import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionRequestDto;
import staport.finance.gBanque.provision.validation.dto.ProvisionTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat de validation (rapprochement) du module Provision (architecture.md, Règle 1).
 */
public interface IProvisionValidationService {

    ProvisionTransactionResponseDto processProvisionTransaction(ProvisionTransactionRequestDto requestDto);

    /**
     * Propage {@code dateValeur}/{@code dateOperation} vers la provision source après modification
     * de sa transaction (architecture.md, Règle 2).
     */
    void syncProvisionFromTransaction(Long provisionId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
