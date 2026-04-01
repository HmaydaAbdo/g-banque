package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.cmt.echeances.entities.EcheanceCMT;
import staport.finance.gBanque.cmt.entities.CreditMoyenTerme;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.cmt.CmtTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;

@Component
public class CmtTransactionMapper {

    public CmtTransactionMapper() {
    }

    /**
     * Maps a CmtTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto      The DTO containing the request data.
     * @param creditMoyenTerme  The fetched cmt entity.
     * @param echeanceCMT The fetched EcheanceCMT entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(CmtTransactionRequestDto requestDto, CreditMoyenTerme creditMoyenTerme, EcheanceCMT echeanceCMT)   {


        if (requestDto == null || creditMoyenTerme == null || echeanceCMT == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(echeanceCMT.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setLibelle(requestDto.getLibelle());
        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.CMT);
        transaction.setDebit(echeanceCMT.getMontant());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(creditMoyenTerme.getBanque());

        BigDecimal ttc = echeanceCMT.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaDeductible(montantTva);


        return transaction;
    }

    /**
     * Maps a Transaction entity to a CmtTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The CmtTransactionResponseDto.
     */
    public CmtTransactionResponseDto toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        CmtTransactionResponseDto responseDto = new CmtTransactionResponseDto();

        // Mapping fields from the entity to the DTO
        responseDto.setId(transaction.getId());
        // Lazy loading might require calling getBanqueDePrelevement() before getName()
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
        responseDto.setTransactionSource(transaction.getTransactionSource());


        return responseDto;
    }
}