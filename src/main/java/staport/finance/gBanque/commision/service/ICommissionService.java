package staport.finance.gBanque.commision.service;

import staport.finance.gBanque.commision.dtos.CommissionRequestDto;
import staport.finance.gBanque.commision.dtos.CommissionResponseDto;

/**
 * Interface publique du module commission.
 * Exposée aux modules tiers (ex : leasing) pour éviter toute dépendance directe
 * sur la classe concrète {@link CommissionService}.
 */
public interface ICommissionService {

    /**
     * Crée une nouvelle commission.
     *
     * @param requestDto Les données de la commission à créer
     * @return Le DTO de la commission créée
     */
    CommissionResponseDto createCommission(CommissionRequestDto requestDto);

    /**
     * Supprime toutes les commissions dont le libellé correspond au pattern
     * "Commission Prelevement Leasing contrat N°{numeroContrat}%".
     * Appelée lors de la suppression ou régénération des échéances d'un contrat de leasing.
     *
     * @param numeroContrat Le numéro du contrat de leasing
     */
    void deleteCommissionsByContratLeasing(String numeroContrat);
}
