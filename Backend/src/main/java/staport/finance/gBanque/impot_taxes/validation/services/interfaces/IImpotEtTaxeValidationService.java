package staport.finance.gBanque.impot_taxes.validation.services.interfaces;

import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionRequestDto;
import staport.finance.gBanque.impot_taxes.validation.dto.ImpotEtTaxeTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des impôts/taxes.
 *
 * <p>Centralise toute la logique impôt/taxe liée aux transactions : création de la transaction
 * bancaire et synchronisation de l'impôt/taxe source lorsque la transaction liée est modifiée a
 * posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IImpotEtTaxeValidationService {

    /**
     * Valide un impôt/taxe : crée la transaction bancaire associée, l'attache au rapprochement
     * cible et marque l'impôt/taxe comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    ImpotEtTaxeTransactionResponseDto processImpotEtTaxeTransaction(ImpotEtTaxeTransactionRequestDto requestDto);

    /**
     * Synchronise un impôt/taxe lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == IMPOT_TAXE}.
     *
     * @param impotEtTaxeId ID de l'impôt/taxe (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncImpotEtTaxeFromTransaction(Long impotEtTaxeId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
