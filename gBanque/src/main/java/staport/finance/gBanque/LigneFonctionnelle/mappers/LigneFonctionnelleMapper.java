package staport.finance.gBanque.LigneFonctionnelle.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.repositories.NatureLigneFonctionnelleRepository;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;
import staport.finance.gBanque.exception.customExceptions.ResourceNotFoundException;

import java.util.List;

@Mapper(componentModel = "spring")
public abstract class LigneFonctionnelleMapper {

    @Autowired
    protected NatureLigneFonctionnelleRepository natureLigneFonctionnelleRepository;

    // Request to Entity for creation
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "banque", ignore = true)
    @Mapping(target = "natureLigneFonctionnelle", expression = "java(mapNatureLigneFonctionnelleIdToEntity(request.getNatureLigneFonctionnelleId()))")
    @Mapping(source = "dateDepart", target = "dateDepart")
    @Mapping(source = "dateFin", target = "dateFin")
    public abstract LigneFonctionnelle toEntity(LigneFonctionnelleRequest request);

    // Entity to Response for reading
    @Mapping(source = "banque.id", target = "banqueId")
    @Mapping(source = "banque.nom", target = "banqueNom")
    @Mapping(source = "natureLigneFonctionnelle.id", target = "natureLigneFonctionnelleId")
    @Mapping(source = "natureLigneFonctionnelle.label", target = "natureLigneFonctionnelleLabel")
    @Mapping(source = "natureLigneFonctionnelle.description", target = "natureLigneFonctionnelleDescription")
    @Mapping(source = "dateDepart", target = "dateDepart")
    @Mapping(source = "dateFin", target = "dateFin")
    public abstract LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle);

    public abstract List<LigneFonctionnelleResponse> toDtoList(List<LigneFonctionnelle> ligneFonctionnelles);

    // Request to Entity for update
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "dateCreation", ignore = true)
    @Mapping(target = "dateModification", ignore = true)
    @Mapping(target = "banque", ignore = true)
    @Mapping(target = "natureLigneFonctionnelle", expression = "java(mapNatureLigneFonctionnelleIdToEntity(request.getNatureLigneFonctionnelleId()))")
    @Mapping(source = "dateDepart", target = "dateDepart")
    @Mapping(source = "dateFin", target = "dateFin")
    public abstract void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request, @MappingTarget LigneFonctionnelle ligneFonctionnelle); // Made public abstract

    // Helper method to fetch NatureLigneFonctionnelle entity by ID
    protected NatureLigneFonctionnelle mapNatureLigneFonctionnelleIdToEntity(Long natureLigneFonctionnelleId) {
        if (natureLigneFonctionnelleId == null) {
            return null;
        }
        return natureLigneFonctionnelleRepository.findById(natureLigneFonctionnelleId)
                .orElseThrow(() -> new ResourceNotFoundException("NatureLigneFonctionnelle non trouvée avec l'ID: " + natureLigneFonctionnelleId));
    }


}
