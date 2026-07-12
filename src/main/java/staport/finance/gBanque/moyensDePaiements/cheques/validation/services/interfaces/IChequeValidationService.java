package staport.finance.gBanque.moyensDePaiements.cheques.validation.services.interfaces;

import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des chèques.
 *
 * <p>Centralise toute la logique chèque liée aux transactions : création de la
 * transaction bancaire (décaissement ou encaissement) et synchronisation du chèque
 * source lorsque la transaction liée est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IChequeValidationService {

    /**
     * Valide un chèque de décaissement : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque le chèque comme REGLE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    ChequeTransactionResponseDto processChequeDecaissementTransaction(ChequeTransactionRequest requestDto);

    /**
     * Valide un chèque d'encaissement : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque le chèque comme REGLE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    ChequeTransactionResponseDto processChequeEncaissementTransaction(ChequeTransactionRequest requestDto);

    /**
     * Synchronise un chèque lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == CHEQUE}.
     *
     * @param chequeId      ID du chèque (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncChequeFromTransaction(Long chequeId, LocalDate dateValeur, LocalDate dateOperation);
}
