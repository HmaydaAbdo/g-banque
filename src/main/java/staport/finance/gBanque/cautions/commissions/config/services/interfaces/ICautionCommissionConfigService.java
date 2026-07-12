package staport.finance.gBanque.cautions.commissions.config.services.interfaces;

import staport.finance.gBanque.cautions.commissions.config.dtos.request.CautionCommissionConfigRequestDTO;
import staport.finance.gBanque.cautions.commissions.config.dtos.response.CautionCommissionConfigResponseDTO;

import java.util.List;

/**
 * Contrat de service pour les paramètres de commission caution (seuil minimum, diviseur),
 * administrables via l'UI — voir {@code conventions/configuration.md}.
 *
 * <p>Consommé par les {@code CommissionCautionStrategie} (module {@code cautions.commissions})
 * pour résoudre le seuil minimum et le diviseur applicables à une banque, à la place des
 * constantes Java auparavant codées en dur.
 */
public interface ICautionCommissionConfigService {

    List<CautionCommissionConfigResponseDTO> findAll();

    CautionCommissionConfigResponseDTO create(CautionCommissionConfigRequestDTO dto);

    CautionCommissionConfigResponseDTO update(Long id, CautionCommissionConfigRequestDTO dto);

    void delete(Long id);

    /**
     * Résout les paramètres de commission caution d'une banque (insensible à la casse).
     *
     * @throws staport.finance.gBanque.cautions.exceptions.CautionBusinessException si aucune
     *         configuration n'existe pour cette banque — la commission ne peut pas être calculée
     *         sans seuil minimum ni diviseur.
     */
    CautionCommissionConfigResponseDTO resolveOuLeverException(String nomBanque);
}
