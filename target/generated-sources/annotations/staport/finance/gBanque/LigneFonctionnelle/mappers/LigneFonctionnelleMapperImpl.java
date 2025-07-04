package staport.finance.gBanque.LigneFonctionnelle.mappers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.entities.Banque;
import staport.finance.gBanque.LigneFonctionnelle.NatureLigneFonctionnelle.entities.NatureLigneFonctionnelle;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleRequest;
import staport.finance.gBanque.LigneFonctionnelle.dtos.LigneFonctionnelleResponse;
import staport.finance.gBanque.LigneFonctionnelle.entities.LigneFonctionnelle;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-27T12:23:12+0100",
    comments = "version: 1.6.1, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class LigneFonctionnelleMapperImpl extends LigneFonctionnelleMapper {

    @Override
    public LigneFonctionnelle toEntity(LigneFonctionnelleRequest request) {
        if ( request == null ) {
            return null;
        }

        LigneFonctionnelle ligneFonctionnelle = new LigneFonctionnelle();

        ligneFonctionnelle.setDateDepart( request.getDateDepart() );
        ligneFonctionnelle.setDateFin( request.getDateFin() );
        ligneFonctionnelle.setActif( setActifDefaultTrue( request.getActif() ) );
        ligneFonctionnelle.setPlafond( request.getPlafond() );
        ligneFonctionnelle.setTaux( request.getTaux() );

        ligneFonctionnelle.setNatureLigneFonctionnelle( mapNatureLigneFonctionnelleIdToEntity(request.getNatureLigneFonctionnelleId()) );

        return ligneFonctionnelle;
    }

    @Override
    public LigneFonctionnelleResponse toDto(LigneFonctionnelle ligneFonctionnelle) {
        if ( ligneFonctionnelle == null ) {
            return null;
        }

        LigneFonctionnelleResponse ligneFonctionnelleResponse = new LigneFonctionnelleResponse();

        ligneFonctionnelleResponse.setBanqueId( ligneFonctionnelleBanqueId( ligneFonctionnelle ) );
        ligneFonctionnelleResponse.setBanqueNom( ligneFonctionnelleBanqueNom( ligneFonctionnelle ) );
        ligneFonctionnelleResponse.setNatureLigneFonctionnelleId( ligneFonctionnelleNatureLigneFonctionnelleId( ligneFonctionnelle ) );
        ligneFonctionnelleResponse.setNatureLigneFonctionnelleLabel( ligneFonctionnelleNatureLigneFonctionnelleLabel( ligneFonctionnelle ) );
        ligneFonctionnelleResponse.setNatureLigneFonctionnelleDescription( ligneFonctionnelleNatureLigneFonctionnelleDescription( ligneFonctionnelle ) );
        ligneFonctionnelleResponse.setDateDepart( ligneFonctionnelle.getDateDepart() );
        ligneFonctionnelleResponse.setDateFin( ligneFonctionnelle.getDateFin() );
        ligneFonctionnelleResponse.setActif( ligneFonctionnelle.isActif() );
        ligneFonctionnelleResponse.setId( ligneFonctionnelle.getId() );
        ligneFonctionnelleResponse.setPlafond( ligneFonctionnelle.getPlafond() );
        ligneFonctionnelleResponse.setTaux( ligneFonctionnelle.getTaux() );
        ligneFonctionnelleResponse.setDateCreation( ligneFonctionnelle.getDateCreation() );
        ligneFonctionnelleResponse.setDateModification( ligneFonctionnelle.getDateModification() );

        return ligneFonctionnelleResponse;
    }

    @Override
    public List<LigneFonctionnelleResponse> toDtoList(List<LigneFonctionnelle> ligneFonctionnelles) {
        if ( ligneFonctionnelles == null ) {
            return null;
        }

        List<LigneFonctionnelleResponse> list = new ArrayList<LigneFonctionnelleResponse>( ligneFonctionnelles.size() );
        for ( LigneFonctionnelle ligneFonctionnelle : ligneFonctionnelles ) {
            list.add( toDto( ligneFonctionnelle ) );
        }

        return list;
    }

    @Override
    public void updateLigneFonctionnelleFromDto(LigneFonctionnelleRequest request, LigneFonctionnelle ligneFonctionnelle) {
        if ( request == null ) {
            return;
        }

        ligneFonctionnelle.setDateDepart( request.getDateDepart() );
        ligneFonctionnelle.setDateFin( request.getDateFin() );
        if ( request.getActif() != null ) {
            ligneFonctionnelle.setActif( request.getActif() );
        }
        ligneFonctionnelle.setPlafond( request.getPlafond() );
        ligneFonctionnelle.setTaux( request.getTaux() );

        ligneFonctionnelle.setNatureLigneFonctionnelle( mapNatureLigneFonctionnelleIdToEntity(request.getNatureLigneFonctionnelleId()) );
    }

    private Long ligneFonctionnelleBanqueId(LigneFonctionnelle ligneFonctionnelle) {
        Banque banque = ligneFonctionnelle.getBanque();
        if ( banque == null ) {
            return null;
        }
        return banque.getId();
    }

    private String ligneFonctionnelleBanqueNom(LigneFonctionnelle ligneFonctionnelle) {
        Banque banque = ligneFonctionnelle.getBanque();
        if ( banque == null ) {
            return null;
        }
        return banque.getNom();
    }

    private Long ligneFonctionnelleNatureLigneFonctionnelleId(LigneFonctionnelle ligneFonctionnelle) {
        NatureLigneFonctionnelle natureLigneFonctionnelle = ligneFonctionnelle.getNatureLigneFonctionnelle();
        if ( natureLigneFonctionnelle == null ) {
            return null;
        }
        return natureLigneFonctionnelle.getId();
    }

    private String ligneFonctionnelleNatureLigneFonctionnelleLabel(LigneFonctionnelle ligneFonctionnelle) {
        NatureLigneFonctionnelle natureLigneFonctionnelle = ligneFonctionnelle.getNatureLigneFonctionnelle();
        if ( natureLigneFonctionnelle == null ) {
            return null;
        }
        return natureLigneFonctionnelle.getLabel();
    }

    private String ligneFonctionnelleNatureLigneFonctionnelleDescription(LigneFonctionnelle ligneFonctionnelle) {
        NatureLigneFonctionnelle natureLigneFonctionnelle = ligneFonctionnelle.getNatureLigneFonctionnelle();
        if ( natureLigneFonctionnelle == null ) {
            return null;
        }
        return natureLigneFonctionnelle.getDescription();
    }
}
