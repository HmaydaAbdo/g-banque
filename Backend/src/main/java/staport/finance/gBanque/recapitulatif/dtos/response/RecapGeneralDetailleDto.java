package staport.finance.gBanque.recapitulatif.dtos.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecapGeneralDetailleDto(
        Long banqueId,
        String banqueNom,
        LocalDate dateRecap,
        ResumeSectionDto resume,
        DecaissementSectionDto decaissements,
        EncaissementSectionDto encaissements,
        BigDecimal soldePrevisionnelFinMois
) {
}
