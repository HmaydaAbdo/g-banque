package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.services.interfaces;

import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des mises à disposition.
 *
 * <p>Centralise toute la logique mise à disposition liée aux transactions : création de la
 * transaction bancaire (décaissement ou encaissement) et synchronisation de la mise à disposition
 * source lorsque la transaction liée est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface IMiseADispositionValidationService {

    /**
     * Valide une mise à disposition, décaissement ou encaissement indifféremment : crée la
     * transaction bancaire associée, attache au rapprochement cible et marque la mise à
     * disposition comme REGLE. Le sens ({@code MiseADisposition.type}) est lu directement sur
     * l'entité — jamais transmis par l'appelant, une seule méthode suffit pour les deux sens.
     */
    MiseAdispositionTransactionResponseDto processMiseAdispositionTransaction(MiseAdispositionTransactionRequestDto requestDto);

    /**
     * Synchronise une mise à disposition liée à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == MiseADisposition}.
     *
     * @param miseADispositionId ID de la mise à disposition (= {@code Transaction.referent})
     * @param dateValeur          nouvelle date de valeur de la transaction
     * @param dateOperation       nouvelle date d'opération de la transaction
     */
    void syncMiseADispositionFromTransaction(Long miseADispositionId, LocalDate dateValeur, LocalDate dateOperation, String libelle, String observation);
}
