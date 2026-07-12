package staport.finance.gBanque.moyensDePaiements.virementInternational.validation.mapper;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.moyensDePaiements.virementInternational.entities.VirementInternational;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionRequestDto;
import staport.finance.gBanque.moyensDePaiements.virementInternational.validation.dto.VirementInternationalTransactionResponseDto;
import staport.finance.gBanque.moyensDePaiements.virements.enums.Type;
import staport.finance.gBanque.transactions.entities.Transaction;
import staport.finance.gBanque.transactions.enums.TransactionSource;
import staport.finance.gBanque.validation.utils.TVAUtils;

import java.math.BigDecimal;

/**
 * Mapper de validation (rapprochement) du module virement international.
 *
 * <p>Déplacé depuis {@code validation.mapper} vers le module {@code virementInternational} —
 * conforme à {@code architecture.md} Règle 1.
 */
@Component
public class VirementInternationalTransactionMapper {

    public VirementInternationalTransactionMapper() {
    }

    /**
     * Convertit le DTO de requête et le virement international associé en entité Transaction.
     * Le service est responsable de récupérer l'entité.
     *
     * @param requestDto             Le DTO contenant les données de la requête.
     * @param virementInternational  Le virement international déjà récupéré.
     * @return L'entité Transaction, prête à être persistée.
     */
    public Transaction toEntity(VirementInternationalTransactionRequestDto requestDto,
                                VirementInternational virementInternational)  {

        if (requestDto == null || virementInternational == null) {
            return null;
        }

        Transaction transaction = new Transaction();

        // observation/dateEmission/libelle : fallback sur le virement international d'origine si non fournis (lot).
        transaction.setReferent(virementInternational.getId());
        transaction.setObservation(requestDto.getObservation() != null ? requestDto.getObservation() : virementInternational.getObservation());
        transaction.setDateEmission(requestDto.getDateEmission() != null ? requestDto.getDateEmission() : virementInternational.getDateEmission());
        transaction.setDateValeur(requestDto.getDateValeur());
        transaction.setDateOperations(requestDto.getDateOperation());
        transaction.setLibelle(requestDto.getLibelle() != null ? requestDto.getLibelle() : virementInternational.getLibelle());
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
     * Convertit une entité Transaction en DTO de réponse.
     *
     * @param transaction L'entité Transaction.
     * @return Le DTO de réponse.
     */
    public VirementInternationalTransactionResponseDto toResponseDto(Transaction transaction)  {
        if (transaction == null) {
            return null;
        }

        VirementInternationalTransactionResponseDto responseDto = new VirementInternationalTransactionResponseDto();

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
