package staport.finance.gBanque.Agios.dtos;

import staport.finance.gBanque.Agios.enums.StatutTrimestre;
import staport.finance.gBanque.Agios.enums.TypeAgios;

public record AgiosTrimestrielSearchDto(

        Long banqueId,
        TypeAgios type,
        Integer annee,
        Integer trimestre,
        StatutTrimestre statut,
        String search

) {}
