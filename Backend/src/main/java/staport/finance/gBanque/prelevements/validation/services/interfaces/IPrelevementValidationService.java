package staport.finance.gBanque.prelevements.validation.services.interfaces;

import staport.finance.gBanque.prelevements.validation.dto.PrelevementTransactionResponse;
import staport.finance.gBanque.prelevements.validation.dto.PrelevementValidationRequest;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des prélèvements.
 *
 * <p>Centralise toute la logique prélèvement liée aux transactions : création de la
 * transaction bancaire et synchronisation du prélèvement source lorsque la transaction liée
 * est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IPrelevementValidationService {

    /**
     * Valide un prélèvement : crée la transaction bancaire associée, l'attache au
     * rapprochement cible et marque le prélèvement comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    PrelevementTransactionResponse processPrelevementToTransaction(PrelevementValidationRequest requestDto);

    /**
     * Synchronise un prélèvement lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == PRELEVEMENT}.
     *
     * @param prelevementId ID du prélèvement (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncPrelevementFromTransaction(Long prelevementId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
