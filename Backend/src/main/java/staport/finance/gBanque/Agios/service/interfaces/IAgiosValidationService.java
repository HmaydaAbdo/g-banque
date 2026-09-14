package staport.finance.gBanque.Agios.service.interfaces;

import staport.finance.gBanque.Agios.dtos.AgiosTransactionResponse;
import staport.finance.gBanque.Agios.dtos.AgiosValidationRequest;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des agios.
 * <p>
 * Centralise toute la logique agios liée aux transactions :
 * création de la transaction bancaire, synchronisation et annulation.
 */
public interface IAgiosValidationService {

    /**
     * Valide un agios : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque l'agios comme PAYE.
     *
     * @param dto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    AgiosTransactionResponse processAgiosToTransaction(AgiosValidationRequest dto);

    /**
     * Synchronise un agios lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == AGIOS}.
     *
     * @param agiosId       ID de l'agios (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur
     * @param dateOperation nouvelle date d'opération
     */
    void syncAgiosFromTransaction(Long agiosId, LocalDate dateValeur, LocalDate dateOperation);

    /**
     * Remet un agios à l'état PREVU lors de l'annulation de sa transaction liée.
     * Appelé par {@code ReverseTransactionService} lorsque {@code transactionSource == AGIOS}.
     *
     * @param agiosId ID de l'agios à réinitialiser
     */
    void resetAgios(Long agiosId);
}
