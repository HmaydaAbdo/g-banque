package staport.finance.gBanque.moyensDePaiements.virements.validation.services.interfaces;

import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des virements.
 *
 * <p>Centralise toute la logique virement liée aux transactions : création de la
 * transaction bancaire (décaissement ou encaissement) et synchronisation du virement
 * source lorsque la transaction liée est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IVirementValidationService {

    /**
     * Valide un virement, décaissement ou encaissement indifféremment : crée la transaction
     * bancaire associée, attache au rapprochement cible et marque le virement comme REGLE. Le sens
     * ({@code Virement.type}) est lu directement sur l'entité — jamais transmis par l'appelant,
     * une seule méthode suffit pour les deux sens.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    VirementTransactionResponseDto processVirementTransaction(VirementTransactionRequestDto requestDto);

    /**
     * Synchronise un virement lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == Virement}.
     *
     * @param virementId    ID du virement (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncVirementFromTransaction(Long virementId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
