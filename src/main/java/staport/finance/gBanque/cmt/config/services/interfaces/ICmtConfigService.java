package staport.finance.gBanque.cmt.config.services.interfaces;

import staport.finance.gBanque.cmt.config.dtos.CmtConfigRequestDTO;
import staport.finance.gBanque.cmt.config.dtos.CmtConfigResponseDTO;

/**
 * Façade publique de la configuration CMT.
 * Seule cette interface doit être injectée par les autres sous-modules cmt
 * (metrics, echeances) — jamais le Repository directement.
 */
public interface ICmtConfigService {

    CmtConfigResponseDTO getConfig();

    CmtConfigResponseDTO update(CmtConfigRequestDTO dto);

    /** Fenêtre (en jours) de la métrique "échéances à venir à court terme". */
    int getAlerteEcheanceJoursCourtTerme();

    /** Fenêtre (en jours) de la métrique "échéances à venir à long terme". */
    int getAlerteEcheanceJoursLongTerme();
}
