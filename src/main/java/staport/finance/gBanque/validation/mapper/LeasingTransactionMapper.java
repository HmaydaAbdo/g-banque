package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.leasing.contratLeasing.entities.ContratLeasing;
import staport.finance.gBanque.leasing.echeancesLeasing.entities.EcheanceLeasing;
import staport.finance.gBanque.validation.dtos.leasing.LeasingTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.leasing.LeasingTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.Bank.entities.Banque;
import java.math.BigDecimal;

@Component
public class LeasingTransactionMapper {

    public LeasingTransactionMapper() {
    }

    /**
     * Maps a LeasingTransactionRequestDto and its associated entities to a Transaction entity.
     * The service layer is responsible for fetching the entities.
     *
     * @param requestDto      The DTO containing the request data.
     * @param contratLeasing  The fetched ContratLeasing entity.
     * @param echeanceLeasing The fetched EcheanceLeasing entity.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(LeasingTransactionRequestDto requestDto, ContratLeasing contratLeasing, EcheanceLeasing echeanceLeasing)   {
        if (requestDto == null || contratLeasing == null || echeanceLeasing == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(echeanceLeasing.getId());
        transaction.setObservation(requestDto.getObservation());
        // Mapping dates (ensuring consistency with OffsetDateTime)
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());

        // Mapping complex fields using the fetched entities

        transaction.setTransactionSource(TransactionSource.LEASING);
        transaction.setDebit(echeanceLeasing.getMontantTTC());
        transaction.setCredit(BigDecimal.ZERO);
        transaction.setBanqueDePrelevement(contratLeasing.getBanqueDePrelevement());

        BigDecimal ttc = echeanceLeasing.getMontantTTC();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaDeductible(montantTva);


        return transaction;
    }

    /**
     * Maps a Transaction entity to a LeasingTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The LeasingTransactionResponseDto.
     */
    public LeasingTransactionResponseDto toResponseDto(Transaction transaction)   {
        if (transaction == null) {
            return null;
        }

        LeasingTransactionResponseDto responseDto = new LeasingTransactionResponseDto();

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