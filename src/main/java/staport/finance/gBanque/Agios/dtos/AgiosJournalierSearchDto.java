package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.TypeAgios;

import java.time.LocalDate;

public record AgiosJournalierSearchDto(

        Long banqueId,
        TypeAgios type,
        Integer trimestre,
        Integer annee,
        LocalDate dateLimite

) {}
