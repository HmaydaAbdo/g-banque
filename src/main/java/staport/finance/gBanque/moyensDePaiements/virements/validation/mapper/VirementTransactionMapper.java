package staport.finance.gBanque.moyensDePaiements.virements.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virements.entities.Virement;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virements.validation.dto.VirementTransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper de construction de la {@link Transaction} bancaire à partir d'un virement validé.
 *
 * <p>Déplacé depuis {@code validation.mapper} vers le module {@code virements} —
 * conforme à {@code architecture.md} Règle 1.
 */
@Component
public class VirementTransactionMapper {

    public VirementTransactionMapper() {
    }

    public Transaction toEntity(VirementTransactionRequestDto requestDto, Virement virement) {

        if (requestDto == null || virement == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur le virement d'origine si non fournis (lot).
        transaction.setReferent(virement.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : virement.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : virement.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : virement.getLibelle());
        transaction.setTransactionSource(TransactionSource.Virement);

        BigDecimal ttc = virement.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if (virement.getType() == Type.DECAISSEMENT) {
            transaction.setDebit(virement.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
            transaction.setTvaDeductible(montantTva);
            transaction.setBanqueDePrelevement(virement.getBanqueEmettrice());
        } else {
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(virement.getMontant());
            transaction.setTvaCollectee(montantTva);
            transaction.setBanqueDePrelevement(virement.getBanqueBeneficiare());
        }

        return transaction;
    }

    public VirementTransactionResponseDto toResponseDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        VirementTransactionResponseDto responseDto = new VirementTransactionResponseDto();

        responseDto.setId(transaction.getId());
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
