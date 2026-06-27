package staport.finance.gBanque.Bank.mappers;

import org.springframework.stereotype.Component;
import staport.finance.gBanque.Bank.dtos.request.BanqueRequest;
import staport.finance.gBanque.Bank.dtos.response.BankAcceuilResponse;
import staport.finance.gBanque.Bank.dtos.response.BanqueResponse;
import staport.finance.gBanque.Bank.entities.Banque;

import java.math.BigDecimal;
import java.util.Base64;


@Component
public class BanqueMapper {

    public String bytesToBase64(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    public Banque toEntity(BanqueRequest request) {
        Banque banque = new Banque();
        banque.setNom(request.nom());
        banque.setNumeroDeCompte(request.numeroDeCompte());
        banque.setDevise(request.devise());
        banque.setAdresse(request.adresse());
        banque.setAgence(request.agence());
        return banque;
    }

    public BanqueResponse toDto(Banque banque, BigDecimal computedSolde) {
        return new BanqueResponse(
                banque.getId(),
                bytesToBase64(banque.getLogoData()),
                banque.getNom(),
                banque.getNumeroDeCompte(),
                banque.getDevise(),
                banque.getAdresse(),
                banque.getAgence(),
                computedSolde,
                banque.getDateCreation(),
                banque.getDateModification()
        );
    }

    public BankAcceuilResponse toAcceuilDto(Banque banque,
                                             BigDecimal computedBalance,
                                             BigDecimal plafondFC,
                                             BigDecimal soldeDebut,
                                             long lcnsEchusCount,
                                             long virementsEchusCount) {
        return new BankAcceuilResponse(
                banque.getId(),
                bytesToBase64(banque.getLogoData()),
                banque.getNom(),
                computedBalance,
                soldeDebut,
                plafondFC,
                banque.getDevise(),
                lcnsEchusCount,
                virementsEchusCount
        );
    }
}
