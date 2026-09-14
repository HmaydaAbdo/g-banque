package staport.finance.gBanque.impot_taxes.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.impot_taxes.dtos.request.ImpotEtTaxeRequestDto;
import staport.finance.gBanque.impot_taxes.dtos.response.ImpotEtTaxeResponseDto;
import staport.finance.gBanque.impot_taxes.entities.ImpotEtTaxe;

/**
 * Mapper MapStruct pour le module Impôts et Taxes (conventions/architecture.md).
 *
 * <p>La {@code banque} est résolue par le Service (via {@code IBanqueService}) et passée déjà
 * chargée — le mapper ne connaît aucun Repository.
 */
@Mapper(componentModel = "spring")
public interface ImpotEtTaxeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", source = "banque")
    @Mapping(target = "montant", source = "dto.montant")
    @Mapping(target = "libelle", source = "dto.libelle")
    @Mapping(target = "observation", source = "dto.observation")
    @Mapping(target = "motif", source = "dto.motif")
    @Mapping(target = "dateEmission", source = "dto.dateEmission")
    @Mapping(target = "status", constant = "PREVU")
    @Mapping(target = "dateOperation", ignore = true)
    @Mapping(target = "dateValeur", ignore = true)
    @Mapping(target = "commission", ignore = true)
    @Mapping(target = "caution", ignore = true)
    // dateCreation/dateModification : gérés par @PrePersist/@PreUpdate sur l'entité (OffsetDateTime).
    // Ignorés explicitement pour éviter que MapStruct ne les auto-mappe depuis Banque.dateCreation/
    // dateModification (LocalDate, champ homonyme mais type différent) — erreur de build sinon
    // ("Can't map property LocalDate dateCreation to OffsetDateTime dateCreation").
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    ImpotEtTaxe toEntity(ImpotEtTaxeRequestDto dto, Banque banque);

    @Mapping(source = "banque.id", target = "banqueId")
    @Mapping(source = "banque.nom", target = "banqueNom")
    ImpotEtTaxeResponseDto toDto(ImpotEtTaxe entity);
}
