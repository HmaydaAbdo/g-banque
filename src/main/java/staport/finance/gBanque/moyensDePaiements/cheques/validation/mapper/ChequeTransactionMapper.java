package staport.finance.gBanque.moyensDePaiements.cheques.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

/**
 * Mapper de construction de la {@link Transaction} bancaire à partir d'un chèque validé.
 *
 * <p>Déplacé depuis {@code validation.mapper} vers le module {@code cheques} —
 * conforme à {@code architecture.md} Règle 1.
 */
@Component
public class ChequeTransactionMapper {

    public ChequeTransactionMapper() {
    }

    public Transaction toEntity(ChequeTransactionRequest requestDto, Cheque cheque) {

        if (requestDto == null || cheque == null) {
            return null;
        }

        Transaction transaction = new Transaction();
        transaction.setReferent(cheque.getId());
        // observation/dateEmission/libelle : fallback sur le chèque d'origine si non fournis (lot).
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : cheque.getObservation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : cheque.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : cheque.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.CHEQUE);
        transaction.setBanqueDePrelevement(cheque.getBanque());

        // Calculer la TVA avec utilitaire
        BigDecimal ttc = cheque.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if (cheque.getType() == Type.DECAISSEMENT) {
            transaction.setDebit(cheque.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(cheque.getMontant());
            transaction.setTvaCollectee(montantTva);
        }

        return transaction;
    }

    /**
     * Maps a Transaction entity to a ChequeTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The ChequeTransactionResponseDto.
     */
    public ChequeTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        ChequeTransactionResponseDto responseDto = new ChequeTransactionResponseDto();

        responseDto.setId(transaction.getId());
        responseDto.setChequeId(transaction.getReferent());
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
