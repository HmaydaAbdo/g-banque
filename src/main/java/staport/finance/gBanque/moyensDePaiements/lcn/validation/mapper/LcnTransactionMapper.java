package staport.finance.gBanque.moyensDePaiements.lcn.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.lcn.dtos.NatureLcn;
import staport.finance.gBanque.moyensDePaiements.lcn.entities.Lcn;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionRequest;
import staport.finance.gBanque.moyensDePaiements.lcn.validation.dto.LcnTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper de construction de la {@link Transaction} bancaire à partir d'un LCN validé.
 *
 * <p>Déplacé depuis {@code validation.mapper} vers le module {@code lcn} —
 * conforme à {@code architecture.md} Règle 1.
 */
@Component
public class LcnTransactionMapper {

    public LcnTransactionMapper() {
    }

    public Transaction toEntity(LcnTransactionRequest requestDto, Lcn lcn) {

        if (requestDto == null || lcn == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur le LCN d'origine si non fournis (lot).
        transaction.setReferent(lcn.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : lcn.getObservation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : lcn.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : lcn.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());

        transaction.setBanqueDePrelevement(lcn.getBanque());

        BigDecimal ttc = lcn.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if (lcn.getType() == Type.DECAISSEMENT) {
            transaction.setTvaDeductible(montantTva);
            transaction.setDebit(lcn.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTransactionSource(TransactionSource.LCN_DECAISSEMENT);
        } else {
            transaction.setTvaCollectee(montantTva);
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(lcn.getMontant());
            if (lcn.getNatureLcn() == NatureLcn.ENCAISSEMENT) {
                transaction.setTransactionSource(TransactionSource.LCN_ENCAISSEMENT);
            } else {
                transaction.setTransactionSource(TransactionSource.LCN_ESCOMPTE);
            }
        }

        return transaction;
    }

    /**
     * @param lcn le LCN source de la transaction — nécessaire pour renseigner {@code lcnNumber},
     *            que la {@link Transaction} elle-même ne porte pas.
     */
    public LcnTransactionResponseDto toResponseDto(Transaction transaction, Lcn lcn) {
        if (transaction == null) {
            return null;
        }

        LcnTransactionResponseDto responseDto = new LcnTransactionResponseDto();

        responseDto.setId(transaction.getId());
        Banque banque = transaction.getBanqueDePrelevement();
        if (banque != null) {
            responseDto.setBanqueName(banque.getNom());
        }
        if (lcn != null) {
            responseDto.setLcnNumber(lcn.getNumero());
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
        responseDto.setTransactionSource(transaction.getTransactionSource());

        return responseDto;
    }
}
