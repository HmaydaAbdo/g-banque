package staport.finance.gBanque.leasing.echeancesLeasing.services.interfaces;

import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionRequestDTO;
import staport.finance.gBanque.leasing.echeancesLeasing.dto.LeasingTransactionResponseDTO;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des échéances de leasing.
 * <p>
 * Centralise toute la logique leasing liée aux transactions :
 * création de la transaction bancaire et synchronisation de l'échéance.
 */
public interface ILeasingValidationService {

    /**
     * Valide une échéance de leasing : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque l'échéance comme PAYE.
     *
     * @param dto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    LeasingTransactionResponseDTO processLeasingTransaction(LeasingTransactionRequestDTO dto);

    /**
     * Synchronise une échéance liée à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == LEASING}.
     *
     * @param echeanceId    ID de l'échéance (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncEcheanceFromTransaction(Long echeanceId, LocalDate dateValeur, LocalDate dateOperation);
}
