package staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.entities.MiseADisposition;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.miseADisposition.validation.dto.MiseAdispositionTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper de validation (rapprochement) du module mise à disposition.
 *
 * <p>Déplacé depuis {@code validation.mapper} vers le module {@code miseADisposition} —
 * conforme à {@code architecture.md} Règle 1.
 */
@Component
public class MiseAdispositionTransactionMapper {

    public MiseAdispositionTransactionMapper() {
    }

    /**
     * Convertit le DTO de requête et la mise à disposition associée en entité Transaction.
     * Le service est responsable de récupérer l'entité.
     */
    public Transaction toEntity(MiseAdispositionTransactionRequestDto requestDto, MiseADisposition miseADispo) {

        if (requestDto == null || miseADispo == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur la mise à disposition d'origine si non fournis (lot).
        transaction.setReferent(miseADispo.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : miseADispo.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : miseADispo.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : miseADispo.getLibelle());
        transaction.setTransactionSource(TransactionSource.MiseADisposition);
        transaction.setBanqueDePrelevement(miseADispo.getBanque());

        BigDecimal ttc = miseADispo.getMontant();
        BigDecimal tauxTva = BigDecimal.valueOf(requestDto.getTvaDeductible());
        BigDecimal montantTva = TVAUtils.calculerTVA(ttc, tauxTva);

        if (miseADispo.getType() == Type.DECAISSEMENT) {
            transaction.setTvaDeductible(montantTva);
            transaction.setTvaCollectee(BigDecimal.ZERO);
            transaction.setDebit(miseADispo.getMontant());
            transaction.setCredit(BigDecimal.ZERO);
        } else {
            transaction.setTvaDeductible(BigDecimal.ZERO);
            transaction.setTvaCollectee(montantTva);
            transaction.setDebit(BigDecimal.ZERO);
            transaction.setCredit(miseADispo.getMontant());
        }

        return transaction;
    }

    /**
     * Convertit une entité Transaction en DTO de réponse.
     *
     * @param miseADispo la mise à disposition source — nécessaire pour renseigner
     *                    {@code miseADispositionNumber}, que la {@link Transaction} elle-même ne porte pas.
     */
    public MiseAdispositionTransactionResponseDto toResponseDto(Transaction transaction, MiseADisposition miseADispo) {
        if (transaction == null) {
            return null;
        }

        MiseAdispositionTransactionResponseDto responseDto = new MiseAdispositionTransactionResponseDto();

        responseDto.setId(transaction.getId());
        responseDto.setMiseADispositionId(transaction.getReferent());
        Banque banque = transaction.getBanqueDePrelevement();
        if (banque != null) {
            responseDto.setBanqueName(banque.getNom());
        }
        if (miseADispo != null) {
            responseDto.setMiseADispositionNumber(miseADispo.getNumero());
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
