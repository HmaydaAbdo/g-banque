package staport.finance.gBanque.prefinancement.validation.services.interfaces;

import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionRequest;
import staport.finance.gBanque.prefinancement.validation.dto.PrefinancementTransactionResponse;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des préfinancements.
 *
 * <p>Centralise toute la logique préfinancement liée aux transactions : création de la
 * transaction bancaire et synchronisation du préfinancement source lorsque la transaction liée
 * est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IPrefinancementValidationService {

    /**
     * Valide un préfinancement : crée la transaction bancaire associée, l'attache au
     * rapprochement cible et marque le préfinancement comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    PrefinancementTransactionResponse processPrefinancementTransaction(PrefinancementTransactionRequest requestDto);

    /**
     * Synchronise un préfinancement lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == PREFINANCEMENT}.
     *
     * @param prefinancementId ID du préfinancement (= {@code Transaction.referent})
     * @param dateValeur       nouvelle date de valeur de la transaction
     * @param dateOperation    nouvelle date d'opération de la transaction
     */
    void syncPrefinancementFromTransaction(Long prefinancementId, LocalDate dateValeur, LocalDate dateOperation);
}
