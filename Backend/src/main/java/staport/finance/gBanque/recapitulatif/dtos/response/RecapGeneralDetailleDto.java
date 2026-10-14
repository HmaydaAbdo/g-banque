package staport.finance.gBanque.recapitulatif.dtos.response;

import staport.finance.gBanque.recapitulatif.enums.PeriodeRecap;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Récapitulatif général détaillé d'une banque : résumé du jour, puis décaissements et
 * encaissements par catégorie du lendemain jusqu'à {@code dateHorizon} (fin de mois ou fin de
 * trimestre selon {@code periode}), et solde prévisionnel à cette date.
 */
public record RecapGeneralDetailleDto(
        Long banqueId,
        String banqueNom,
        LocalDate dateRecap,
        PeriodeRecap periode,
        LocalDate dateHorizon,
        ResumeSectionDto resume,
        DecaissementSectionDto decaissements,
        EncaissementSectionDto encaissements,
        BigDecimal soldePrevisionnel
) {
}
