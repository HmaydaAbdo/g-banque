package staport.finance.gBanque.leasing.config.services.interfaces;

import staport.finance.gBanque.leasing.config.dtos.LeasingConfigRequestDTO;
import staport.finance.gBanque.leasing.config.dtos.LeasingConfigResponseDTO;

import java.math.BigDecimal;

/**
 * Façade publique de la configuration leasing.
 * Seule cette interface doit être injectée par les autres sous-modules leasing
 * (contratLeasing, echeancesLeasing) — jamais le Repository directement.
 */
public interface ILeasingConfigService {

    LeasingConfigResponseDTO getConfig();

    LeasingConfigResponseDTO update(LeasingConfigRequestDTO dto);

    /** Taux appliqué au montant d'achat TTC pour calculer la valeur résiduelle d'un contrat. */
    BigDecimal getValeurResiduelle();

    /** Taux de TVA utilisé pour ventiler les échéances (montant HT / TVA). */
    BigDecimal getTauxTva();
}
