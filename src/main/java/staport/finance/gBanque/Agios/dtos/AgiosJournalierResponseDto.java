package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record AgiosJournalierResponseDto(

        Long id,
        TypeAgios typeAgios,
        LocalDate dateValeur,
        Long banqueId,
        String banqueNom,
        BigDecimal soldeJournalier,
        BigDecimal baseCalcul,
        BigDecimal tauxApplique,
        BigDecimal agiosHT,
        BigDecimal tva,
        BigDecimal agiosTTC,
        OffsetDateTime dateCalcul,
        int nbRecalculs,
        /** Identifiant du marché — null pour les agios Facilité de Caisse. */
        Long marcheId,
        /** Nom du marché — null pour les agios Facilité de Caisse. */
        String marcheNom

) {}
