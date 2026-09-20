package staport.finance.gBanque.droitConstate.validation.services.interfaces;

import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionRequest;
import staport.finance.gBanque.droitConstate.validation.dto.DroitConstateTransactionResponse;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des droits constatés.
 *
 * <p>Centralise toute la logique droit constaté liée aux transactions : création de la
 * transaction bancaire et synchronisation du droit constaté source lorsque la transaction liée
 * est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IDroitConstateValidationService {

    /**
     * Valide un droit constaté : crée la transaction bancaire associée, l'attache au
     * rapprochement cible et marque le droit constaté comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    DroitConstateTransactionResponse processDroitConstateTransaction(DroitConstateTransactionRequest requestDto);

    /**
     * Synchronise un droit constaté lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == DROIT_CONSTATE}.
     *
     * @param droitConstateId ID du droit constaté (= {@code Transaction.referent})
     * @param dateValeur      nouvelle date de valeur de la transaction
     * @param dateOperation   nouvelle date d'opération de la transaction
     */
    void syncDroitConstateFromTransaction(Long droitConstateId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
