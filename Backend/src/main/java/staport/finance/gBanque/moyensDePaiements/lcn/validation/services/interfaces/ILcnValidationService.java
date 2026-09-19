package staport.finance.gBanque.moyensDePaiements.lcn.validation.services.interfaces;

import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;

import java.time.LocalDate;

/**
 * Contrat du service de validation (rapprochement) des LCN.
 *
 * <p>Centralise toute la logique LCN liée aux transactions : création de la
 * transaction bancaire (décaissement ou encaissement) et synchronisation du LCN
 * source lorsque la transaction liée est modifiée a posteriori.
 *
 * <p>Conforme à {@code architecture.md} Règle 1 — extrait du {@code ValidationService}
 * générique, qui ne doit contenir aucune logique métier propre à un module.
 */
public interface ILcnValidationService {

    /**
     * Valide un LCN, décaissement ou encaissement (nature ENCAISSEMENT ou ESCOMPTE)
     * indifféremment : crée la transaction bancaire associée, attache au rapprochement cible et
     * marque le LCN comme REGLE. Le sens et la nature ({@code Lcn.type}/{@code Lcn.natureLcn})
     * sont lus directement sur l'entité — jamais transmis par l'appelant, une seule méthode
     * suffit.
     *
     * @param requestDto DTO de la requête de validation
     * @return DTO de la transaction créée
     */
    LcnTransactionResponseDto processLcnTransaction(LcnTransactionRequest requestDto);

    /**
     * Synchronise un LCN lié à une transaction mise à jour.
     * Appelé par {@code TransactionService} lorsque {@code transactionSource} vaut
     * {@code LCN_DECAISSEMENT}, {@code LCN_ENCAISSEMENT} ou {@code LCN_ESCOMPTE}.
     *
     * @param lcnId         ID du LCN (= {@code Transaction.referent})
     * @param dateValeur    nouvelle date de valeur de la transaction
     * @param dateOperation nouvelle date d'opération de la transaction
     */
    void syncLcnFromTransaction(Long lcnId, LocalDate dateValeur, LocalDate dateOperation);
}
