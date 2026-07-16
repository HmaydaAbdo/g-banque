package staport.finance.gBanque.transactions.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.transactions.dtos.TransactionResponseDto;
import staport.finance.gBanque.transactions.entities.Transaction;

import java.math.BigDecimal;

/**
 * Conversion {@link Transaction} → {@link TransactionResponseDto}.
 * Le libellé et l'observation sont normalisés en majuscules ; les montants et taxes {@code null}
 * sont ramenés à {@link BigDecimal#ZERO} ; le statut du rapprochement est par défaut
 * {@code EN_COURS} lorsque la relation n'est pas encore chargée.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "observation", expression = "java(toUpper(transaction.getObservation()))")
    @Mapping(target = "libelle", expression = "java(toUpper(transaction.getLibelle()))")
    @Mapping(target = "tvaDeductible", expression = "java(zeroIfNull(transaction.getTvaDeductible()))")
    @Mapping(target = "tvaCollectee", expression = "java(zeroIfNull(transaction.getTvaCollectee()))")
    @Mapping(target = "debit", expression = "java(zeroIfNull(transaction.getDebit()))")
    @Mapping(target = "credit", expression = "java(zeroIfNull(transaction.getCredit()))")
    @Mapping(target = "statutRapprochement", expression = "java(transaction.getRapprochement() != null ? transaction.getRapprochement().getStatut() : staport.finance.gBanque.rapprochement.enums.StatutRapprochement.EN_COURS)")
    @Mapping(target = "banqueDePrelevementId", expression = "java(transaction.getBanqueDePrelevement() != null ? transaction.getBanqueDePrelevement().getId() : null)")
    @Mapping(target = "banqueDePrelevementNom", expression = "java(transaction.getBanqueDePrelevement() != null ? toUpper(transaction.getBanqueDePrelevement().getNom()) : null)")
    TransactionResponseDto toDto(Transaction transaction);

    default String toUpper(String value) {
        return value != null ? value.toUpperCase() : null;
    }

    default BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
