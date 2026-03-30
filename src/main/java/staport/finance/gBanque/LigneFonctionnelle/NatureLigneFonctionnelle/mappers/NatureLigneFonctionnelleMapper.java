package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers;

import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NatureLigneFonctionnelleMapper {

    public NatureLigneFonctionnelle toEntity(NatureLigneFonctionnelleRequest request) {
        if (request == null) return null;

        NatureLigneFonctionnelle entity = new NatureLigneFonctionnelle();
        entity.setCode(request.code());
        entity.setLabel(request.label());
        entity.setDescription(request.description());
        entity.setPortee(request.portee());
        return entity;
    }

    public NatureLigneFonctionnelleResponse toDto(NatureLigneFonctionnelle entity) {
        if (entity == null) return null;

        // Using the canonical constructor of your NatureLigneFonctionnelleResponse record
        return new NatureLigneFonctionnelleResponse(
                entity.getId(),
                entity.getCode(),
                entity.getLabel(),
                entity.getDescription(),
                entity.getPortee(),
                entity.getDateCreation(),
                entity.getDateModification()
        );
    }

    public List<NatureLigneFonctionnelleResponse> toDtoList(List<NatureLigneFonctionnelle> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(this::toDto)
                .toList(); // Stream.toList() is standard since Java 16
    }


}