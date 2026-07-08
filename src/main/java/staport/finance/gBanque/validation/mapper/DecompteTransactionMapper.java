package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionRequestDto;
import staport.finance.gBanque.validation.dtos.decomptes.DecompteTransactionResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.decomptes.repositories.DecompteRepository;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;

@Component
public class DecompteTransactionMapper {

    private final DecompteRepository decompteRepository;
    private final BanqueRepository banqueRepository;


    public DecompteTransactionMapper(  DecompteRepository decompteRepository, BanqueRepository banqueRepository) {
        this.decompteRepository = decompteRepository;
        this.banqueRepository = banqueRepository;
    }



    /**
     * Maps DecompteTransactionRequestDto and its associated entities to a Transaction entity.
     *
     * @param requestDto The DTO containing the request data.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(DecompteTransactionRequestDto requestDto  )   {

        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        Decompte decompte=this.decompteRepository.findById(requestDto.getDecompteId())
                .orElseThrow(()->new ResourceNotFoundException("décompte  with id "+requestDto.getDecompteId()+" not found"));

        if (decompte.getBanqueEncaissement() == null
                || !decompte.getBanqueEncaissement().getId().equals(requestDto.getBanqueId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque d'encaissement du décompte "
                    + requestDto.getDecompteId() + ".");
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));

        transaction.setReferent(decompte.getId());
        transaction.setLibelle(requestDto.getLibelle());
        transaction.setObservation(requestDto.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setTransactionSource(TransactionSource.DECOMPTES);
        transaction.setBanqueDePrelevement(banque);
        transaction.setDebit(BigDecimal.ZERO);
        transaction.setCredit(decompte.getMontant());


        BigDecimal ttc = decompte.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
        transaction.setTvaCollectee(montantTva);



        return transaction;
    }

    public DecompteTransactionResponseDto toResponseDto(Transaction transaction)   {

        if (transaction == null) {
            return null;
        }

        DecompteTransactionResponseDto responseDto = new DecompteTransactionResponseDto();

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
