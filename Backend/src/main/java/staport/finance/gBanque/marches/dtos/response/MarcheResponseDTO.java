package staport.finance.gBanque.marches.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MarcheResponseDTO(
        Long id,
        String numero,
        String nom,
        BigDecimal montant,
        String maitreDouvrage,
        LocalDate dateDebutDesTravaux,
        LocalDate dateDAchievementDesTravaux,
        Long banqueId,
        String banqueNom,
        boolean actif,
        LocalDate createdAt,
        LocalDate modifiedAt
) {
}
