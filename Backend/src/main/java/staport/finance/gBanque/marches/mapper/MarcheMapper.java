package staport.finance.gBanque.marches.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.marches.dtos.request.MarcheRequestDTO;
import staport.finance.gBanque.marches.dtos.response.MarcheResponseDTO;
import staport.finance.gBanque.marches.entities.Marche;

/**
 * Mapper du module Marches. Conversion uniquement — aucune logique métier, aucun log
 * (conventions/architecture.md, conventions/logging.md).
 */
@Mapper(componentModel = "spring")
public interface MarcheMapper {

    /**
     * Convertit un {@link MarcheRequestDTO} en nouvelle entité {@link Marche}, avec la
     * {@link Banque} déjà résolue par l'appelant (jamais via {@code BanqueRepository}
     * directement — encapsulation des domaines, architecture.md).
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "numero", source = "dto.numero")
    @Mapping(target = "nom", source = "dto.nom")
    @Mapping(target = "montant", source = "dto.montant")
    @Mapping(target = "maitreDouvrage", source = "dto.maitreDouvrage")
    @Mapping(target = "dateDebutDesTravaux", source = "dto.dateDebutDesTravaux")
    @Mapping(target = "dateDAchievementDesTravaux", source = "dto.dateDAchievementDesTravaux")
    @Mapping(target = "banque", source = "banque")
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "lignesFonctionnelles", ignore = true)
    @Mapping(target = "lignesFonctionnellesSpecifiques", ignore = true)
    @Mapping(target = "prefinancements", ignore = true)
    @Mapping(target = "droitConstates", ignore = true)
    @Mapping(target = "factorings", ignore = true)
    @Mapping(target = "decomptes", ignore = true)
    Marche toEntity(MarcheRequestDTO dto, Banque banque);

    /**
     * Applique les champs modifiables d'un {@link MarcheRequestDTO} sur un {@link Marche}
     * existant. La banque n'est pas mappée ici — son remplacement est optionnel à la mise à
     * jour, le service décide de l'affecter séparément si {@code requestDTO.banqueId()} est
     * fourni.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "banque", ignore = true)
    @Mapping(target = "actif", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "lignesFonctionnelles", ignore = true)
    @Mapping(target = "lignesFonctionnellesSpecifiques", ignore = true)
    @Mapping(target = "prefinancements", ignore = true)
    @Mapping(target = "droitConstates", ignore = true)
    @Mapping(target = "factorings", ignore = true)
    @Mapping(target = "decomptes", ignore = true)
    void applyUpdate(@MappingTarget Marche entity, MarcheRequestDTO dto);

    @Mapping(target = "banqueId", source = "banque.id")
    @Mapping(target = "banqueNom", source = "banque.nom")
    MarcheResponseDTO toResponseDTO(Marche entity);
}
