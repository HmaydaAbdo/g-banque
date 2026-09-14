package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.services.interfaces;

import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des virements internationaux.
 *
 * <p>Centralise toute la logique virement international liée aux transactions : création de la
 * transaction bancaire (décaissement ou encaissement) et synchronisation du virement international
 * source lorsque la transaction liée est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IVirementInternationalValidationService {

    /**
     * Valide un virement international de décaissement : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque le virement international comme REGLE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    VirementInternationalTransactionResponseDto processVirementInternationalDecaissementTransaction(VirementInternationalTransactionRequestDto requestDto);

    /**
     * Valide un virement international d'encaissement : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque le virement international comme REGLE.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    VirementInternationalTransactionResponseDto processVirementInternationalEncaissementTransaction(VirementInternationalTransactionRequestDto requestDto);

    /**
     * Synchronise un virement international lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == VIREMENT_INTERNATIONAL}.
     *
     * @param virementInternationalId ID du virement international (= {@code Transaction.referent})
     * @param dateValeur               nouvelle date de valeur de la transaction
     * @param dateOperation            nouvelle date d'opération de la transaction
     */
    void syncVirementInternationalFromTransaction(Long virementInternationalId, LocalDate dateValeur, LocalDate dateOperation);
}
