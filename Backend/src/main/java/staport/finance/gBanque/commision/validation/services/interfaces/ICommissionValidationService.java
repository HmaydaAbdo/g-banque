package staport.finance.gBanque.commision.validation.services.interfaces;

import staport.finance.gBanque.commision.validation.dto.CommissionTransactionRequestDto;
import staport.finance.gBanque.commision.validation.dto.CommissionTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des commissions.
 *
 * <p>Centralise toute la logique commission liée aux transactions : création de la transaction
 * bancaire et synchronisation de la commission source lorsque la transaction liée est modifiée a
 * posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface ICommissionValidationService {

    /**
     * Valide une commission : crée la transaction bancaire associée, l'attache au rapprochement
     * cible et marque la commission comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    CommissionTransactionResponseDto processCommissionTransaction(CommissionTransactionRequestDto requestDto);

    /**
     * Synchronise une commission liée à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == COMMISSION}.
     *
     * @param commissionId  ID de la commission (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncCommissionFromTransaction(Long commissionId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
