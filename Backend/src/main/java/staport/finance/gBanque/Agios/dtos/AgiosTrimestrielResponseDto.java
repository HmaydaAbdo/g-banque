package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record AgiosTrimestrielResponseDto(

        Long id,
        TypeAgios typeAgios,
        int trimestre,
        int annee,
        LocalDate dateDebut,
        LocalDate dateFin,
        Long banqueId,
        String banqueNom,
        BigDecimal totalAgiosHT,
        BigDecimal totalTVA,
        BigDecimal totalAgiosTTC,
        int nbJoursCalcules,
        int nbJoursTotal,
        StatutTrimestre statut,
        OffsetDateTime dateDernierCalcul,
        OffsetDateTime dateCloture,
        Long agiosDefinitifId,
        List<AgiosJournalierResponseDto> detailJournalier

) {}
