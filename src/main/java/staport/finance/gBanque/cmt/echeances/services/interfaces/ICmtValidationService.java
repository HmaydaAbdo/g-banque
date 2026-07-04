package staport.finance.gBanque.cmt.echeances.services.interfaces;

import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionRequestDto;
import staport.finance.gBanque.cmt.echeances.dtos.CmtTransactionResponseDto;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des échéances CMT.
 * <p>
 * Centralise toute la logique CMT liée aux transactions : création de la
 * transaction bancaire et synchronisation de l'échéance.
 */
public interface ICmtValidationService {

    /**
     * Valide une échéance CMT : crée la transaction bancaire associée,
     * attache au rapprochement cible et marque l'échéance comme PAYE.
     * <p>
     * Prend le {@code CreditMoyenTerme} directement (résolu par l'appelant, {@code ValidationController})
     * plutôt que son ID, pour éviter que ce service n'ait besoin d'injecter {@code CreditMoyenTermeService} —
     * une telle dépendance créerait un cycle de beans Spring (même précaution que pour le Leasing).
     *
     * @param creditMoyenTerme crédit déjà résolu par l'appelant
     * @param dto              DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    CmtTransactionResponseDto processCmtTransaction(CreditMoyenTerme creditMoyenTerme, CmtTransactionRequestDto dto);

    /**
     * Synchronise une échéance liée à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource == CMT}.
     *
     * @param echeanceId    ID de l'échéance (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncEcheanceFromTransaction(Long echeanceId, LocalDate dateValeur, LocalDate dateOperation);
}
