package staport.finance.gBanque.cmt.echeances.dtos;

import staport.finance.gBanque.cmt.echeances.enums.StatutEcheanceCMT;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO utilisé pour renvoyer les informations d'une échéance de crédit au client.
 */
public record EcheanceCMTResponseDto(
        Long id,
        Integer numeroEcheance,
        LocalDate dateEcheance,
        LocalDate dateValuer,
        LocalDate dateOperation,
        BigDecimal montantTtc,
        String observation,
        String libelle,
        StatutEcheanceCMT statutPaiement,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification,
        CreditMoyenTermeDto credit
) {

    /**
     * DTO imbriqué pour exposer les informations minimales du crédit parent.
     */
    public record CreditMoyenTermeDto(Long id, String designation) {
    }
}
