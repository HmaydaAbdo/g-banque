package staport.finance.gBanque.restitution.validation.services.interfaces;

import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionRequestDto;
import staport.finance.gBanque.restitution.validation.dto.RestitutionTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat de validation (rapprochement) du module Restitution (architecture.md, Règle 1).
 */
public interface IRestitutionValidationService {

    RestitutionTransactionResponseDto processRestitutionTransaction(RestitutionTransactionRequestDto requestDto);

    void syncRestitutionFromTransaction(Long restitutionId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
