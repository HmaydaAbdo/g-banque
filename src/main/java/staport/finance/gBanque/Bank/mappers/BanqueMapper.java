package staport.finance.gBanque.Bank.mappers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import java.util.Base64;


@Component
public class BanqueMapper {


    private static final Logger logger = LoggerFactory.getLogger(BanqueMapper.class);


    public String bytesToBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            logger.debug("No logo data to convert to Base64");
            return null;
        }

        logger.debug("Converting logo data to Base64 (size: {} bytes)", bytes.length);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public Banque toEntity(BanqueRequest request) {
        if (request == null) {
            logger.warn("Attempted to convert null BanqueRequest to Entity");
            return null;
        }

        logger.debug("Converting BanqueRequest to Entity - request: {}", request);

        Banque banque = new Banque();
        banque.setNom(request.getNom());
        banque.setNumeroDeCompte(request.getNumeroDeCompte());
        banque.setDevise(request.getDevise());
        banque.setAddress(request.getAddress());
        banque.setAgence(request.getAgence());

        logger.debug("Successfully converted BanqueRequest to Entity for: {}", request.getNom());
        return banque;
    }


    public BanqueResponse toDto(Banque banque) {
        if (banque == null) {
            logger.warn("Attempted to convert null Banque Entity to DTO");
            return null;
        }

        logger.debug("Converting Banque Entity to DTO - ID: {}, Name: {}",
                banque.getId(), banque.getNom());

        BanqueResponse response = new BanqueResponse();
        response.setId(banque.getId());
        response.setNom(banque.getNom());
        response.setNumeroDeCompte(banque.getNumeroDeCompte());
        response.setDevise(banque.getDevise());
        response.setAddress(banque.getAddress());
        response.setAgence(banque.getAgence());
        response.setSoldeActuel(banque.getSoldeActuel());
        response.setSoldeDepart(banque.getSoldeDepart());
        response.setDateCreation(banque.getDateCreation());
        response.setDateModification(banque.getDateModification());

        // Convert logo byte[] to Base64 String
        response.setLogoBase64(bytesToBase64(banque.getLogoData()));

        logger.debug("Successfully converted Banque Entity to DTO - ID: {}, Has logo: {}",
                banque.getId(), banque.getLogoData() != null);
        return response;
    }

    public void updateBanqueFromDto(BanqueRequest request, Banque banque) {
        if (request == null) {
            logger.warn("Attempted to update Banque Entity with null BanqueRequest");
            return;
        }

        if (banque == null) {
            logger.warn("Attempted to update null Banque Entity");
            return;
        }

        logger.debug("Updating Banque Entity ID: {} with data from request - New name: {}",
                banque.getId(), request.getNom());

        banque.setNom(request.getNom());
        banque.setDevise(request.getDevise());
        banque.setAddress(request.getAddress());
        banque.setAgence(request.getAgence());

        logger.debug("Successfully updated Banque Entity ID: {}", banque.getId());
    }


    public BankAcceuilResponse toAcceuilDto(Banque banque) {
        if (banque == null) {
            logger.warn("Attempted to convert null Banque Entity to AcceuilDTO");
            return null;
        }

        logger.debug("Converting Banque Entity to AcceuilDTO - ID: {}, Name: {}, Solde: {}",
                banque.getId(), banque.getNom(), banque.getSoldeActuel());

        BankAcceuilResponse response = new BankAcceuilResponse();
        response.setBankId(banque.getId());
        response.setName(banque.getNom());
        response.setSoldeActuelle(banque.getSoldeActuel());
        response.setSoldeDepart(banque.getSoldeDepart());
        response.setDevise(banque.getDevise());
        // Convert logo byte[] to Base64 String
        response.setLogoBase64(bytesToBase64(banque.getLogoData()));

        // plafondFC is deliberately not set here, as it's computed/fetched in the service layer.
        response.setPlafondFC(null);

        logger.debug("Successfully converted Banque Entity to AcceuilDTO - ID: {}, Has logo: {}",
                banque.getId(), banque.getLogoData() != null);
        return response;
    }


}