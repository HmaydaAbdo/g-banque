package staport.finance.gBanque.LigneFonctionnelle.config.services.interfaces;

import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigRequestDTO;
import staport.finance.gBanque.LigneFonctionnelle.config.dtos.LigneFonctionnelleConfigResponseDTO;

/**
 * Façade publique de la configuration LigneFonctionnelle.
 * Seule cette interface doit être injectée par les autres sous-modules LigneFonctionnelle
 * (batch général, batch spécifique) — jamais le Repository directement.
 */
public interface ILigneFonctionnelleConfigService {

    LigneFonctionnelleConfigResponseDTO getConfig();

    LigneFonctionnelleConfigResponseDTO update(LigneFonctionnelleConfigRequestDTO dto);

    /** Nombre de mois restants avant expiration en-deçà duquel une ALERTE est générée. */
    int getAlertThresholdMonths();

    /** Nombre de mois restants avant expiration en-deçà duquel un AVERTISSEMENT est généré. */
    int getWarningThresholdMonths();
}
