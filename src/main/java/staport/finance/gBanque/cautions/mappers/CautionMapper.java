package staport.finance.gBanque.cautions.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.cautions.dtos.request.CautionCreateDto;
import staport.finance.gBanque.cautions.dtos.request.CautionUpdateDto;
import staport.finance.gBanque.cautions.dtos.response.CautionResponseDto;
import staport.finance.gBanque.cautions.entities.Caution;

/**
 * Mapper MapStruct pour le module Caution — cohérent avec la convention du projet
 * ({@code @Mapper(componentModel = "spring")}, voir {@code conventions/architecture.md}).
 *
 * <p>Les associations ({@code banque}, {@code nature}, {@code marche}) sont résolues et
 * settées par le service après résolution des IDs — jamais par le mapper (le mapper ne
 * connaît aucun Repository, conformément à {@code conventions/architecture.md}).
 */
@Mapper(componentModel = "spring")
public interface CautionMapper {

    // ── Create DTO → Entity ───────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", ignore = true)
    @Mapping(target = "nature", ignore = true)
    @Mapping(target = "marche", ignore = true)
    @Mapping(target = "actif", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Caution toEntity(CautionCreateDto dto);

    // ── Entity → Response DTO ─────────────────────────────────────────────────

    @Mapping(source = "banque.id",     target = "banqueId")
    @Mapping(source = "banque.nom",    target = "banqueNom")
    @Mapping(source = "nature.id",     target = "natureId")
    @Mapping(source = "nature.label",  target = "natureLabel")
    @Mapping(source = "nature.code",   target = "natureCode")
    @Mapping(source = "marche.id",     target = "marcheId")
    @Mapping(source = "marche.numero", target = "marcheNumero")
    @Mapping(source = "marche.nom",    target = "marcheNom")
    CautionResponseDto toDto(Caution caution);

    // ── Apply Update ──────────────────────────────────────────────────────────

    /**
     * Applique tous les champs modifiables sur une entité existante.
     * Tous les champs sont modifiables : {@code numeroCaution}, {@code dateDePrise},
     * {@code designation}, {@code organisme}, {@code montant}, {@code taux}.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", ignore = true)
    @Mapping(target = "nature", ignore = true)
    @Mapping(target = "marche", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void applyUpdate(CautionUpdateDto dto, @MappingTarget Caution caution);
}
