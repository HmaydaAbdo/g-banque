package staport.finance.gBanque.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.Bank.repositories.BanqueRepository;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;
import staport.finance.gBanque.exception.customExceptions.ValidationException;
import staport.finance.gBanque.validation.dtos.droitConstate.DroitConstateTransactionRequest;
import staport.finance.gBanque.validation.dtos.droitConstate.DroitConstateTransactionResponse;
import staport.finance.gBanque.droitConstate.entities.DroitConstate;
import staport.finance.gBanque.droitConstate.repositories.DroitConstateRepository;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Type;
import staport.finance.gBanque.validation.utils.TVAUtils;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import java.math.BigDecimal;

@Component
public class DroitConstateTransactionMapper {

    private final DroitConstateRepository droitConstateRepository;
    private final BanqueRepository banqueRepository;

    public DroitConstateTransactionMapper(  DroitConstateRepository droitConstateRepository, BanqueRepository banqueRepository) {
        this.droitConstateRepository = droitConstateRepository;
        this.banqueRepository = banqueRepository;
    }


    /**
     * Maps DroitConstateTransactionRequest and its associated entities to a Transaction entity.
     *
     * @param requestDto The DTO containing the request data.
     * @return The Transaction entity, ready for persistence.
     */
    public Transaction toEntity(DroitConstateTransactionRequest requestDto  )   {


        if (requestDto == null  ) {
            return null;
        }

        Transaction transaction = new Transaction();

        DroitConstate droitConstate=this.droitConstateRepository.findById(requestDto.getDroitConstateId())
                .orElseThrow(()->new ResourceNotFoundException("Droits constaté with id "+requestDto.getDroitConstateId()+" not found"));

        Marche marche=droitConstate.getMarche();

        if(marche==null){
            throw new ResourceNotFoundException("Marche null for droit constaté with id "+requestDto.getDroitConstateId());
        }

        if (droitConstate.getBanque() == null
                || !droitConstate.getBanque().getId().equals(requestDto.getBanqueId())) {
            throw new ValidationException("Le banqueId fourni (" + requestDto.getBanqueId()
                    + ") ne correspond pas à la banque du droit constaté "
                    + requestDto.getDroitConstateId() + ".");
        }

        Banque banque=this.banqueRepository.findById(requestDto.getBanqueId()).orElseThrow(()->new ResourceNotFoundException("Banque with id "+requestDto.getBanqueId()+" not found"));
        // libelle/observation/dateEmission : fallback sur le droit constaté d'origine si non fournis (lot).
        transaction.setReferent(droitConstate.getId());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : droitConstate.getLibelle());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : droitConstate.getObservation());
        transaction.setTransactionSource(TransactionSource.DROIT_CONSTATE);
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : droitConstate.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setBanqueDePrelevement(banque);

        if(droitConstate.getType()== Type.REMBOURSEMENT){

            transaction.setDebit(droitConstate.getMontant());
            transaction.setCredit(BigDecimal.ZERO);

            BigDecimal ttc = droitConstate.getMontant();
            BigDecimal tauxTVA = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTVA);
            transaction.setTvaDeductible(montantTva);

        }else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(droitConstate.getMontant());

            BigDecimal ttc = droitConstate.getMontant();
            BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTva());
            BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);
            transaction.setTvaCollectee(montantTva);
        }


        return transaction;
    }

    public DroitConstateTransactionResponse toResponseDto(Transaction transaction)   {

        if (transaction == null) {
            return null;
        }

        DroitConstateTransactionResponse responseDto = new DroitConstateTransactionResponse();

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
