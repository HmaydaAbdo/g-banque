package staport.finance.gBanque.decomptes.validation.services.interfaces;

import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionRequestDto;
import staport.finance.gBanque.decomptes.validation.dto.DecompteTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des décomptes.
 *
 * <p>Centralise toute la logique décompte liée aux transactions : création de la transaction
 * bancaire et synchronisation du décompte source lorsque la transaction liée est modifiée a
 * posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IDecompteValidationService {

    /**
     * Valide un décompte : crée la transaction bancaire associée, l'attache au rapprochement
     * cible et marque le décompte comme PAYE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    DecompteTransactionResponseDto processDecompteTransaction(DecompteTransactionRequestDto requestDto);

    /**
     * Synchronise un décompte lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == DECOMPTES}.
     *
     * @param decompteId    ID du décompte (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncDecompteFromTransaction(Long decompteId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
