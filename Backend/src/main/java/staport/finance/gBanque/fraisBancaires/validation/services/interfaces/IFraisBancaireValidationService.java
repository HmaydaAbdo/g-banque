package staport.finance.gBanque.fraisBancaires.validation.services.interfaces;

import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionRequestDto;
import staport.finance.gBanque.fraisBancaires.validation.dto.FraisBancaireTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat de validation (rapprochement) du module Frais bancaires (architecture.md, Règle 1).
 */
public interface IFraisBancaireValidationService {

    FraisBancaireTransactionResponseDto processFraisBancaireTransaction(FraisBancaireTransactionRequestDto requestDto);

    /**
     * Propage {@code dateValeur}/{@code dateOperation} vers le frais bancaire source après
     * modification de sa transaction (architecture.md, Règle 2).
     */
    void syncFraisBancaireFromTransaction(Long fraisBancaireId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
