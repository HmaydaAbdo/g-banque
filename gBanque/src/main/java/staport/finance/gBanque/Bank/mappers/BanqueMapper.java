package staport.finance.gBanque.Bank.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import java.util.Base64;


@Component
public class BanqueMapper {

    /**
     * Converts a byte array (logo data) to a Base64 encoded String.
     * @param bytes The byte array of the logo.
     * @return Base64 encoded string, or null if input is null or empty.
     */
    public String bytesToBase64(byte[] bytes) {
        return (bytes != null && bytes.length > 0) ? Base64.getEncoder().encodeToString(bytes) : null;
    }

    /**
     * Converts a BanqueRequest DTO to a Banque entity for creation.
     * Fields like ID, creation/modification dates, and current balance are handled by JPA lifecycle callbacks or set separately.
     * logoData needs to be set by the service layer from MultipartFile.
     *
     * @param request The BanqueRequest DTO.
     * @return A new Banque entity.
     */
    public Banque toEntity(BanqueRequest request) {
        if (request == null) {
            return null;
        }

        Banque banque = new Banque();
        banque.setNom(request.getNom());
        banque.setNumeroDeCompte(request.getNumeroDeCompte());
        banque.setDevise(request.getDevise());
        banque.setAddress(request.getAddress());
        banque.setAgence(request.getAgence());

        return banque;
    }

    /**
     * Converts a Banque entity to a BanqueResponse DTO.
     *
     * @param banque The Banque entity.
     * @return A BanqueResponse DTO.
     */
    public BanqueResponse toDto(Banque banque) {
        if (banque == null) {
            return null;
        }

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

        return response;
    }



    /**
     * Updates an existing Banque entity from a BanqueRequest DTO.
     * Ignores ID, creation/modification dates, account number, and balance as they shouldn't be updated via this DTO.
     * logoData needs to be updated by the service layer from MultipartFile.
     *
     * @param request The BanqueRequest DTO with updated data.
     * @param banque The existing Banque entity to be updated.
     */
    public void updateBanqueFromDto(BanqueRequest request, Banque banque) {
        if (request == null || banque == null) {
            return; // Or throw IllegalArgumentException
        }

        banque.setNom(request.getNom());
        banque.setDevise(request.getDevise());
        banque.setAddress(request.getAddress());
        banque.setAgence(request.getAgence());

    }

    /**
     * Converts a Banque entity to a BankAcceuilResponse DTO for the dashboard view.
     *
     * @param banque The Banque entity.
     * @return A BankAcceuilResponse DTO.
     */
    public BankAcceuilResponse toAcceuilDto(Banque banque) {
        if (banque == null) {
            return null;
        }

        BankAcceuilResponse response = new BankAcceuilResponse();
        response.setBankId(banque.getId());
        response.setName(banque.getNom());
        response.setSoldeActuelle(banque.getSoldeActuel());
        response.setSoldeDepart(banque.getSoldeDepart());
        // Convert logo byte[] to Base64 String
        response.setLogoBase64(bytesToBase64(banque.getLogoData()));

        // plafondFC is deliberately not set here, as it's computed/fetched in the service layer.
        response.setPlafondFC(null); // Ensure it's null by default

        return response;
    }


}