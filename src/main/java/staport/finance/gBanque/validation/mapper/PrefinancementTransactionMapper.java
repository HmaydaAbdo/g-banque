package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionRequest;
import staport.finance.gBanque.validation.dtos.prefinancements.PrefinancementTransactionResponse;
import staport.finance.gBanque.prefinancement.entities.Prefinancement;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.prefinancement.repository.PrefinancementRepository;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;

@Component
public class PrefinancementTransactionMapper {

    private final PrefinancementRepository prefinancementRepository;
    private final BanqueRepository  banqueRepository;

    public PrefinancementTransactionMapper( PrefinancementRepository prefinancementRepository, BanqueRepository banqueRepository) {
        this.prefinancementRepository = prefinancementRepository;
        this.banqueRepository = banqueRepository;
    }

    /**
     * Maps PrefinancementTransactionRequest and its associated entities to a Transaction entity.
     *
     * @param requestDto The DTO containing the request data.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(PrefinancementTransactionRequest requestDto  )   {


        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        Prefinancement prefinancement=this.prefinancementRepository.findById(requestDto.getPrefinancementId())
                .orElseThrow(()->new ResourceNotFoundException("Prefinancement with id "+requestDto.getPrefinancementId()+" not found"));

        Marche marche=prefinancement.getMarche();

        if(marche==null){
            throw new IllegalStateException("Marche null for prefinancement with id "+requestDto.getPrefinancementId());
        }

        if (marche.getBanque() == null || !marche.getBanque().getId().equals(requestDto.getBanqueId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque du marché associé au préfinancement "
                    + requestDto.getPrefinancementId() + ".");
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        transaction.setReferent(prefinancement.getId());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);
        transaction.setObservation(requestDto.getObservation());
        transaction.setTransactionSource(TransactionSource.PREFINANCEMENT);

        if(prefinancement.getType()== Type.REMBOURSEMENT){
            transaction.setDebit(prefinancement.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = prefinancement.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaDeductible(montantTva);

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(prefinancement.getMontant());

            BigDecimal ttc = prefinancement.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }



        return transaction;
    }

    public PrefinancementTransactionResponse toResponseDto(Transaction transaction)   {

        if (transaction == null) {
            return null;
        }

        PrefinancementTransactionResponse responseDto = new PrefinancementTransactionResponse();

        responseDto.setId(transaction.getId());
        responseDto.setBanqueName(transaction.getBanqueDePrelevement().getNom());
        responseDto.setLibelle(transaction.getLibelle());
        responseDto.setObservation(transaction.getObservation());
        responseDto.setDateEmission(transaction.getDateEmission());
        responseDto.setDateValeur(transaction.getDateValeur());
        responseDto.setDateOperations(transaction.getDateOperations());
        responseDto.setDebit(transaction.getDebit());
        responseDto.setCredit(transaction.getCredit());

        return responseDto;

    }
}
