package staport.finance.gBanque.factoring.validation.services.interfaces;

import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionRequest;
import staport.finance.gBanque.factoring.validation.dto.FactoringTransactionResponse;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des factorings.
 *
 * <p>Centralise toute la logique factoring liée aux transactions : création de la transaction
 * bancaire et synchronisation du factoring source lorsque la transaction liée est modifiée a
 * posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IFactoringValidationService {

    /**
     * Valide un factoring : crée la transaction bancaire associée, l'attache au rapprochement
     * cible et marque le factoring comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    FactoringTransactionResponse processFactoringTransaction(FactoringTransactionRequest requestDto);

    /**
     * Synchronise un factoring lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == FACTORING}.
     *
     * @param factoringId   ID du factoring (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncFactoringFromTransaction(Long factoringId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
