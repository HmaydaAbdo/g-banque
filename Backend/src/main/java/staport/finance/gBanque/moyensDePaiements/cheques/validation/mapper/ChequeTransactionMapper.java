package staport.finance.gBanque.moyensDePaiements.cheques.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.cheques.entities.Cheque;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.cheques.validation.dto.ChequeTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

/**
 * Mapper de construction de la {@link Transaction} bancaire à partir d'un chèque validé.
 *
 * <p>Déplacé depuis {@code validation.mapper} vers le module {@code cheques} —
 * conforme à {@code architecture.md} Règle 1.
 *
 * <p>Le montant de TVA n'est plus calculé ici : il est résolu en amont par
 * {@code TvaResolver} dans {@code ChequeValidationService}, qui porte les règles (taux choisi ou
 * montant saisi, bornes). Le mapper se contente de le poser sur {@code tvaDeductible} ou
 * {@code tvaCollectee} selon le sens du chèque — conversion uniquement, conforme à
 * {@code architecture.md}.
 */
@Component
public class ChequeTransactionMapper {

    public ChequeTransactionMapper() {
    }

    /**
     * @param montantTva montant de TVA déjà résolu par {@code TvaResolver} (jamais recalculé ici)
     */
    public Transaction toEntity(ChequeTransactionRequest requestDto, Cheque cheque, BigDecimal montantTva) {

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
