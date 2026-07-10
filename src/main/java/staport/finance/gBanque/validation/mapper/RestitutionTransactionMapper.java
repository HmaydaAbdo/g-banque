package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.restitution.entities.Restitution;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionTransactionResponse;
import staport.finance.gBanque.validation.dtos.restitutions.RestitutionValidationRequest;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;


@Component
public class RestitutionTransactionMapper {

    public RestitutionTransactionMapper() {
    }

    /**
     * Convertit le DTO de validation et l'entité Restitution en Transaction
     */
    public Transaction toEntity(RestitutionValidationRequest requestDto, Restitution restitution)  {

        if (requestDto == null || restitution == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping des champs simples
        // observation/dateEmission/libelle : fallback sur la restitution d'origine si non fournis (lot).
        transaction.setReferent(restitution.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : restitution.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : restitution.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : restitution.getLibelle());
        transaction.setTransactionSource(TransactionSource.RESTITUTION);

        // Les restitutions sont des ENCAISSEMENTS (crédit)
        transaction.setCredit(restitution.getMontant());
        transaction.setDebit(BigDecimal.ZERO);

        transaction.setBanqueDePrelevement(restitution.getBanque());

        // Calcul de la TVA
        BigDecimal ttc = restitution.getMontant();
        BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());

        // Calculer uniquement le montant de la TVA
        BigDecimal tva = TVAUtils.calculerTVA(ttc, tauxTVA);

        // Stocker la TVA dans la transaction (TVA collectée pour les encaissements)
        transaction.setTvaCollectee(tva);


        return transaction;
    }

    /**
     * Convertit une Transaction en DTO de réponse
     */
    public RestitutionTransactionResponse toResponseDto(Transaction transaction)  {
        if (transaction == null) {
            return null;
        }

        RestitutionTransactionResponse responseDto = new RestitutionTransactionResponse();

        // Mapping des champs de l'entité vers le DTO
        responseDto.setId(transaction.getId());

        // Gestion du lazy loading de la banque
        Banque banque = transaction.getBanqueDePrelevement();
        if (banque != null) {
            responseDto.setBanqueName(banque.getNom());
        }

        responseDto.setLibelle(transaction.getLibelle());
        responseDto.setObservation(transaction.getObservation());
        responseDto.setDateEmission(transaction.getDateEmission());
        responseDto.setDateValeur(transaction.getDateValeur());
        responseDto.setDateOperations(transaction.getDateOperations());
        responseDto.setTvaDeductible(transaction.getTvaDeductible());
        responseDto.setTvaCollectee(transaction.getTvaCollectee());
        responseDto.setDebit(transaction.getDebit());
        responseDto.setCredit(transaction.getCredit());

        return responseDto;
    }
}