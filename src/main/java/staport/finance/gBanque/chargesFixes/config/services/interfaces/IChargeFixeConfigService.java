package staport.finance.gBanque.chargesFixes.config.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigRequestDTO;
import staport.finance.gBanque.chargesFixes.config.dtos.request.ChargeFixeConfigSearchCriteria;
import staport.finance.gBanque.chargesFixes.config.dtos.response.ChargeFixeConfigResponseDTO;
import staport.finance.gBanque.chargesFixes.config.entities.ChargeFixeConfig;

import java.util.List;

/**
 * Contrat de service pour l'administration des charges fixes bancaires (DIP) —
 * les contrôleurs et le scheduler injectent exclusivement cette interface.
 */
public interface IChargeFixeConfigService {

    Page<ChargeFixeConfigResponseDTO> findAll(ChargeFixeConfigSearchCriteria criteria, Pageable pageable);

    ChargeFixeConfigResponseDTO create(ChargeFixeConfigRequestDTO dto);

    ChargeFixeConfigResponseDTO update(Long id, ChargeFixeConfigRequestDTO dto);

    void delete(Long id);

    /**
     * Façade réservée à {@code ChargesFixesScheduler} (même module {@code chargesFixes}) :
     * retourne les entités complètes (banque déjà résolue) nécessaires à la génération
     * mensuelle. Jamais exposée hors de ce module.
     */
    List<ChargeFixeConfig> findAllEntities();
}
