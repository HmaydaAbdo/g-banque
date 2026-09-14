package staport.finance.gBanque.Agios.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Agios.dtos.AgiosJournalierResponseDto;
import staport.finance.gBanque.Agios.dtos.AgiosTrimestrielResponseDto;
import staport.finance.gBanque.Agios.entities.AgiosJournalier;
import staport.finance.gBanque.Agios.entities.AgiosTrimestriel;

/**
 * Mapper MapStruct pour le calcul automatique d'agios (trimestres/journaliers).
 * Conversion uniquement — aucune logique métier.
 */
@Mapper(componentModel = "spring")
public interface AgiosCalculatorMapper {

    @Mapping(target = "banqueId",   source = "banque.id")
    @Mapping(target = "banqueNom",  source = "banque.nom")
    @Mapping(target = "detailJournalier", ignore = true)
    AgiosTrimestrielResponseDto toTrimDto(AgiosTrimestriel entity);

    @Mapping(target = "banqueId",  source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    @Mapping(target = "marcheId",  source = "marche.id")
    @Mapping(target = "marcheNumero", source = "marche.numero")
    AgiosJournalierResponseDto toJourDto(AgiosJournalier entity);
}
