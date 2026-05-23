package staport.finance.gBanque.Bank.mappers;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.dtos.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;
import java.math.BigDecimal;
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
        banque.setNom(request.nom());
        banque.setNumeroDeCompte(request.numeroDeCompte());
        banque.setDevise(request.devise());
        banque.setAddress(request.address());
        banque.setAgence(request.agence());

        logger.debug("Successfully converted BanqueRequest to Entity for: {}", request.nom());
        return banque;
    }

    public BanqueResponse toDto(Banque banque, BigDecimal computedSolde) {
        if (banque == null) {
            logger.warn("Attempted to convert null Banque Entity to DTO");
            return null;
        }

        // Convert logo once
        String logoBase64 = bytesToBase64(banque.getLogoData());

        // Atomic creation: The object is born fully populated and immutable
        return new BanqueResponse(
                banque.getId(),
                logoBase64,
                banque.getNom(),
                banque.getNumeroDeCompte(),
                banque.getDevise(),
                banque.getAddress(),
                banque.getAgence(),
                computedSolde,
                banque.getSoldeDepart(),
                banque.getDateCreation(),
                banque.getDateModification()
        );
    }

    public BankAcceuilResponse toAcceuilDto(Banque banque,
                                              BigDecimal computedBalance,
                                              BigDecimal plafondFC,
                                              long lcnsEchusCount,
                                              long virementsEchusCount) {
        if (banque == null) {
            logger.warn("Attempted to convert null Banque Entity to BankAcceuilResponse");
            return null;
        }

        String logoBase64 = bytesToBase64(banque.getLogoData());

        return new BankAcceuilResponse(
                banque.getId(),
                logoBase64,
                banque.getNom(),
                computedBalance,
                banque.getSoldeDepart(),
                plafondFC,
                banque.getDevise(),
                lcnsEchusCount,
                virementsEchusCount
        );
    }

}