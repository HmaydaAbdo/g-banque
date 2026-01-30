package staport.finance.gBanque.validation.mapper;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.validation.dtos.virementInternationaux.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.virementInternationaux.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;

import java.math.BigDecimal;

@Component
public class VirementInternationalTransactionMapper {

    private final ObjectMapper objectMapper;
    private final BanqueRepository banqueRepository;

    public VirementInternationalTransactionMapper(ObjectMapper objectMapper, BanqueRepository banqueRepository) {
        this.objectMapper = objectMapper;
        this.banqueRepository = banqueRepository;
    }

    /**
     * Maps VirementInternationalTransactionRequestDto and its associated entity to a Transaction entity.
     * The service layer is responsible for fetching the entity.
     *
     * @param requestDto The DTO containing the request data.
     * @param virementInternational The fetched virement international entity.
     * @return The Transaction entity, ready for persistence.
     * @throws Exception if an error occurs during JSON serialization.
     */
    public Transaction toEntity(VirementInternationalTransactionRequestDto requestDto,
                                VirementInternational virementInternational) throws Exception {

        if (requestDto == null || virementInternational == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // Mapping simple fields
        transaction.setReferent(virementInternational.getId());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setTransactionSource(TransactionSource.VIREMENT_INTERNATIONAL);
        BigDecimal ttc = virementInternational.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if (virementInternational.getType() == Type.DECAISSEMENT) {
            transaction.setDebit(virementInternational.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
            transaction.setBanqueDePrelevement(virementInternational.getBanque());
        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(virementInternational.getMontant());
            transaction.setTvaCollectee(montantTva);
            transaction.setBanqueDePrelevement(virementInternational.getBanque());
        }

        return transaction;
    }

    /**
     * Maps a Transaction entity to a VirementInternationalTransactionResponseDto.
     *
     * @param transaction The Transaction entity.
     * @return The VirementInternationalTransactionResponseDto.
     * @throws Exception if an error occurs during JSON deserialization.
     */
    public VirementInternationalTransactionResponseDto toResponseDto(Transaction transaction) throws Exception {
        if (transaction == null) {
            return null;
        }

        VirementInternationalTransactionResponseDto responseDto = new VirementInternationalTransactionResponseDto();

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

        return responseDto;
    }
}