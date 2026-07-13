package staport.finance.gBanque.decomptes.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.decomptes.dtos.request.DecompteRequestDto;
import staport.finance.gBanque.decomptes.dtos.response.DecompteResponseDto;
import staport.finance.gBanque.decomptes.entities.Decompte;
import staport.finance.gBanque.marches.entities.Marche;
import staport.finance.gBanque.prefinancement.enums.Status;

/**
 * Mapper MapStruct pour le module Décompte (conventions/architecture.md).
 *
 * <p>Les associations ({@code marche}, {@code banqueEncaissement}) sont résolues par le Service
 * (via {@code IMarcheService}/{@code IBanqueService}) et passées déjà chargées — le mapper ne
 * connaît aucun Repository.
 */
@Mapper(componentModel = "spring")
public interface DecompteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "marche", source = "marche")
    @Mapping(target = "banqueEncaissement", source = "banqueEncaissement")
    @Mapping(target = "montant", source = "dto.montant")
    @Mapping(target = "dateEmission", source = "dto.dateEmission")
    @Mapping(target = "libelle", source = "dto.libelle")
    @Mapping(target = "status", constant = "PREVU")
    @Mapping(target = "dateOperation", ignore = true)
    @Mapping(target = "dateValeur", ignore = true)
    @Mapping(target = "observation", source = "marche.numero")
    Decompte toEntity(DecompteRequestDto dto, Marche marche, Banque banqueEncaissement);

    @Mapping(source = "marche.id", target = "marcheId")
    @Mapping(source = "marche.nom", target = "marcheName")
    @Mapping(source = "marche.numero", target = "marcheNumero")
    @Mapping(source = "marche.banque.id", target = "marcheBanqueId")
    @Mapping(source = "marche.banque.nom", target = "marcheBanqueName")
    @Mapping(source = "banqueEncaissement.id", target = "banqueEncaissementId")
    @Mapping(source = "banqueEncaissement.nom", target = "banqueEncaissementName")
    DecompteResponseDto toDto(Decompte decompte);
}
