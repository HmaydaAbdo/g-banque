package staport.finance.gBanque.Bank.mappers;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-06-27T12:23:11+0100",
    comments = "version: 1.6.1, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class BanqueMapperImpl implements BanqueMapper {

    @Override
    public Banque toEntity(BanqueRequest banqueRequest) {
        if ( banqueRequest == null ) {
            return null;
        }

        Banque banque = new Banque();

        banque.setNom( banqueRequest.getNom() );
        banque.setNumeroDeCompte( banqueRequest.getNumeroDeCompte() );
        banque.setDevise( banqueRequest.getDevise() );
        banque.setAddress( banqueRequest.getAddress() );
        banque.setAgence( banqueRequest.getAgence() );

        return banque;
    }

    @Override
    public BanqueResponse toDto(Banque banque) {
        if ( banque == null ) {
            return null;
        }

        BanqueResponse banqueResponse = new BanqueResponse();

        banqueResponse.setLogoBase64( bytesToBase64( banque.getLogoData() ) );
        banqueResponse.setId( banque.getId() );
        banqueResponse.setNom( banque.getNom() );
        banqueResponse.setNumeroDeCompte( banque.getNumeroDeCompte() );
        banqueResponse.setDevise( banque.getDevise() );
        banqueResponse.setAddress( banque.getAddress() );
        banqueResponse.setAgence( banque.getAgence() );
        banqueResponse.setSoldeActuel( banque.getSoldeActuel() );
        banqueResponse.setDateCreation( banque.getDateCreation() );
        banqueResponse.setDateModification( banque.getDateModification() );

        return banqueResponse;
    }

    @Override
    public List<BanqueResponse> toDtoList(List<Banque> banques) {
        if ( banques == null ) {
            return null;
        }

        List<BanqueResponse> list = new ArrayList<BanqueResponse>( banques.size() );
        for ( Banque banque : banques ) {
            list.add( toDto( banque ) );
        }

        return list;
    }

    @Override
    public void updateBanqueFromDto(BanqueRequest banqueRequest, Banque banque) {
        if ( banqueRequest == null ) {
            return;
        }

        banque.setNom( banqueRequest.getNom() );
        banque.setDevise( banqueRequest.getDevise() );
        banque.setAddress( banqueRequest.getAddress() );
        banque.setAgence( banqueRequest.getAgence() );
    }

    @Override
    public BankAcceuilResponse toAcceuilDto(Banque banque) {
        if ( banque == null ) {
            return null;
        }

        BankAcceuilResponse bankAcceuilResponse = new BankAcceuilResponse();

        bankAcceuilResponse.setBankId( banque.getId() );
        bankAcceuilResponse.setLogoBase64( bytesToBase64( banque.getLogoData() ) );
        bankAcceuilResponse.setName( banque.getNom() );
        bankAcceuilResponse.setSoldeActuelle( banque.getSoldeActuel() );

        return bankAcceuilResponse;
    }

    @Override
    public List<BankAcceuilResponse> toAcceuilDtoList(List<Banque> banques) {
        if ( banques == null ) {
            return null;
        }

        List<BankAcceuilResponse> list = new ArrayList<BankAcceuilResponse>( banques.size() );
        for ( Banque banque : banques ) {
            list.add( toAcceuilDto( banque ) );
        }

        return list;
    }
}
