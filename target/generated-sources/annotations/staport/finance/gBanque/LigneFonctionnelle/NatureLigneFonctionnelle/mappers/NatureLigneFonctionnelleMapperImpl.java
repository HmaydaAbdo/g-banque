package staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.mappers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.dtos.NatureLigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-04T09:41:32+0100",
    comments = "version: 1.6.1, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class NatureLigneFonctionnelleMapperImpl implements NatureLigneFonctionnelleMapper {

    @Override
    public NatureLigneFonctionnelle toEntity(NatureLigneFonctionnelleRequest request) {
        if ( request == null ) {
            return null;
        }

        NatureLigneFonctionnelle natureLigneFonctionnelle = new NatureLigneFonctionnelle();

        natureLigneFonctionnelle.setLabel( request.getLabel() );
        natureLigneFonctionnelle.setDescription( request.getDescription() );

        return natureLigneFonctionnelle;
    }

    @Override
    public NatureLigneFonctionnelleResponse toDto(NatureLigneFonctionnelle entity) {
        if ( entity == null ) {
            return null;
        }

        NatureLigneFonctionnelleResponse natureLigneFonctionnelleResponse = new NatureLigneFonctionnelleResponse();

        natureLigneFonctionnelleResponse.setId( entity.getId() );
        natureLigneFonctionnelleResponse.setLabel( entity.getLabel() );
        natureLigneFonctionnelleResponse.setDescription( entity.getDescription() );
        natureLigneFonctionnelleResponse.setDateCreation( entity.getDateCreation() );
        natureLigneFonctionnelleResponse.setDateModification( entity.getDateModification() );

        return natureLigneFonctionnelleResponse;
    }

    @Override
    public List<NatureLigneFonctionnelleResponse> toDtoList(List<NatureLigneFonctionnelle> entities) {
        if ( entities == null ) {
            return null;
        }

        List<NatureLigneFonctionnelleResponse> list = new ArrayList<NatureLigneFonctionnelleResponse>( entities.size() );
        for ( NatureLigneFonctionnelle natureLigneFonctionnelle : entities ) {
            list.add( toDto( natureLigneFonctionnelle ) );
        }

        return list;
    }

    @Override
    public void updateEntityFromDto(NatureLigneFonctionnelleRequest request, NatureLigneFonctionnelle entity) {
        if ( request == null ) {
            return;
        }

        entity.setLabel( request.getLabel() );
        entity.setDescription( request.getDescription() );
    }
}
