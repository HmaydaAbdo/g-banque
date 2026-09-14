package staport.finance.gBanque.rapprochement.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.rapprochement.dtos.RapprochementAggregates;
import staport.finance.gBanque.rapprochement.dtos.RapprochementResponseDto;
import staport.finance.gBanque.rapprochement.dtos.ReportDto;
import staport.finance.gBanque.rapprochement.entities.Rapprochement;

/**
 * Les agrégats (totalDebit, totalCredit, TVA, soldeFin) sont toujours calculés à la volée via
 * {@link RapprochementAggregates} et jamais lus depuis des champs stockés sur l'entité — voir
 * {@code RapprochementServiceImpl}. {@code transactions} n'est jamais mappé ici : attaché après
 * coup via {@code RapprochementResponseDto.withTransactions(...)}/{@code ReportDto.withTransactions(...)}.
 */
@Mapper(componentModel = "spring")
public interface RapprochementMapper {

    @Mapping(target = "banqueId", source = "entity.banque.id")
    @Mapping(target = "banqueNom", source = "entity.banque.nom")
    @Mapping(target = "dateRapprochement", source = "entity.dateRapprochement")
    @Mapping(target = "soldeDebut", source = "entity.soldeDebut")
    @Mapping(target = "statutRapprochement", source = "entity.statut")
    @Mapping(target = "totalDebit", source = "agg.totalDebit")
    @Mapping(target = "totalCredit", source = "agg.totalCredit")
    @Mapping(target = "tvaDeductible", source = "agg.tvaDeductible")
    @Mapping(target = "tvaCollectee", source = "agg.tvaCollectee")
    @Mapping(target = "tvaDue", expression = "java(agg.tvaDue())")
    @Mapping(target = "soldeFin", expression = "java(agg.soldeFin(entity.getSoldeDebut()))")
    @Mapping(target = "transactions", ignore = true)
    RapprochementResponseDto toResponseDto(Rapprochement entity, RapprochementAggregates agg);

    @Mapping(target = "banqueId", source = "entity.banque.id")
    @Mapping(target = "banqueNom", source = "entity.banque.nom")
    @Mapping(target = "dateRapprochement", source = "entity.dateRapprochement")
    @Mapping(target = "soldeDebut", source = "entity.soldeDebut")
    @Mapping(target = "totalDebit", source = "agg.totalDebit")
    @Mapping(target = "totalCredit", source = "agg.totalCredit")
    @Mapping(target = "tvaDeductible", source = "agg.tvaDeductible")
    @Mapping(target = "tvaCollectee", source = "agg.tvaCollectee")
    @Mapping(target = "tvaDue", expression = "java(agg.tvaDue())")
    @Mapping(target = "soldeFin", expression = "java(agg.soldeFin(entity.getSoldeDebut()))")
    @Mapping(target = "transactions", ignore = true)
    ReportDto toReportDto(Rapprochement entity, RapprochementAggregates agg);
}
