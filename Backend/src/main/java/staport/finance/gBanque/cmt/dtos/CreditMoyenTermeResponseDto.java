package staport.finance.gBanque.cmt.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO utilisé pour renvoyer les informations d'un crédit moyen terme au client.
 */
public record CreditMoyenTermeResponseDto(
        Long id,
        BanqueDto banque,
        String designation,
        BigDecimal nominal,
        Integer periodicite,
        Integer duree,
        BigDecimal echeance,
        BigDecimal taux,
        LocalDate dateDebut,
        OffsetDateTime dateCreation,
        OffsetDateTime dateModification,
        boolean completed
) {

    /**
     * DTO imbriqué pour la banque.
     */
    public record BanqueDto(Long id, String nom) {
    }
}
